package com.pranav.punecityguide.data.database

import androidx.room.*
import com.pranav.punecityguide.data.model.AiConversation
import com.pranav.punecityguide.data.model.AiMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getConversations(userId: String): Flow<List<AiConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversation)

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessages(conversationId: String): Flow<List<AiMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessage)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
}
