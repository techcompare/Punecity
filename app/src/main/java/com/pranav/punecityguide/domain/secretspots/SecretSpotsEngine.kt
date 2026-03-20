package com.pranav.punecityguide.domain.secretspots

import com.pranav.punecityguide.data.model.Attraction
import kotlin.math.abs

object SecretSpotsEngine {
    fun rank(
        attractions: List<Attraction>,
        favoriteIds: Set<Int>,
        recentlyViewedIds: Set<Int>,
        nowHour: Int,
        limit: Int = 20,
    ): List<Attraction> {
        if (attractions.isEmpty()) return emptyList()

        fun timeBoost(category: String): Float {
            val c = category.lowercase()
            return when {
                nowHour in 6..10 && (c.contains("cafe") || c.contains("breakfast")) -> 1.2f
                nowHour in 17..21 && (c.contains("food") || c.contains("restaurant") || c.contains("night")) -> 1.2f
                nowHour in 10..16 && (c.contains("museum") || c.contains("heritage") || c.contains("park")) -> 1.15f
                else -> 1.0f
            }
        }

        fun noveltyPenalty(attractionId: Int): Float {
            if (favoriteIds.contains(attractionId)) return 0.55f
            if (recentlyViewedIds.contains(attractionId)) return 0.7f
            return 1.0f
        }

        fun ratingSignal(rating: Float, reviewCount: Int): Float {
            val stable = (rating.coerceIn(0f, 5f) / 5f)
            val confidence = (reviewCount.coerceIn(0, 500) / 500f) * 0.25f + 0.75f
            return stable * confidence
        }

        // Encourage variety: light penalty if many items from same category bubble to top.
        val scored = attractions.map { a ->
            val base = 0.55f * ratingSignal(a.rating, a.reviewCount) +
                0.25f * (1f - abs((a.rating - 4.4f) / 2f).coerceIn(0f, 1f)) +
                0.20f
            val score = base * timeBoost(a.category) * noveltyPenalty(a.id)
            a to score
        }.sortedByDescending { it.second }

        val categoryCounts = mutableMapOf<String, Int>()
        val out = ArrayList<Attraction>(limit)
        for ((a, s) in scored) {
            if (out.size >= limit) break
            val key = a.category.lowercase()
            val count = categoryCounts.getOrDefault(key, 0)
            // Soft cap per category in the first results.
            if (out.size < 12 && count >= 4) continue
            if (s <= 0.01f) continue
            out.add(a)
            categoryCounts[key] = count + 1
        }
        return out
    }
}

