package com.pranav.punecityguide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "ai_token_quota")
data class AiTokenQuota(
    @PrimaryKey
    val userId: String,
    val date: String = LocalDate.now().toString(),
    val tokensUsed: Int = 0,
    val dailyLimit: Int = 10,
    val lastResetDate: String = LocalDate.now().toString()
) {
    fun hasTokensRemaining(): Boolean {
        val today = LocalDate.now().toString()
        return if (date != today) {
            true // New day, reset
        } else {
            tokensUsed < dailyLimit
        }
    }

    fun remainingTokens(): Int {
        val today = LocalDate.now().toString()
        return if (date != today) {
            dailyLimit
        } else {
            maxOf(0, dailyLimit - tokensUsed)
        }
    }
}
