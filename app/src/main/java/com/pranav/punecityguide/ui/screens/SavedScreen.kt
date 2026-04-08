package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranav.punecityguide.model.SavedPlace
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted
import com.pranav.punecityguide.ui.theme.BuzzTextPrimary
import kotlinx.coroutines.delay


@Composable
fun SavedScreen(
    isLoading: Boolean,
    places: List<SavedPlace>,
    error: String?,
    onRetry: () -> Unit,
    onRemovePlace: (SavedPlace) -> Unit = {},
    onSharePlace: (SavedPlace) -> Unit = {},
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showContent by remember { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<SavedPlace?>(null) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Filter places
    val filteredPlaces = remember(places, selectedFilter) {
        when (selectedFilter) {
            "All" -> places
            else -> places.filter { it.category == selectedFilter }
        }
    }
    
    // Get unique categories
    val categories = remember(places) {
        listOf("All") + places.mapNotNull { it.category }.distinct()
    }

    // Delete confirmation dialog
    if (placeToDelete != null) {
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Remove from saved?") },
            text = { Text("\"${placeToDelete?.name}\" will be removed from your collection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        placeToDelete?.let { onRemovePlace(it) }
                        placeToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { placeToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = BuzzCard
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Your Collection",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "${places.size} saved places",
                                style = MaterialTheme.typography.labelMedium,
                                color = BuzzTextMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BuzzCard)
                                    .clickable(enabled = !isLoading) { onRetry() }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(18.dp),
                                    tint = BuzzPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Stats Card (if places exist)
        if (places.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f)
                ) {
                    SavedStatsCard(places = places)
                }
            }
        }

        // Filter chips (if multiple categories)
        if (categories.size > 1) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { -20 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BuzzTextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Filter by category",
                                style = MaterialTheme.typography.labelSmall,
                                color = BuzzTextMuted
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { category ->
                                FilterChip(
                                    label = category,
                                    isSelected = category == selectedFilter,
                                    onClick = { selectedFilter = category }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Content
        when {
            isLoading -> item { SavedShimmer() }
            error != null -> item { ErrorCard(error, onRetry) }
            places.isEmpty() -> item { EmptySaved() }
            filteredPlaces.isEmpty() -> {
                item {
                    NoFilterResultsCard(
                        filter = selectedFilter,
                        onClear = { selectedFilter = "All" }
                    )
                }
            }
            else -> {
                itemsIndexed(filteredPlaces) { index, place ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 50 * (index + 1) }
                    ) {
                        PlaceCard(
                            place = place,
                            index = index,
                            onDelete = { placeToDelete = place },
                            onShare = { onSharePlace(place) }
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SavedStatsCard(places: List<SavedPlace>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BuzzPrimary.copy(alpha = 0.8f), BuzzAccent.copy(alpha = 0.8f))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = places.size.toString(),
                    label = "Saved"
                )
                StatItem(
                    value = places.mapNotNull { it.category }.distinct().size.toString(),
                    label = "Categories"
                )
                StatItem(
                    value = places.count { it.isVisited }.toString(),
                    label = "Visited"
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) BuzzPrimary else BuzzCard,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else BuzzTextPrimary,
        label = "chipText"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlaceCard(
    place: SavedPlace,
    index: Int,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardScale"
    )
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with gradient
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BuzzPrimary.copy(alpha = 0.2f),
                                    BuzzAccent.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = BuzzPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (place.subtitle != null) {
                        Text(
                            place.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = BuzzTextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (place.category != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BuzzPrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    place.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BuzzPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        if (place.isVisited) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF4ADE80).copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Visited",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp),
                        tint = BuzzTextMuted
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BuzzCard)
            )
        }
    }
}

@Composable
private fun EmptySaved() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BuzzPrimary.copy(alpha = 0.1f), BuzzAccent.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = BuzzPrimary
                )
            }
            Text(
                "Start your collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Save your favorite Pune spots to visit later. Tap the bookmark icon on any place to add it here.",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { /* Navigate to discover */ },
                colors = ButtonDefaults.buttonColors(containerColor = BuzzPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore Places")
            }
        }
    }
}

@Composable
private fun NoFilterResultsCard(filter: String, onClear: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔍", style = MaterialTheme.typography.headlineLarge)
            Text(
                "No \"$filter\" places saved",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onClear) {
                Text("Show all places", color = BuzzPrimary)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BuzzSecondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠️")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Unable to sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BuzzSecondary
                    )
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = BuzzTextMuted
                    )
                }
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = BuzzPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }
        }
    }
}

