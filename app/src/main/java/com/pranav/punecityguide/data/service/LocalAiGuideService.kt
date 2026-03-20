package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.model.Attraction

object LocalAiGuideService {

    data class AiResponse(
        val reply: String,
        val recommendations: List<Attraction>
    )

    private val localPersonas = listOf(
        "Puneri Guide",
        "Dada (Big Brother)",
        "Pune Insider",
        "The Local Historian"
    )

    fun buildResponse(
        query: String,
        attractions: List<Attraction>,
        categories: List<String>
    ): AiResponse {
        val trimmed = query.trim()
        val q = trimmed.lowercase()

        // 1. Handle Empty Query
        if (q.isEmpty()) {
            return AiResponse(
                reply = "Ek Number! I'm your Pune Guide. Ask me for the best Misal Pav, a trek to Sinhagad, or some hidden spots away from the crowd!",
                recommendations = attractions.sortedByDescending { it.rating }.take(3)
            )
        }

        // 2. Secret / Hidden Gems
        if (containsAny(q, listOf("hidden gem", "hidden gems", "underrated", "less crowded", "secret", "peaceful"))) {
            val gems = pickHiddenGems(attractions, 4)
            val reply = if (gems.isNotEmpty()) {
                "Oh, so you want the secret sauce? 🤐 Pune has spots that haven't been 'Instagrammed' to death yet. Like ${gems.first().name}. It has a solid ${gems.first().rating} rating but is surprisingly quiet."
            } else {
                "Punekars usually keep their secrets well! I suggest exploring the Pashan Lake area or the old Peth heritage walks early in the morning."
            }
            return AiResponse(reply = reply, recommendations = gems)
        }

        // 3. Weather / Monsoon
        if (containsAny(q, listOf("rain", "raining", "monsoon", "weather", "wet"))) {
            val rainSafe = pickMonsoonSafe(attractions, 4)
            val reply = "Baap re! Raining in Pune is a vibe. ⛈️ You should check out ${rainSafe.getOrNull(0)?.name ?: "Aga Khan Palace"} - it looks majestic in the rain, or stick to indoor museums like Raja Dinkar Kelkar Museum."
            return AiResponse(reply = reply, recommendations = rainSafe)
        }

        // 4. Surprise / Random
        if (containsAny(q, listOf("surprise", "random", "something different", "bored"))) {
            val surprise = buildSurpriseTrail(attractions, 3)
            val reply = "Kadak! 💥 Let's break the routine. I've picked 3 random spots for a 'Surprise Trail'. Start at ${surprise.getOrNull(0)?.name ?: "Shaniwar Wada"} and see where it takes you!"
            return AiResponse(reply = reply, recommendations = surprise)
        }

        // 5. Itinerary / Structured Planning
        val constraints = extractConstraints(q)
        if (constraints != null || containsAny(q, listOf("plan", "itinerary", "route", "hours"))) {
            val plan = buildBudgetTimePlan(q, attractions, constraints ?: TripConstraints())
            return AiResponse(reply = plan.first, recommendations = plan.second)
        }

        // 6. Generic Category or Keyword Match
        val replyText = buildSmartReply(q, attractions, categories)
        val recommended = recommendAttractions(q, attractions)
        
        return AiResponse(reply = replyText, recommendations = recommended)
    }

    private fun buildSmartReply(query: String, attractions: List<Attraction>, categories: List<String>): String {
        val matches = attractions.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }.take(3)

        return when {
            containsAny(query, listOf("food", "misal", "bhakarwadi", "cafe")) -> {
                "As we say in Pune: 'Savadhan!' (Be Careful) - you might fall in love with the food here. 😉 Check out the 'Food' category. Camp and FC Road are your best bets for street eats."
            }
            containsAny(query, listOf("trek", "hill", "fort", "nature")) -> {
                "History meet heights! ⚔️ Pune is surrounded by forts like Sinhagad and hills like Vetal Tekdi. Most are free to climb and offer the best sunset views."
            }
            matches.isNotEmpty() -> {
                "I found some 'Ek Number' spots for '$query'. I'd recommend ${matches.first().name}. It's a ${matches.first().category} spot and very popular with locals."
            }
            else -> {
                val variety = listOf(
                    "I'm a local expert on Pune! Ask me about 'hidden gems', '1-day plans', or 'best viewpoints'.",
                    "I couldn't find a direct match, but did you know Pune is the 'Oxford of the East'? Try asking about museums or student hangouts!",
                    "That's a new one! How about exploring ${categories.take(2).joinToString(" or ")} instead? Pune has some great spots there.",
                    "Wait, let me think... actually, why don't you try asking for a 'random surprise trail'? It's more fun!"
                )
                variety.random()
            }
        }
    }

    fun recommendAttractions(query: String, attractions: List<Attraction>, limit: Int = 3): List<Attraction> {
        val q = query.lowercase()
        return attractions
            .filter { it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
            .sortedByDescending { it.rating }
            .take(limit)
            .ifEmpty { attractions.sortedByDescending { it.rating }.take(limit) }
    }

    private data class TripConstraints(
        val hours: Int = 6,
        val budget: Int = 1000,
        val group: String = "general"
    )

    private fun extractConstraints(query: String): TripConstraints? {
        val hourRegex = Regex("(\\d{1,2})\\s*(hour|hours|hr|hrs)")
        val budgetRegex = Regex("(?:rs\\.?|rupees|inr)?\\s*(\\d{2,5})")
        val hours = hourRegex.find(query)?.groupValues?.get(1)?.toIntOrNull()
        val budget = budgetRegex.find(query)?.groupValues?.get(1)?.toIntOrNull()
        val group = when {
            containsAny(query, listOf("family", "kids")) -> "family"
            containsAny(query, listOf("couple", "date")) -> "couple"
            else -> "general"
        }
        if (hours == null && budget == null && group == "general") return null
        return TripConstraints(hours ?: 6, budget ?: 1000, group)
    }

    private fun buildBudgetTimePlan(query: String, attractions: List<Attraction>, constraints: TripConstraints): Pair<String, List<Attraction>> {
        val count = if (constraints.hours <= 4) 2 else 3
        val picks = attractions
            .filter { attractions ->
                if (constraints.group == "family") containsAny(attractions.category.lowercase(), listOf("park", "museum", "garden")) else true
            }
            .sortedByDescending { it.rating }
            .take(count)

        val reply = buildString {
            append("Lay Bhari! (Very Great!) Here's a custom ${constraints.hours}h Pune plan for your ${constraints.group} outing:\n\n")
            picks.forEachIndexed { i, p -> append("📍 ${i+1}. ${p.name} (${p.category})\n") }
            append("\nLocal Tip: Pune traffic can be 'Shocking' 🚦, so keep 30 mins buffer between stops. Enjoy!")
        }
        return reply to picks
    }

    private fun pickHiddenGems(attractions: List<Attraction>, limit: Int) = attractions.filter { it.rating >= 4.0f && it.reviewCount < 500 }.sortedByDescending { it.rating }.take(limit)
    private fun pickMonsoonSafe(attractions: List<Attraction>, limit: Int) = attractions.filter { containsAny(it.category.lowercase(), listOf("museum", "palace", "temple", "mall")) }.take(limit)
    private fun buildSurpriseTrail(attractions: List<Attraction>, limit: Int) = attractions.shuffled().take(limit)

    private fun containsAny(text: String, tokens: List<String>) = tokens.any { text.contains(it) }
}
