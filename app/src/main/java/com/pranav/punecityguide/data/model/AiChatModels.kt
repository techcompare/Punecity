package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Serializable
@Entity(tableName = "ai_conversations")
data class AiConversation(
    @PrimaryKey
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("title")
    val title: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
@Entity(tableName = "ai_messages")
data class AiMessage(
    @PrimaryKey
    @SerialName("id")
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("role")
    val role: String, // "user" or "assistant"
    @SerialName("content")
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
