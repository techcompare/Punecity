package com.pranav.punecityguide.util

import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.ui.viewmodel.PassportProgress

object PassportUtil {
    fun buildPassport(favorites: List<Attraction>, categories: List<String>): PassportProgress {
        val collectedPlaces = favorites.size
        val unlockedCategories = favorites.map { it.category.lowercase() }.distinct().size
        val totalCategories = categories.distinct().size

        val levels = listOf(0, 3, 6, 10, 15)
        val levelNames = listOf(
            "New Explorer",
            "Weekend Wanderer",
            "City Navigator",
            "Pune Insider",
            "Pune Legend"
        )

        val levelIndex = levels.indexOfLast { collectedPlaces >= it }.coerceAtLeast(0)
        val levelName = levelNames[levelIndex]
        val currentFloor = levels[levelIndex]
        val nextLevelTarget = levels.getOrElse(levelIndex + 1) { currentFloor }

        val progressToNext = if (nextLevelTarget == currentFloor) {
            1f
        } else {
            (collectedPlaces - currentFloor).toFloat() / (nextLevelTarget - currentFloor).toFloat()
        }.coerceIn(0f, 1f)

        val badges = buildList {
            if (collectedPlaces >= 1) add("First Stamp")
            if (collectedPlaces >= 5) add("5 Places Club")
            if (unlockedCategories >= 3) add("Category Hopper")
            if (unlockedCategories >= 5) add("Culture Mixer")
            if (collectedPlaces >= 12) add("Local Expert")
        }

        val day = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(java.util.Date())
        val todaysQuest = when {
            collectedPlaces == 0 -> "Quest: Add your first favorite place to start your passport."
            day == "Sunday" -> "Sunday Quest: Experience Pune's nature. Find a park or hill to visit."
            day == "Monday" -> "Monday Quest: Start the week right. Search for a top-rated cafe to work from."
            day == "Friday" -> "Friday Night Quest: Pune's pulse is peaking! Discover a nightlife or dining spot."
            day == "Saturday" -> "Weekend Explorer: Unlock a new category today to earn bonus progress."
            unlockedCategories < 3 -> "Quest: Unlock 3 categories. Add 1 place from a new category."
            collectedPlaces < 10 -> "Quest: Reach 10 collected places to unlock Pune Insider level."
            else -> "Quest: Maintain your explorer streak by adding one new favorite today."
        }

        return PassportProgress(
            levelName = levelName,
            collectedPlaces = collectedPlaces,
            unlockedCategories = unlockedCategories,
            totalCategories = totalCategories,
            badges = badges,
            nextLevelTarget = nextLevelTarget,
            progressToNext = progressToNext,
            todaysQuest = todaysQuest
        )
    }
}
