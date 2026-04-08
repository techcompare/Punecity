package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pranav.punecityguide.model.PuneSpot
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted
import com.pranav.punecityguide.ui.theme.BuzzTextPrimary
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    spots: List<PuneSpot>,
    isLoading: Boolean,
    currentCategory: String,
    error: String?,
    onRetry: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onSpotSelected: (PuneSpot) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    
    // Animate content appearance
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Handle pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRetry()
            delay(1000)
            isRefreshing = false
        }
    }
    
    // Filter spots by category and search
    val filteredSpots = remember(spots, currentCategory, searchQuery) {
        val categoryFiltered = when (currentCategory) {
            "All" -> spots
            else -> spots.filter { spot ->
                spot.category?.equals(currentCategory, ignoreCase = true) == true
            }
        }
        
        if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { spot ->
                spot.name.contains(searchQuery, ignoreCase = true) ||
                spot.area?.contains(searchQuery, ignoreCase = true) == true ||
                spot.description?.contains(searchQuery, ignoreCase = true) == true ||
                spot.category?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    
    val categories = listOf("All", "Heritage", "Food", "Nature", "Spiritual")

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { -40 }
                ) {
                    SmartHeader(isLoading = isLoading)
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { -30 }
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" }
                    )
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { -20 }
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(categories) { index, cat ->
                            CategoryChip(
                                label = cat,
                                isSelected = cat == currentCategory,
                                onClick = { onCategorySelected(cat) },
                                index = index
                            )
                        }
                    }
                }
            }

            // Quick Stats Row
            if (!isLoading && spots.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 20 }
                    ) {
                        QuickStatsRow(
                            totalPlaces = spots.size,
                            categories = spots.mapNotNull { it.category }.distinct().size
                        )
                    }
                }
            }

            if (isLoading) {
                item { LoadingShimmer() }
            } else if (error != null) {
                item { ErrorCard(message = error, onRetry = onRetry) }
            } else if (spots.isEmpty()) {
                item { EmptyStateCard() }
            } else if (filteredSpots.isEmpty()) {
                item { 
                    NoResultsCard(
                        searchQuery = searchQuery,
                        category = currentCategory,
                        onClearFilters = {
                            searchQuery = ""
                            onCategorySelected("All")
                        }
                    )
                }
            } else {
                item { 
                    SectionHeader(
                        title = if (searchQuery.isNotBlank()) "Search Results" else "Places in Pune",
                        subtitle = if (searchQuery.isNotBlank()) "${filteredSpots.size} places found" else "Explore the city",
                        count = filteredSpots.size
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(filteredSpots) { index, spot ->
                            SpotCard(spot = spot, onClick = onSpotSelected, index = index)
                        }
                    }
                }
                
                // Explore by Area Section
                if (searchQuery.isBlank()) {
                    val areas = spots.mapNotNull { it.area }.distinct().take(6)
                    if (areas.isNotEmpty()) {
                        item { SectionHeader(title = "Explore by Area", subtitle = "Popular neighborhoods") }
                        item { AreasGrid(areas = areas, onAreaClick = { searchQuery = it }) }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

        // Floating Refresh Button
        FloatingActionButton(
            onClick = {
                isRefreshing = true
                onRetry()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = BuzzPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isRefreshing) 0.8f else 1f,
                animationSpec = spring(dampingRatio = 0.4f),
                label = "refreshScale"
            )
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh discoveries",
                modifier = Modifier.scale(scale)
            )
        }
    }
    
    // Reset refreshing state when loading completes
    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit, index: Int = 0) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BuzzPrimary else BuzzCard,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else BuzzTextPrimary,
        label = "chipText"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "chipScale"
    )
    
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SmartHeader(isLoading: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "puneBuzz",
                    style = MaterialTheme.typography.headlineLarge,
                    color = BuzzPrimary,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Live indicator
                val pulseAlpha by animateFloatAsState(
                    targetValue = if (isLoading) 0.3f else 1f,
                    animationSpec = spring(),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha.coerceIn(0f, 1f)))
                )
            }
            Text(
                text = if (isLoading) "Refreshing..." else "Live Discoveries",
                style = MaterialTheme.typography.labelMedium,
                color = BuzzTextMuted
            )
        }
        
        // Time-based greeting badge
        val greeting = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                hour < 12 -> "☀️ Good Morning"
                hour < 17 -> "🌤️ Good Afternoon"
                hour < 21 -> "🌆 Good Evening"
                else -> "🌙 Good Night"
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BuzzCard)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.labelSmall,
                color = BuzzTextMuted
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BuzzCard)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = if (query.isNotBlank()) BuzzPrimary else BuzzTextMuted
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = BuzzTextPrimary,
                fontSize = 16.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(BuzzPrimary),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search cafes, forts, food lanes...",
                            color = BuzzTextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotBlank()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = BuzzTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, count: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = BuzzTextMuted
            )
        }
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BuzzPrimary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = BuzzPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SpotCard(spot: PuneSpot, onClick: (PuneSpot) -> Unit, index: Int = 0) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cardScale"
    )
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .scale(scale)
            .clickable { onClick(spot) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                if (spot.imageUrl != null) {
                    AsyncImage(
                        model = spot.imageUrl,
                        contentDescription = spot.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                } else {
                    // Gradient placeholder when no image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BuzzPrimary.copy(alpha = 0.7f), BuzzAccent.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Gradient overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )
                if (spot.rating != null) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = spot.rating.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Category badge at bottom left
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BuzzPrimary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = spot.category ?: "Discovery",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BuzzSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = spot.area ?: "Pune",
                        style = MaterialTheme.typography.labelMedium,
                        color = BuzzTextMuted
                    )
                }
                if (!spot.description.isNullOrBlank()) {
                    Text(
                        text = spot.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = BuzzTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BuzzCard),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tuning into Pune Buzz...", color = BuzzTextMuted)
    }
}

// Quick Stats Row
@Composable
private fun QuickStatsRow(totalPlaces: Int, categories: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBadge(
            value = totalPlaces.toString(),
            label = "Places",
            color = BuzzPrimary,
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            value = categories.toString(),
            label = "Categories",
            color = BuzzSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBadge(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BuzzTextMuted
            )
        }
    }
}

// Empty state when no data
@Composable
private fun EmptyStateCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuzzPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌆", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                text = "No discoveries available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Check back later for live updates from Pune",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// No results for search/filter
@Composable
private fun NoResultsCard(searchQuery: String, category: String, onClearFilters: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🔍", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "No matches found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = buildString {
                    if (searchQuery.isNotBlank()) append("\"$searchQuery\" ")
                    if (category != "All") append("in $category")
                }.ifBlank { "Try adjusting your filters" },
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(containerColor = BuzzPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear Filters")
            }
        }
    }
}

// Areas Grid for exploration
@Composable
private fun AreasGrid(areas: List<String>, onAreaClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        areas.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { area ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BuzzCard)
                            .clickable { onAreaClick(area) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = area,
                            style = MaterialTheme.typography.labelMedium,
                            color = BuzzTextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Fill remaining space if row has fewer than 3 items
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Connection Issue", style = MaterialTheme.typography.titleMedium, color = BuzzSecondary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = BuzzTextMuted)
            Button(onClick = onRetry) {
                Text("Reconnect")
            }
        }
    }
}