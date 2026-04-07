package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.database.ExpenseDao
import com.pranav.punecityguide.data.model.Expense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val total: Double = 0.0
)

class ExpenseViewModel(private val dao: ExpenseDao) : ViewModel() {

    val uiState: StateFlow<ExpenseUiState> = dao.getAllExpenses()
        .map { list ->
            ExpenseUiState(
                expenses = list,
                total = list.sumOf { it.amount }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseUiState()
        )

    fun addExpense(title: String, amount: Double, category: String) {
        viewModelScope.launch {
            dao.insertExpense(Expense(title = title, amount = amount, category = category))
            com.pranav.punecityguide.data.service.ServiceLocator.preferenceManager.completeMission(com.pranav.punecityguide.data.service.MissionType.EXPENSE)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            dao.deleteExpense(expense)
        }
    }
    
    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    companion object {
        fun factory(dao: ExpenseDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExpenseViewModel(dao) as T
            }
        }
    }
}
