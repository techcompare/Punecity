package com.pranav.punecityguide.data.service

import android.content.Context
import android.util.Log
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.SyncAuditRepository

/**
 * Centralized service locator for CostPilot backend services.
 *
 * Provides lazy-initialized, singleton-scoped access to all backend services.
 * This eliminates scattered service creation and ensures consistent lifecycles.
 *
 * Must be initialized once via [initialize] in Application.onCreate().
 */
object ServiceLocator {

    private const val TAG = "ServiceLocator"
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        Log.i(TAG, "ServiceLocator initialized")
    }

    private fun requireContext(): Context {
        return applicationContext
            ?: throw IllegalStateException("ServiceLocator not initialized. Call initialize() in Application.onCreate().")
    }

    // ── Database ──
    val database: PuneCityDatabase by lazy {
        PuneCityDatabase.getInstance(requireContext())
    }

    // ── DAOs ──
    val syncAuditDao by lazy { database.syncAuditDao() }
    val aiTokenQuotaDao by lazy { database.aiTokenQuotaDao() }
    val aiChatDao by lazy { database.aiChatDao() }
    val expenseDao by lazy { database.expenseDao() }

    // ── Repositories ──
    val syncAuditRepository: SyncAuditRepository by lazy {
        SyncAuditRepository(syncAuditDao)
    }

    val aiChatRepository by lazy {
        com.pranav.punecityguide.data.repository.AiChatRepository(aiChatDao)
    }

    val cityCostRepository by lazy {
        com.pranav.punecityguide.data.repository.CityCostRepository(offlineCacheService)
    }

    val communityRepository by lazy {
        com.pranav.punecityguide.data.repository.CommunityRepository()
    }

    val planRepository by lazy {
        com.pranav.punecityguide.data.repository.PlanRepository()
    }

    val userProfileRepository by lazy {
        com.pranav.punecityguide.data.repository.UserProfileRepository()
    }

    val expenseSyncRepository by lazy {
        com.pranav.punecityguide.data.repository.ExpenseSyncRepository(expenseDao)
    }

    // ── Services ──
    val authService: AuthService by lazy { AuthService() }

    val tokenSessionManager: TokenSessionManager by lazy {
        SupabaseClient.getSessionManager()
    }

    val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(requireContext())
    }

    val aiTokenQuotaService: AiTokenQuotaService by lazy {
        AiTokenQuotaService(aiTokenQuotaDao)
    }

    val currencyService: CurrencyService by lazy {
        CurrencyService(requireContext())
    }

    val offlineCacheService: OfflineCacheService by lazy {
        OfflineCacheService(requireContext())
    }

    val backendHealthService: BackendHealthService by lazy {
        BackendHealthService(syncAuditRepository)
    }

    val backgroundSyncManager: BackgroundSyncManager by lazy {
        BackgroundSyncManager(requireContext())
    }

    val analyticsService: AnalyticsService by lazy {
        AnalyticsService(syncAuditRepository, preferenceManager)
    }
}
