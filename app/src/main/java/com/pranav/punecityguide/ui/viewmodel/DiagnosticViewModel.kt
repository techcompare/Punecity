package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.SyncAuditLog
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.AttractionCache
import com.pranav.punecityguide.data.service.BackendHealthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticUiState(
    val healthReport: BackendHealthService.HealthReport? = null,
    val cacheStats: AttractionCache.CacheStats? = null,
    val recentLogs: List<SyncAuditLog> = emptyList(),
    val isCheckingHealth: Boolean = false,
    val lastCheckTime: Long = 0,
    val featureToggles: Map<String, Boolean> = emptyMap()
)

class DiagnosticViewModel(
    private val healthService: BackendHealthService,
    private val auditRepository: SyncAuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
        observeLogs()
    }

    fun refreshAll() {
        runHealthCheck()
        refreshCacheStats()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            auditRepository.getRecentLogs(50).collect { logs ->
                _uiState.update { it.copy(recentLogs = logs) }
            }
        }
    }

    fun runHealthCheck() {
        if (_uiState.value.isCheckingHealth) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true) }
            val report = healthService.runFullHealthCheck()
            _uiState.update { it.copy(
                healthReport = report,
                isCheckingHealth = false,
                lastCheckTime = System.currentTimeMillis()
            ) }
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            val stats = AttractionCache.stats()
            _uiState.update { it.copy(cacheStats = stats) }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            auditRepository.clearAll()
        }
    }

    companion object {
        fun factory(
            healthService: BackendHealthService,
            auditRepository: SyncAuditRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DiagnosticViewModel(healthService, auditRepository) as T
            }
        }
    }
}
