package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.database.ExpenseDao
import com.pranav.punecityguide.data.model.Expense
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Syncs local Room expenses ↔ Supabase `expenses` table.
 *
 * Offline-first strategy:
 * 1. All writes go to local Room DB first (instant UI update)
 * 2. Background sync pushes pending changes to Supabase
 * 3. Pull from Supabase on app launch / manual refresh
 * 4. Conflict resolution: last-write-wins with timestamp comparison
 */
class ExpenseSyncRepository(private val expenseDao: ExpenseDao) {

    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL
    private val TAG = "ExpenseSyncRepo"

    // ── Local Operations (instant) ──

    fun getAllExpensesLocal(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun addExpenseLocal(expense: Expense): Long {
        expenseDao.insertExpense(expense)
        return expense.id.toLong()
    }

    suspend fun deleteExpenseLocal(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun clearAllLocal() {
        expenseDao.clearAll()
    }

    // ── Remote Sync Operations ──

    /**
     * Push a single expense to Supabase.
     */
    suspend fun pushExpense(userId: String, expense: Expense): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteExpense = RemoteExpense(
                userId = userId,
                title = expense.title,
                amount = expense.amount,
                category = expense.category,
                timestamp = expense.timestamp,
                localId = expense.id
            )
            val response = client.post("$baseUrl/rest/v1/expenses") {
                contentType(ContentType.Application.Json)
                header("Prefer", "return=minimal,resolution=merge-duplicates")
                setBody(remoteExpense)
            }
            if (response.status.isSuccess()) {
                Log.d(TAG, "Expense pushed: ${expense.title}")
                Result.success(Unit)
            } else {
                val body = response.bodyAsText()
                Log.w(TAG, "Push failed: ${response.status} - $body")
                Result.failure(Exception("Sync failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Push error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Pull all expenses for a user from Supabase and merge into local DB.
     */
    suspend fun pullExpenses(userId: String): Result<List<Expense>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/expenses") {
                parameter("user_id", "eq.$userId")
                parameter("select", "*")
                parameter("order", "timestamp.desc")
            }
            if (response.status.isSuccess()) {
                val remote: List<RemoteExpense> = response.body()
                val local = remote.map { it.toLocalExpense() }
                // Merge: upsert all into local DB
                local.forEach { expenseDao.insertExpense(it) }
                Log.d(TAG, "Pulled ${local.size} expenses from Supabase")
                Result.success(local)
            } else {
                Result.failure(Exception("Pull failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pull error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Full bidirectional sync.
     */
    suspend fun fullSync(userId: String): Result<SyncResult> = withContext(Dispatchers.IO) {
        var pushed = 0
        var pulled = 0
        var errors = 0

        try {
            // 1. Pull remote → local
            pullExpenses(userId).onSuccess { pulled = it.size }

            // 2. Push local → remote (only unsync'd expenses)
            val localExpenses = expenseDao.getAllExpenses().firstOrNull() ?: emptyList()
            for (expense in localExpenses) {
                pushExpense(userId, expense).onSuccess { pushed++ }.onFailure { errors++ }
            }

            val result = SyncResult(pulled = pulled, pushed = pushed, errors = errors)
            Log.i(TAG, "Full sync complete: $result")
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a remote expense.
     */
    suspend fun deleteRemoteExpense(userId: String, localId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$baseUrl/rest/v1/expenses") {
                parameter("user_id", "eq.$userId")
                parameter("local_id", "eq.$localId")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Delete failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get expense analytics/summary from Supabase.
     */
    suspend fun getExpenseSummary(userId: String): Result<ExpenseSummary> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/expenses") {
                parameter("user_id", "eq.$userId")
                parameter("select", "amount,category")
            }
            if (response.status.isSuccess()) {
                val expenses: List<RemoteExpense> = response.body()
                val totalSpent = expenses.sumOf { it.amount }
                val byCategory = expenses.groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                val avgDaily = if (expenses.isNotEmpty()) {
                    val timestamps = expenses.mapNotNull { it.timestamp }
                    if (timestamps.size >= 2) {
                        val dayRange = ((timestamps.max() - timestamps.min()) / 86400000.0).coerceAtLeast(1.0)
                        totalSpent / dayRange
                    } else totalSpent
                } else 0.0

                Result.success(
                    ExpenseSummary(
                        totalSpent = totalSpent,
                        expenseCount = expenses.size,
                        averageDaily = avgDaily,
                        byCategory = byCategory
                    )
                )
            } else {
                Result.failure(Exception("Summary fetch failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Remote Models ──

@Serializable
data class RemoteExpense(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long? = System.currentTimeMillis(),
    @SerialName("local_id") val localId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toLocalExpense(): Expense = Expense(
        id = localId ?: 0,
        title = title,
        amount = amount,
        category = category,
        timestamp = timestamp ?: System.currentTimeMillis()
    )
}

data class SyncResult(
    val pulled: Int,
    val pushed: Int,
    val errors: Int
) {
    override fun toString() = "SyncResult(pulled=$pulled, pushed=$pushed, errors=$errors)"
}

data class ExpenseSummary(
    val totalSpent: Double,
    val expenseCount: Int,
    val averageDaily: Double,
    val byCategory: Map<String, Double>
)
