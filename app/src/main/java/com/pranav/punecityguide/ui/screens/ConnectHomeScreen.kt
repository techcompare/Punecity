package com.pranav.punecityguide.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pranav.punecityguide.ui.components.LoadableImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.model.ConnectPost
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.ui.viewmodel.ConnectHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectHomeScreen(
    onNavigateToCreatePost: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onAiPrompt: (String) -> Unit = {}
) {
    val repository = remember { PuneConnectRepository() }
    val viewModel: ConnectHomeViewModel = viewModel(factory = ConnectHomeViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            totalItems > 0 && lastVisibleItemIndex >= (totalItems - 3)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Pune Connect", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Hyperlocal Updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { onAiPrompt("") }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Assistant")
                    }
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreatePost,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Post Update") },
                expanded = !scrollState.isScrollInProgress
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // V6 Social Hub Tabs
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Community Feed", "Live Lounge")
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ) 
                        }
                    )
                }
            }

            if (selectedTab == 1) {
                // Live Lounge Slot (V6 Integrated)
                CityLoungeScreen()
            } else {
                // Original Feed UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedSort == "latest",
                        onClick = { viewModel.setSortMode("latest") },
                        label = { Text("Latest") }
                    )
                    FilterChip(
                        selected = uiState.selectedSort == "trending",
                        onClick = { viewModel.setSortMode("trending") },
                        label = { Text("Trending") }
                    )
                    
                    VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableAreas) { area ->
                            FilterChip(
                                selected = uiState.selectedArea == area,
                                onClick = { 
                                    if (uiState.selectedArea == area) viewModel.setAreaFilter(null) 
                                    else viewModel.setAreaFilter(area)
                                },
                                label = { Text(area) }
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                    }
                } else if (uiState.posts.isEmpty()) {
                     Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Filled.PostAdd, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No updates in this area yet", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Be the first to post about ${uiState.selectedArea ?: "Pune"}!", 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onNavigateToCreatePost) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create First Post")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Daily AI Spark
                        item {
                            PuneDailySpark(
                                tip = uiState.dailyAiSpark,
                                onPromptSelected = { prompt -> 
                                    onAiPrompt(prompt)
                                }
                            )
                        }

                        val urgentPosts = uiState.posts.filter { (it.category ?: "") == "Alert" || (it.category ?: "") == "Traffic" }
                        val otherPosts = uiState.posts.filterNot { (it.category ?: "") == "Alert" || (it.category ?: "") == "Traffic" }

                        if (urgentPosts.isNotEmpty()) {
                            item {
                                Text(
                                    "Live Pune Updates", 
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            items(urgentPosts, key = { it.id }) { post ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    PostCard(
                                        post = post,
                                        isSaved = uiState.savedPostIds.contains(post.id),
                                        onUpvote = { viewModel.votePost(post, 1) },
                                        onDownvote = { viewModel.votePost(post, -1) },
                                        onToggleSave = { viewModel.toggleSave(post.id) },
                                        onClick = { onNavigateToDetail(post.id) }
                                    )
                                }
                            }
                        }
                        
                        item {
                            Text(
                                "Community Feed", 
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        items(otherPosts, key = { it.id }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                 PostCard(
                                    post = post,
                                    isSaved = uiState.savedPostIds.contains(post.id),
                                    onUpvote = { viewModel.votePost(post, 1) },
                                    onDownvote = { viewModel.votePost(post, -1) },
                                    onToggleSave = { viewModel.toggleSave(post.id) },
                                    onClick = { onNavigateToDetail(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: ConnectPost,
    isSaved: Boolean = false,
    onUpvote: () -> Unit = {},
    onDownvote: () -> Unit = {},
    onToggleSave: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val category = post.category ?: "General"
    val area = post.area ?: "Pune"
    val date = (post.createdAt ?: "2024-01-01").take(10)

    val isUrgent = category.equals("Traffic", ignoreCase = true) || category.equals("Alert", ignoreCase = true)
    
    val categoryColor = when (category) {
        "Traffic" -> Color(0xFFEF4444)
        "Alert" -> Color(0xFFF59E0B)
        "Food" -> Color(0xFFEC4899)
        "Flats" -> Color(0xFF8B5CF6)
        "Events" -> Color(0xFF3B82F6)
        "Students" -> Color(0xFF06B6D4)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, if (isUrgent) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column {
            // Header with User & Category
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = categoryColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$area • $date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Optional Image
            if (post.imageUrl != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))) {
                    LoadableImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        category = post.category ?: "General"
                    )
                    // Category Tag Overlay
                    Surface(
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = post.category ?: "General",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                 // Inline category tag for text-only posts
                 Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = post.category ?: "General",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description
            if (!post.description.isNullOrBlank()) {
                Text(
                    text = post.description ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // Interaction Bar
            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUpvote) {
                        Icon(Icons.Filled.ThumbUp, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "${post.score}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDownvote) {
                        Icon(Icons.Filled.ThumbDown, null, modifier = Modifier.size(20.dp))
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    TextButton(onClick = onClick) {
                        Text("View Details", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PuneDailySpark(
    tip: String? = null,
    onPromptSelected: (String) -> Unit
) {
    val displayTip = tip ?: "Did you know? Pune was once the base of the Peshwas."
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Pune Daily Spark", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("AI-curated local insights", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Text(
                displayTip,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val prompts = listOf("Top Cafes", "Student Deals", "Baner Traffic")
                prompts.forEach { tag ->
                    ElevatedAssistChip(
                        onClick = { onPromptSelected("Show me $tag in Pune") },
                        label = { Text(tag) },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
