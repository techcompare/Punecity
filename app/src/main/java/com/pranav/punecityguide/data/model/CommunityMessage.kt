package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityMessage(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("channel_id") val channelId: String = "global",
    @SerialName("reactions") val reactions: Map<String, Int> = emptyMap()
)

@Serializable
data class CommunityChannel(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // String representation of icon name
    @SerialName("message_count") val messageCount: Int = 0
)
