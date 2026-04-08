package com.pranav.punecityguide.data

import android.content.Context
import com.pranav.punecityguide.model.PuneSpot
import com.pranav.punecityguide.model.SavedPlace
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists "saved" spots on-device so bookmarks work without a Supabase "saved" table.
 * Entries are merged with any remote rows in [SavedRepository].
 */
object LocalSavedPlacesStore {
    private const val PREFS = "punebuzz_prefs"
    private const val KEY = "local_saved_places_v1"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val listSerializer = ListSerializer(LocalSavedPlaceEntry.serializer())

    fun getAll(context: Context): List<LocalSavedPlaceEntry> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }.getOrElse { emptyList() }
    }

    fun isSaved(context: Context, spotId: Int): Boolean =
        getAll(context).any { it.spotId == spotId }

    /**
     * @return true if the spot is saved after the toggle.
     */
    fun toggle(context: Context, spot: PuneSpot): Boolean {
        val current = getAll(context).toMutableList()
        val idx = current.indexOfFirst { it.spotId == spot.id }
        return if (idx >= 0) {
            current.removeAt(idx)
            persist(context, current)
            false
        } else {
            current.add(
                LocalSavedPlaceEntry(
                    spotId = spot.id,
                    name = spot.name,
                    category = spot.category,
                    area = spot.area,
                    imageUrl = spot.imageUrl,
                ),
            )
            persist(context, current)
            true
        }
    }

    private fun persist(context: Context, entries: List<LocalSavedPlaceEntry>) {
        val encoded = json.encodeToString(listSerializer, entries)
        prefs(context).edit().putString(KEY, encoded).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

@Serializable
data class LocalSavedPlaceEntry(
    val spotId: Int,
    val name: String,
    val category: String? = null,
    val area: String? = null,
    val imageUrl: String? = null,
) {
    fun toSavedPlace(): SavedPlace {
        val subtitle = listOfNotNull(category?.takeIf { it.isNotBlank() }, area?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifBlank { null }
        return SavedPlace(
            id = spotId.toString(),
            name = name,
            subtitle = subtitle,
            imageUrl = imageUrl,
        )
    }
}
