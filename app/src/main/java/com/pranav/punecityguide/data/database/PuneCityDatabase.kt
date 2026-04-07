package com.pranav.punecityguide.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.model.Itinerary
import com.pranav.punecityguide.data.model.RecentlyViewed
import com.pranav.punecityguide.data.model.SyncAuditLog
import com.pranav.punecityguide.data.model.AiTokenQuota

@Database(
    entities = [
        Attraction::class, 
        Itinerary::class, 
        RecentlyViewed::class, 
        SyncAuditLog::class, 
        AiTokenQuota::class,
        com.pranav.punecityguide.data.model.AiConversation::class,
        com.pranav.punecityguide.data.model.AiMessage::class
    ],
    version = 11,
    exportSchema = false
)
abstract class PuneCityDatabase : RoomDatabase() {
    abstract fun attractionDao(): AttractionDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun syncAuditDao(): SyncAuditDao
    abstract fun aiTokenQuotaDao(): AiTokenQuotaDao
    abstract fun aiChatDao(): AiChatDao
    
    companion object {
        @Volatile
        private var instance: PuneCityDatabase? = null
        
        fun getInstance(context: Context): PuneCityDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PuneCityDatabase::class.java,
                    "pune_city_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
