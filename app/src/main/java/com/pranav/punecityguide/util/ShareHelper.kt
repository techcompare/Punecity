package com.pranav.punecityguide.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pranav.punecityguide.data.model.Attraction
import java.io.File

/**
 * Share utilities for itineraries and attractions
 */
object ShareHelper {
    
    /**
     * Share a single attraction via system share sheet
     */
    fun shareAttraction(context: Context, attraction: Attraction) {
        val shareText = buildString {
            append("📍 ${attraction.name}\n\n")
            append("${attraction.description}\n\n")
            append("⭐ Rating: ${attraction.rating}/5 (${attraction.reviewCount} reviews)\n")
            append("🎫 Entry: ${attraction.entryFee}\n")
            append("🕐 Hours: ${attraction.openingHours}\n")
            append("📂 Category: ${attraction.category}\n\n")
            append("Shared from Pune City Guide")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, attraction.name)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share ${attraction.name}"))
    }
    
    /**
     * Share an itinerary (multiple attractions)
     */
    fun shareItinerary(context: Context, attractions: List<Attraction>, title: String = "My Pune Itinerary") {
        val shareText = buildString {
            append("🗺️ $title\n")
            append("=".repeat(40) + "\n\n")
            
            attractions.forEachIndexed { index, attraction ->
                val timeSlot = when (index) {
                    0 -> "☀️ Morning"
                    1 -> "🌤️ Afternoon"
                    2 -> "🌆 Evening"
                    else -> "📍 Stop ${index + 1}"
                }
                
                append("$timeSlot: ${attraction.name}\n")
                append("   ${attraction.category} • ${attraction.rating}⭐\n")
                append("   ${attraction.entryFee}\n\n")
            }
            
            append("\n✨ Created with Pune City Guide\n")
            append("Download the app to explore more!")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Itinerary"))
    }
    
    /**
     * Share chatbot conversation recommendations
     */
    fun shareChatRecommendations(context: Context, query: String, recommendations: List<Attraction>) {
        shareItinerary(
            context = context,
            attractions = recommendations,
            title = "AI Recommendations: $query"
        )
    }
    
    /**
     * Share favorites collection
     */
    fun shareFavorites(context: Context, favorites: List<Attraction>) {
        shareItinerary(
            context = context,
            attractions = favorites,
            title = "My Favorite Pune Places"
        )
    }
}
