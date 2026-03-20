package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.ui.components.AttractionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverCategoryScreen(
    category: String,
    database: PuneCityDatabase,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val allAttractions by repository.observeAllAttractions().collectAsState(initial = emptyList())
    
    // UI Local State for sorting
    var sortByRating by remember { mutableStateOf(true) }

    val attractions = remember(category, allAttractions, sortByRating) {
        val filtered = filterForCategory(category, allAttractions)
        if (sortByRating) filtered.sortedByDescending { it.rating } else filtered
    }

    val categoryColor = remember(category) {
        when (category.lowercase()) {
            "historical" -> Color(0xFF8B4513)
            "nature" -> Color(0xFF2E7D32)
            "food" -> Color(0xFFD32F2F)
            "study spots" -> Color(0xFF1976D2)
            "hidden views" -> Color(0xFF7B1FA2)
            else -> Color(0xFF6200EE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(category, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${attractions.size} places found", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { sortByRating = !sortByRating }) {
                        Icon(
                            if (sortByRating) Icons.AutoMirrored.Filled.Sort else Icons.Default.FilterList,
                            contentDescription = "Sort"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Background Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(categoryColor.copy(alpha = 0.05f), Color.Transparent),
                            radius = 1200f
                        )
                    )
            )

            if (attractions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        CategoryIntro(category)
                    }
                    items(attractions) { attraction ->
                        AttractionCard(
                            attraction = attraction, 
                            onNavigateToDetail = onNavigateToDetail,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Quick Filters Overlay (Bottom)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortByRating,
                        onClick = { sortByRating = true },
                        label = { Text("Top Rated") },
                        leadingIcon = if (sortByRating) { { Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(16.dp)) } } else null
                    )
                    FilterChip(
                        selected = !sortByRating,
                        onClick = { sortByRating = false },
                        label = { Text("Popular") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryIntro(category: String) {
    val description = when (category.lowercase()) {
        "street food" -> "Pune's culinary soul is on the streets. From the spicy Misal Pav of old Peths to the bustling night markets of Camp."
        "hidden views" -> "Escape the urban noise. These Tekdis and Forts offer panoramic views that define the 'Oxford of the East'."
        "historical" -> "Trace the legacy of the Maratha Empire and colonial era through museums, wadas, and majestic palaces."
        else -> "Discover the best of $category in the cultural capital of Maharashtra."
    }
    
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

private fun filterForCategory(category: String, all: List<Attraction>): List<Attraction> {
    if (all.isEmpty()) return emptyList()

    val categoryKey = category.trim().lowercase()
    val queryWords = when (categoryKey) {
        "street food" -> listOf("street", "food", "misal", "vada", "chai", "thali", "trail", "market")
        "cafes" -> listOf("cafe", "coffee", "tea", "bakery", "brunch")
        "study spots" -> listOf("study", "library", "quiet", "campus", "university", "garden", "workspace")
        "hidden views" -> listOf("hidden", "view", "sunset", "hill", "fort", "lake", "tekdi", "point")
        "weekend trips" -> listOf("weekend", "trip", "fort", "dam", "hill", "trek", "getaway", "lake")
        "nightlife" -> listOf("night", "bar", "club", "late", "social", "park", "food")
        else -> listOf(categoryKey)
    }

    val scored = all.map { attraction ->
        val text = "${attraction.name} ${attraction.description} ${attraction.category}".lowercase()
        val exactCategoryBoost = if (attraction.category.trim().lowercase() == categoryKey) 8 else 0
        val wordMatches = queryWords.count { word -> text.contains(word) }
        val score = exactCategoryBoost + wordMatches
        attraction to score
    }

    return scored
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Pair<Attraction, Int>> { it.second }.thenByDescending { it.first.rating })
        .map { it.first }
        .take(40)
        .ifEmpty { all.sortedByDescending { it.rating }.take(20) }
}
