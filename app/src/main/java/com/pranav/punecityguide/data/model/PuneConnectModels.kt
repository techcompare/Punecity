package com.pranav.punecityguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectUser(
    val id: String,
    val username: String = "Explorer",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ConnectPost(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null, // Fixed: Screenshot shows "user_id":null, making it nullable prevents the crash
    val title: String,
    @SerialName("description") // Aligned with standard Supabase schema to prevent 400 Bad Request
    val description: String? = null,
    val category: String? = "General",
    val area: String? = "Pune",
    @SerialName("image_url")
    val imageUrl: String? = null,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    val score: Int
        get() = upvotes - downvotes
}

@Serializable
data class ConnectComment(
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    val text: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class ConnectVote(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("vote_type")
    val voteType: Int // +1 or -1
)

@Serializable
data class ConnectSavedPost(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("post_id")
    val postId: String
)

@Serializable
data class GlobalChatMessage(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null
)
