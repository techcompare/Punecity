package com.pranav.punecityguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pranav.punecityguide.data.model.Attraction
import kotlinx.coroutines.flow.Flow

@Dao
interface AttractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttraction(attraction: Attraction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttractions(attractions: List<Attraction>)
    
    @Query("SELECT COUNT(*) FROM attractions")
    suspend fun getAttractionCount(): Int

    @Query("SELECT name FROM attractions")
    suspend fun getAllAttractionNames(): List<String>
    
    @Query("SELECT * FROM attractions ORDER BY rating DESC LIMIT :limit")
    fun getTopAttractions(limit: Int = 50): Flow<List<Attraction>>
    
    @Query("SELECT * FROM attractions WHERE category = :category ORDER BY rating DESC")
    fun getAttractionsByCategory(category: String): Flow<List<Attraction>>
    
    @Query("SELECT * FROM attractions WHERE name LIKE :query OR description LIKE :query")
    fun searchAttractions(query: String): Flow<List<Attraction>>
    
    @Query("SELECT DISTINCT category FROM attractions")
    fun getAllCategories(): Flow<List<String>>
    
    @Query("SELECT * FROM attractions WHERE id = :id")
    fun observeAttractionById(id: Int): Flow<Attraction?>

    @Query("SELECT * FROM attractions WHERE id = :id")
    suspend fun getAttractionById(id: Int): Attraction?

    @Query("SELECT * FROM attractions")
    suspend fun getAllAttractions(): List<Attraction>

    @Query("SELECT * FROM attractions ORDER BY rating DESC, reviewCount DESC")
    fun observeAllAttractions(): Flow<List<Attraction>>
    
    @Query("UPDATE attractions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)
    
    @Query("SELECT * FROM attractions WHERE isFavorite = 1")
    fun getFavoriteAttractions(): Flow<List<Attraction>>
    
    @Update
    suspend fun updateAttraction(attraction: Attraction)
    
    @Query("DELETE FROM attractions WHERE id = :id")
    suspend fun deleteAttraction(id: Int)

    @Transaction
    suspend fun upsertAllAttractions(attractions: List<Attraction>) {
        val currentItems = getAllAttractions()
        val currentByNaturalKey = currentItems.associateBy { 
            it.name.trim().lowercase() to it.category.trim().lowercase() 
        }

        val itemsToUpsert = attractions.map { incoming ->
            val key = incoming.name.trim().lowercase() to incoming.category.trim().lowercase()
            val existing = currentByNaturalKey[key]
            
            if (existing != null) {
                // Preserve the ID and any local-only metadata like isFavorite
                incoming.copy(
                    id = existing.id,
                    isFavorite = existing.isFavorite
                )
            } else {
                incoming
            }
        }
        
        insertAttractions(itemsToUpsert)
    }

    @Query("DELETE FROM attractions")
    suspend fun clearAllAttractions()

    @Query("SELECT * FROM attractions WHERE name = :name AND category = :category LIMIT 1")
    suspend fun getAttractionByNameAndCategory(name: String, category: String): Attraction?

    @Query("SELECT * FROM attractions WHERE neighborhood = :neighborhood ORDER BY rating DESC")
    fun getAttractionsByNeighborhood(neighborhood: String): Flow<List<Attraction>>

    @Query("SELECT DISTINCT neighborhood FROM attractions WHERE neighborhood != '' ORDER BY neighborhood")
    fun getAllNeighborhoods(): Flow<List<String>>
}
