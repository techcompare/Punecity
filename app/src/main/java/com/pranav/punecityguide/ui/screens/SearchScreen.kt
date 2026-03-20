package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.ui.components.AttractionCard
import com.pranav.punecityguide.ui.components.LoadableImage
import com.pranav.punecityguide.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    database: PuneCityDatabase,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(repository, auditRepository))
    val uiState by viewModel.uiState.collectAsState()

    val popularSearches = listOf("Sinhagad", "Misal Pav", "Aga Khan Palace", "Vetal Tekdi", "FC Road")
    val trendingCategories = listOf("Historical", "Nature", "Food", "Study Spots")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                            Spacer(Modifier.width(12.dp))
                            TextField(
                                value = uiState.query,
                                onValueChange = { viewModel.updateQuery(it) },
                                placeholder = { Text("Search Pune's best...", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                ),
                                singleLine = true
                            )
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = uiState.query.isEmpty(),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "searchContentTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptySearchState(
                        popularSearches = popularSearches,
                        categories = trendingCategories,
                        onSuggestionClick = { viewModel.updateQuery(it) }
                    )
                } else {
                    SearchResultsState(
                        query = uiState.query,
                        results = uiState.results,
                        isLoading = uiState.isLoading,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    popularSearches: List<String>,
    categories: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Popular Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Popular Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                popularSearches.forEach { search ->
                    SuggestionChip(label = search, onClick = { onSuggestionClick(search) })
                }
            }
        }

        // Browse Categories
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Explore Vibrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories) { category ->
                    CategorySearchCard(category = category, onClick = { onSuggestionClick(category) })
                }
            }
        }

        Spacer(Modifier.weight(1f))
        
        // Illustration or Tip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    "Try searching for specific dishes like 'Misal' or vibes like 'Peaceful'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SearchResultsState(
    query: String,
    results: List<com.pranav.punecityguide.data.model.Attraction>,
    isLoading: Boolean,
    onNavigateToDetail: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Results for \"$query\"",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${results.size} found",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
        }

        if (results.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No places matched your vibe.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(results) { attraction ->
                    AttractionCard(
                        attraction = attraction,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategorySearchCard(category: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(width = 120.dp, height = 80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val sequences = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val mainAxisSizes = mutableListOf<Int>()

        var currentSequence = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentSequence.isNotEmpty() && currentMainAxisSize + mainAxisSpacing.toPx() + placeable.width > constraints.maxWidth) {
                sequences.add(currentSequence)
                mainAxisSizes.add(currentMainAxisSize)
                crossAxisSizes.add(currentCrossAxisSize)
                currentSequence = mutableListOf()
                currentMainAxisSize = 0
                currentCrossAxisSize = 0
            }
            currentSequence.add(placeable)
            currentMainAxisSize += (placeable.width + mainAxisSpacing.toPx()).toInt()
            currentCrossAxisSize = maxOf(currentCrossAxisSize, placeable.height)
        }
        sequences.add(currentSequence)
        mainAxisSizes.add(currentMainAxisSize)
        crossAxisSizes.add(currentCrossAxisSize)

        val totalHeight = crossAxisSizes.sum() + ((crossAxisSizes.size - 1) * crossAxisSpacing.toPx()).toInt()
        
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            sequences.forEachIndexed { i, sequence ->
                var x = 0
                sequence.forEach { placeable ->
                    placeable.place(x, y)
                    x += (placeable.width + mainAxisSpacing.toPx()).toInt()
                }
                y += (crossAxisSizes[i] + crossAxisSpacing.toPx()).toInt()
            }
        }
    }
}
