package com.pranav.punecityguide.data.repository

import android.util.Log
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.model.Plan
import com.pranav.punecityguide.data.model.PlanPlace
import com.pranav.punecityguide.data.service.NetworkResilience
import com.pranav.punecityguide.data.service.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production-grade plan repository with:
 * - Network resilience (retry + circuit breaker)
 * - Plan CRUD with optimistic updates
 * - Plan cloning with deep copy
 * - Plan deletion with cascade
 * - Plan sharing/visibility toggle
 * - Plan statistics
 */
class PlanRepository {
    private val client: HttpClient get() = SupabaseClient.getHttpClient()
    private val baseUrl = AppConfig.Supabase.SUPABASE_URL
    private val TAG = "PlanRepository"

    suspend fun getPublicPlans(): Result<List<Plan>> {
        return NetworkResilience.withRetry("plans_public", maxRetries = 2) {
            val response = client.get("$baseUrl/rest/v1/plans") {
                parameter("is_public", "eq.true")
                parameter("select", "*")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) {
                response.body<List<Plan>>()
            } else {
                throw Exception("Failed to fetch public plans: HTTP ${response.status.value}")
            }
        }
    }

    suspend fun getMyPlans(userId: String): Result<List<Plan>> {
        return NetworkResilience.withRetry("plans_mine", maxRetries = 2) {
            val response = client.get("$baseUrl/rest/v1/plans") {
                parameter("created_by", "eq.$userId")
                parameter("select", "*")
                parameter("order", "created_at.desc")
            }
            if (response.status.isSuccess()) {
                response.body<List<Plan>>()
            } else {
                throw Exception("Failed to fetch your plans: HTTP ${response.status.value}")
            }
        }
    }

    suspend fun getPlanPlaces(planId: String): Result<List<PlanPlace>> {
        return try {
            val response = client.get("$baseUrl/rest/v1/plan_places") {
                parameter("plan_id", "eq.$planId")
                parameter("select", "*")
                parameter("order", "order.asc")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch plan places"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlan(userId: String, title: String, description: String?, duration: String?, isPublic: Boolean): Result<Plan> {
        return try {
            val plan = Plan(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                duration = duration,
                createdBy = userId,
                isPublic = isPublic
            )
            val response = client.post("$baseUrl/rest/v1/plans") {
                contentType(ContentType.Application.Json)
                setBody(plan)
                header("Prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<Plan> = response.body()
                Result.success(created.first())
            } else {
                val body = response.bodyAsText()
                Log.e(TAG, "Create plan failed: ${response.status} - $body")
                Result.failure(Exception("Failed to create plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update a plan's details.
     */
    suspend fun updatePlan(planId: String, title: String? = null, description: String? = null, isPublic: Boolean? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>()
            title?.let { updates["title"] = it }
            description?.let { updates["description"] = it }
            isPublic?.let { updates["is_public"] = it }

            if (updates.isEmpty()) return@withContext Result.success(Unit)

            val response = client.patch("$baseUrl/rest/v1/plans") {
                parameter("id", "eq.$planId")
                contentType(ContentType.Application.Json)
                setBody(updates)
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to update plan"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a plan and its associated places.
     */
    suspend fun deletePlan(planId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete places first (cascade)
            client.delete("$baseUrl/rest/v1/plan_places") {
                parameter("plan_id", "eq.$planId")
            }
            // Delete the plan
            val response = client.delete("$baseUrl/rest/v1/plans") {
                parameter("id", "eq.$planId")
            }
            if (response.status.isSuccess()) {
                Log.d(TAG, "Plan deleted: $planId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clonePlan(userId: String, sourcePlanId: String): Result<Plan> {
        return try {
            // 1. Fetch source plan
            val planResponse = client.get("$baseUrl/rest/v1/plans") {
                parameter("id", "eq.$sourcePlanId")
                parameter("select", "*")
            }
            if (!planResponse.status.isSuccess()) return Result.failure(Exception("Source plan not found"))
            val sourcePlan = planResponse.body<List<Plan>>().first()

            // 2. Fetch source places
            val placesResult = getPlanPlaces(sourcePlanId)
            if (placesResult.isFailure) return Result.failure(placesResult.exceptionOrNull()!!)
            val sourcePlaces = placesResult.getOrDefault(emptyList())

            // 3. Create new plan
            val newPlanResult = createPlan(userId, "Copy of ${sourcePlan.title}", sourcePlan.description, sourcePlan.duration, false)
            if (newPlanResult.isFailure) return Result.failure(newPlanResult.exceptionOrNull()!!)
            val newPlan = newPlanResult.getOrNull()!!

            // 4. Create new places
            sourcePlaces.forEach { place ->
                val newPlace = place.copy(id = null, planId = newPlan.id)
                client.post("$baseUrl/rest/v1/plan_places") {
                    contentType(ContentType.Application.Json)
                    setBody(newPlace)
                }
            }

            Result.success(newPlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPlaceToPlan(planId: String, name: String, placeId: String?, order: Int): Result<PlanPlace> {
        return try {
            val place = PlanPlace(
                planId = planId,
                placeName = name,
                placeId = placeId,
                order = order
            )
            val response = client.post("$baseUrl/rest/v1/plan_places") {
                contentType(ContentType.Application.Json)
                setBody(place)
                header("Prefer", "return=representation")
            }
            if (response.status.isSuccess()) {
                val created: List<PlanPlace> = response.body()
                Result.success(created.first())
            } else {
                Result.failure(Exception("Failed to add place to plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a place from a plan.
     */
    suspend fun removePlaceFromPlan(placeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$baseUrl/rest/v1/plan_places") {
                parameter("id", "eq.$placeId")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to remove place"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reorder places within a plan.
     */
    suspend fun reorderPlaces(planId: String, orderedPlaceIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            orderedPlaceIds.forEachIndexed { index, id ->
                client.patch("$baseUrl/rest/v1/plan_places") {
                    parameter("id", "eq.$id")
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("order" to index))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get plan count for a user (for profile stats).
     */
    suspend fun getUserPlanCount(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$baseUrl/rest/v1/plans") {
                parameter("created_by", "eq.$userId")
                parameter("select", "id")
                header("Prefer", "count=exact")
            }
            if (response.status.isSuccess()) {
                response.headers["Content-Range"]?.substringAfterLast("/")?.toIntOrNull() ?: 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }
}
