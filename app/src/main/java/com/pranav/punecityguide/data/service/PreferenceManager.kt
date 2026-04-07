package com.pranav.punecityguide.data.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * Manages user engagement preferences, streaks, and onboarding state.
 * Direct strategy to solve "users are leaving" by providing continuity and rewards.
 */
class PreferenceManager(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DISCOVERY_STREAK = intPreferencesKey("discovery_streak")
        private val KEY_LAST_CHECKIN_DATE = stringPreferencesKey("last_checkin_date")
        private val KEY_SPOT_OF_THE_DAY_ID = intPreferencesKey("spot_of_the_day_id")
        private val KEY_SPOT_OF_THE_DAY_DATE = stringPreferencesKey("spot_of_the_day_date")
        private val KEY_SPOT_REVEALED_DATE = stringPreferencesKey("spot_revealed_date")
        val KEY_PROFILE_PIC_URI = stringPreferencesKey("profile_pic_uri")
        private val KEY_LAST_POST_TIME = longPreferencesKey("last_post_time_epoch")
        
        // Guest Auth
        private val KEY_GUEST_ID = stringPreferencesKey("guest_id_v2")
        private val KEY_GUEST_NAME = stringPreferencesKey("guest_name_v2")
        
        // Mission Keys
        private val KEY_DAILY_MISSION_DATE = stringPreferencesKey("daily_mission_date")
        private val KEY_MISSION_COMPARE_DONE = booleanPreferencesKey("mission_compare_done")
        private val KEY_MISSION_EXPENSE_DONE = booleanPreferencesKey("mission_expense_done")
        private val KEY_MISSION_COMMUNITY_DONE = booleanPreferencesKey("mission_community_done")
    }

    val profilePicUri: Flow<String?> = context.appPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_PROFILE_PIC_URI] }

    suspend fun setProfilePicUri(uri: String) {
        context.appPrefsDataStore.edit { it[KEY_PROFILE_PIC_URI] = uri }
    }

    val onboardingCompleted: Flow<Boolean> = context.appPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    val lastPostTime: Flow<Long> = context.appPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_LAST_POST_TIME] ?: 0L }

    suspend fun updateLastPostTime() {
        context.appPrefsDataStore.edit { it[KEY_LAST_POST_TIME] = System.currentTimeMillis() }
    }

    val discoveryStreak: Flow<Int> = context.appPrefsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_DISCOVERY_STREAK] ?: 0 }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appPrefsDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    /**
     * Updates the daily discovery streak.
     * Logic: If checked today, do nothing. If checked yesterday, increment. If older, reset to 1.
     */
    suspend fun updateStreak() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDate = context.appPrefsDataStore.data.map { it[KEY_LAST_CHECKIN_DATE] }.firstOrNull()

        if (lastDate == today) return // Already updated today

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        val currentStreak = context.appPrefsDataStore.data.map { it[KEY_DISCOVERY_STREAK] }.firstOrNull() ?: 0

        val newStreak = when (lastDate) {
            yesterday -> (currentStreak + 1).coerceAtMost(99)
            else -> 1
        }

        context.appPrefsDataStore.edit { prefs ->
            prefs[KEY_DISCOVERY_STREAK] = newStreak
            prefs[KEY_LAST_CHECKIN_DATE] = today
        }
        
        resetMissionsIfNewDay()
    }

    private suspend fun resetMissionsIfNewDay() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = context.appPrefsDataStore.data.map { it[KEY_DAILY_MISSION_DATE] }.firstOrNull()
        
        if (savedDate != today) {
            context.appPrefsDataStore.edit { prefs ->
                prefs[KEY_DAILY_MISSION_DATE] = today
                prefs[KEY_MISSION_COMPARE_DONE] = false
                prefs[KEY_MISSION_EXPENSE_DONE] = false
                prefs[KEY_MISSION_COMMUNITY_DONE] = false
            }
        }
    }

    suspend fun completeMission(type: MissionType) {
        context.appPrefsDataStore.edit { prefs ->
            when (type) {
                MissionType.COMPARE -> prefs[KEY_MISSION_COMPARE_DONE] = true
                MissionType.EXPENSE -> prefs[KEY_MISSION_EXPENSE_DONE] = true
                MissionType.COMMUNITY -> prefs[KEY_MISSION_COMMUNITY_DONE] = true
            }
        }
    }

    val missionsState: Flow<MissionsState> = context.appPrefsDataStore.data
        .map { prefs ->
            MissionsState(
                compareDone = prefs[KEY_MISSION_COMPARE_DONE] ?: false,
                expenseDone = prefs[KEY_MISSION_EXPENSE_DONE] ?: false,
                communityDone = prefs[KEY_MISSION_COMMUNITY_DONE] ?: false
            )
        }

    /**
     * Gets a stable spot ID for the day or generates a new one.
     */
    suspend fun getSpotOfTheDay(totalCount: Int): Int {
        if (totalCount <= 0) return 0
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val prefs = context.appPrefsDataStore.data
                .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
                .firstOrNull() ?: emptyPreferences()

            val savedDate = prefs[KEY_SPOT_OF_THE_DAY_DATE]
            val savedId = prefs[KEY_SPOT_OF_THE_DAY_ID]

            if (savedDate == today && savedId != null) {
                return savedId.coerceIn(0, totalCount - 1)
            }

            // Generate new spot of the day (stable for 24h)
            val newId = (0 until totalCount).random()
            context.appPrefsDataStore.edit { p ->
                p[KEY_SPOT_OF_THE_DAY_ID] = newId
                p[KEY_SPOT_OF_THE_DAY_DATE] = today
            }
            return newId
        } catch (e: Exception) {
            return 0
        }
    }
    suspend fun getOrCreateGuestId(): String {
        val prefs = context.appPrefsDataStore.data.firstOrNull() ?: emptyPreferences()
        val currentId = prefs[KEY_GUEST_ID]
        if (currentId != null) return currentId

        val newId = UUID.randomUUID().toString()
        context.appPrefsDataStore.edit { it[KEY_GUEST_ID] = newId }
        return newId
    }

    suspend fun getOrCreateGuestName(): String {
        val prefs = context.appPrefsDataStore.data.firstOrNull() ?: emptyPreferences()
        val currentName = prefs[KEY_GUEST_NAME]
        if (currentName != null) return currentName

        val animal = listOf("Nomad", "Explorer", "Pilot", "Scout", "Voyager", "Rover").random()
        val suffix = (1000..9999).random()
        val newName = "Guest $animal#$suffix"
        context.appPrefsDataStore.edit { it[KEY_GUEST_NAME] = newName }
        return newName
    }
}

enum class MissionType { COMPARE, EXPENSE, COMMUNITY }
data class MissionsState(
    val compareDone: Boolean,
    val expenseDone: Boolean,
    val communityDone: Boolean
) {
    val totalDone: Int get() {
        var count = 0
        if (compareDone) count++
        if (expenseDone) count++
        if (communityDone) count++
        return count
    }
    val allDone: Boolean get() = compareDone && expenseDone && communityDone
}
