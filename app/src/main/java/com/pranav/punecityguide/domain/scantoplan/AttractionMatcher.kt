package com.pranav.punecityguide.domain.scantoplan

import com.pranav.punecityguide.data.model.Attraction
import kotlin.math.max

object AttractionMatcher {
    data class Match(val attraction: Attraction, val score: Int)

    fun bestMatch(attractions: List<Attraction>, text: String): Match? {
        val cleaned = text.lowercase()
        if (cleaned.length < 3) return null

        var best: Match? = null
        for (a in attractions) {
            val name = a.name.lowercase()
            val nativeName = a.nativeLanguageName.lowercase()

            val score =
                exactBoost(cleaned, name) +
                exactBoost(cleaned, nativeName) +
                tokenOverlap(cleaned, name) +
                (tokenOverlap(cleaned, nativeName) / 2)

            if (score <= 0) continue
            if (best == null || score > best!!.score) {
                best = Match(a, score)
            }
        }
        return best
    }

    private fun exactBoost(haystack: String, needle: String): Int {
        if (needle.isBlank()) return 0
        return if (haystack.contains(needle)) 120 else 0
    }

    private fun tokenOverlap(a: String, b: String): Int {
        val ta = a.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
        val tb = b.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0
        val common = ta.intersect(tb).size
        return max(0, common * 25)
    }
}

