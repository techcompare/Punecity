package com.pranav.punecityguide.model

data class CommunityPost(
    val id: String,
    val author: String,
    val content: String,
    val createdAt: String?,
    val location: String? = null,
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
)

data class CuratedPlan(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationHours: Int,
    val estimatedCost: Int? = null,
    val spots: List<PuneSpot> = emptyList(),
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
)

data class PassportProfile(
    val userId: String,
    val username: String,
    val joinDate: String,
    val postsCount: Int = 0,
    val savedPlacesCount: Int = 0,
    val visitsCount: Int = 0,
    val xp: Int = 0,
    val level: PassportLevel = PassportLevel.EXPLORER,
    val badges: List<Badge> = emptyList(),
)

enum class PassportLevel(val title: String, val minXp: Int, val icon: String) {
    EXPLORER("Explorer", 0, "🌱"),
    WANDERER("Wanderer", 100, "🚶"),
    NAVIGATOR("Navigator", 300, "🧭"),
    INSIDER("Insider", 600, "⭐"),
    LEGEND("Legend", 1000, "👑");
    
    companion object {
        fun fromXp(xp: Int): PassportLevel = entries.lastOrNull { xp >= it.minXp } ?: EXPLORER
    }
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val earnedAt: String? = null,
)

object BadgeDefinitions {
    val FIRST_POST = Badge("first_post", "First Voice", "Shared your first community post", "💬")
    val FIRST_SAVE = Badge("first_save", "Bookworm", "Saved your first place", "📖")
    val FIVE_VISITS = Badge("five_visits", "Frequent Visitor", "Visited 5 places", "🎯")
    val TEN_POSTS = Badge("ten_posts", "Storyteller", "Shared 10 community posts", "📝")
    val TWENTY_SAVES = Badge("twenty_saves", "Collector", "Saved 20 places", "🗂️")
    val EARLY_ADOPTER = Badge("early_adopter", "Early Adopter", "Joined in the early days", "🚀")
}
