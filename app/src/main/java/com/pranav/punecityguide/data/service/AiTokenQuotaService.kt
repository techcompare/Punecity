package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.database.AiTokenQuotaDao
import com.pranav.punecityguide.data.model.AiTokenQuota
import java.time.LocalDate

class AiTokenQuotaService(private val quotaDao: AiTokenQuotaDao) {
    
    suspend fun canUseAi(userId: String): Boolean {
        val today = LocalDate.now().toString()
        quotaDao.resetQuotaIfNewDay(userId, today)
        
        val quota = quotaDao.getQuota(userId) ?: run {
            quotaDao.insertQuota(AiTokenQuota(userId = userId, date = today, tokensUsed = 0))
            AiTokenQuota(userId = userId, date = today, tokensUsed = 0)
        }
        
        return quota.hasTokensRemaining()
    }

    suspend fun getRemainingTokens(userId: String): Int {
        val today = LocalDate.now().toString()
        quotaDao.resetQuotaIfNewDay(userId, today)
        
        val quota = quotaDao.getQuota(userId) ?: run {
            quotaDao.insertQuota(AiTokenQuota(userId = userId, date = today, tokensUsed = 0))
            AiTokenQuota(userId = userId, date = today, tokensUsed = 0)
        }
        
        return quota.remainingTokens()
    }

    suspend fun consumeToken(userId: String) {
        val today = LocalDate.now().toString()
        quotaDao.resetQuotaIfNewDay(userId, today)
        quotaDao.incrementTokenUsage(userId, today)
    }

    suspend fun getQuota(userId: String): AiTokenQuota? {
        val today = LocalDate.now().toString()
        quotaDao.resetQuotaIfNewDay(userId, today)
        return quotaDao.getQuota(userId)
    }
}
