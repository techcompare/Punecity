package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.ui.components.LoadableImage
import com.pranav.punecityguide.ui.components.ShimmerBox
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.ui.viewmodel.HomeViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: PuneCityDatabase,
    healthService: com.pranav.punecityguide.data.service.BackendHealthService,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    onNavigateToSecret: () -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    onAskAi: (String) -> Unit = {}
) {
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val context = LocalContext.current
    val prefManager = remember { com.pranav.punecityguide.data.service.PreferenceManager(context) }
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            context.applicationContext as android.app.Application,
            repository,
            auditRepository,
            healthService,
            prefManager
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showOnboarding) {
        OnboardingScreen(onFinished = { viewModel.completeOnboarding() })
        return
    }

    if (uiState.isLoading && uiState.topAttractions.isEmpty()) {
        HomeSkeleton()
        return
    }

    if (uiState.topAttractions.isEmpty() && !uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error ?: "No attractions found.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.refresh() }) {
                    Text("Refresh")
                }
            }
        }
        return
    }

    val categories = remember(uiState.categories) {
        uiState.categories.filter { it.isNotBlank() }.ifEmpty {
            listOf("Street Food", "Historical", "Nature", "Modern", "Religious", "Adventure")
        }
    }

    Scaffold(
        topBar = {
            HomeHeader(
                greeting = getGreeting(),
                userName = uiState.passport.levelName,
                onSearchClick = onNavigateToSearch
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Today in Pune (compact) ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TodayInPuneCard(onAskAi = onAskAi)
            }

            // ── 2. Quick Plans Row ──
            item {
                SectionHeader("Ready-to-go Plans")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val plans = getQuickPlans()
                    itemsIndexed(plans) { _, plan ->
                        QuickPlanCard(plan = plan, onClick = { onAskAi(plan.query) })
                    }
                }
            }

            // ── AI & Master Tools ──
            item {
                SectionHeader("AI & Master Tools")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DiscoveryFeatureCard(
                            title = "Scan to Plan",
                            description = "Convert tickets into plans",
                            icon = Icons.Default.QrCodeScanner,
                            color = Color(0xFFA64DFF),
                            onClick = onNavigateToScan
                        )
                    }
                    item {
                        DiscoveryFeatureCard(
                            title = "Secret Spots",
                            description = "Hidden views & gems",
                            icon = Icons.Default.VisibilityOff,
                            color = Color(0xFF00C853),
                            onClick = onNavigateToSecret
                        )
                    }
                    item {
                        DiscoveryFeatureCard(
                            title = "Plan Archives",
                            description = "Your saved itineraries",
                            icon = Icons.Default.History,
                            color = Color(0xFF2962FF),
                            onClick = onNavigateToPlans
                        )
                    }
                }
            }

            // ── 3. Categories ──
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val categories = listOf("Historical", "Nature", "Study Spots", "Hidden Views", "Nightlife", "Street Food")
                    itemsIndexed(categories) { _, category ->
                        val emoji = getCategoryEmoji(category)
                        CategoryChip(
                            emoji = emoji,
                            label = category,
                            onClick = { onNavigateToCategory(category) }
                        )
                    }
                }
            }

            // ── 4. Student Hangouts (Removed hardcoded section) ──
            // if (uiState.studentHangouts.isNotEmpty()) {
            //    item {
            //        SectionHeader("Student Hangouts \uD83C\uDF93")
            //        LazyRow(
            //            contentPadding = PaddingValues(horizontal = 20.dp),
            //            horizontalArrangement = Arrangement.spacedBy(12.dp)
            //        ) {
            //            itemsIndexed(emptyList<Attraction>()) { _, spot ->
            //                // StudentHangoutCard(spot, onClick = { onNavigateToDetail(spot.id) })
            //            }
            //        }
            //    }
            // }

            // ── 5. Trending ──
            item {
                SectionHeader("Trending Now")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val trending = uiState.topAttractions.sortedByDescending { it.reviewCount }.take(8)
                    itemsIndexed(trending, key = { _, a -> "trend_${a.id}" }) { index, attraction ->
                        TrendingCard(attraction = attraction, rank = index + 1, onClick = { onNavigateToDetail(attraction.id) })
                    }
                }
            }

            // ── 6. All Places ──
            item {
                SectionHeader("All Places")
            }

            itemsIndexed(uiState.topAttractions, key = { _, a -> "all_${a.id}" }) { _, attraction ->
                CompactPlaceCard(
                    attraction = attraction,
                    onClick = { onNavigateToDetail(attraction.id) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

// ═
// COMPONENTS
// 

@Composable
fun HomeHeader(
    greeting: String,
    userName: String?,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (userName != null) "$greeting, $userName!" else greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Explore the best of Pune today!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            // Real User Profile
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userName?.firstOrNull()?.toString()?.uppercase() ?: "P", 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Modern Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(onClick = onSearchClick),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Search places, food, vibes...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        onClick = onClick,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StudentHangoutCard(spot: Attraction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadableImage(
                model = spot.imageUrl,
                contentDescription = spot.name,
                modifier = Modifier.fillMaxSize(),
                height = 160,
                contentScale = ContentScale.Crop,
                category = spot.category
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = spot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = spot.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (spot.entryFee.lowercase().contains("free")) "FREE" else spot.entryFee,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
            
            // "Popular" Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp).size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun TrendingCard(
    attraction: Attraction,
    rank: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(260.dp)
            .height(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadableImage(
                model = attraction.imageUrl,
                contentDescription = attraction.name,
                modifier = Modifier.fillMaxSize(),
                height = 180,
                contentScale = ContentScale.Crop,
                category = attraction.category
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 60f
                        )
                    )
            )
            // Rank badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    "#$rank",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            // Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    attraction.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${attraction.rating}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        attraction.category,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactPlaceCard(
    attraction: Attraction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadableImage(
                model = attraction.imageUrl,
                contentDescription = attraction.name,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                height = 96,
                contentScale = ContentScale.Crop,
                category = attraction.category
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    attraction.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    attraction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${attraction.rating}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• ${attraction.category}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val isFree = attraction.entryFee.contains("free", ignoreCase = true)
                    if (isFree) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                "FREE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header shimmer
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.width(160.dp).height(24.dp), cornerRadius = 8f)
                ShimmerBox(modifier = Modifier.width(220.dp).height(16.dp), cornerRadius = 8f)
            }
            ShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 20f)
        }
        
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(52.dp), cornerRadius = 26f)
        
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(140.dp), cornerRadius = 16f)
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { ShimmerBox(modifier = Modifier.width(90.dp).height(38.dp), cornerRadius = 12f) }
        }
        
        repeat(3) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(96.dp), cornerRadius = 16f)
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        hour < 21 -> "Good Evening"
        else -> "Good Night"
    }
}

private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "street food" -> "\uD83C\uDF5C"
        "food" -> "\uD83C\uDF5B"
        "nature" -> "\uD83C\uDF3F"
        "historical" -> "\uD83C\uDFDB"
        "adventure" -> "\uD83E\uDDF3"
        "religious" -> "\uD83D\uDE4F"
        "spiritual" -> "\uD83D\uDD49"
        "modern" -> "\uD83C\uDFD9"
        "hidden views" -> "\uD83D\uDC40"
        "study spots" -> "\uD83D\uDCDA"
        "cafes" -> "\u2615"
        "nightlife" -> "\uD83C\uDF1F"
        "parks" -> "\uD83C\uDFDE"
        "museums" -> "\uD83C\uDFDB"
        "forts" -> "\u2694"
        else -> "\uD83D\uDCCD"
    }
}

// ─────────────────────────────────────────────
// TODAY IN PUNE — Smart Utility Card
// ─────────────────────────────────────────────

@Composable
private fun TodayInPuneCard(onAskAi: (String) -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val month = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

    val season = when (month) {
        in 6..9 -> "Monsoon" to "🌧️"
        in 10..11 -> "Post-Monsoon" to "🌤️"
        12, 1, 2 -> "Winter" to "❄️"
        else -> "Summer" to "☀️"
    }
    val seasonTip = when (season.first) {
        "Monsoon" -> "Waterfalls are alive! Best time for Mulshi, Bhushi Dam. Avoid open forts."
        "Post-Monsoon" -> "Perfect weather. Forts & nature at their greenest. Ideal trekking season."
        "Winter" -> "Cool & pleasant. Morning fog at Sinhagad. Great for early sunrise treks."
        else -> "Hot afternoons. Stick to cafes, museums, and evening spots after 5 PM."
    }

    val crowdStatus = when {
        isWeekend && hour in 10..18 -> "🔴 High crowd at popular spots" to Color(0xFFFF5252)
        isWeekend -> "🟡 Moderate crowd expected" to Color(0xFFFFB300)
        hour in 8..10 || hour in 17..20 -> "🟡 Moderate — rush hour nearby" to Color(0xFFFFB300)
        else -> "🟢 Low crowd — great time to go!" to Color(0xFF4CAF50)
    }

    val timeSlot = when {
        hour < 10 -> "Morning" to "🌅" to "Breakfast spots, morning walks, gardens"
        hour < 14 -> "Late Morning" to "⛅" to "Heritage sites, museums, temples"
        hour < 17 -> "Afternoon" to "🌞" to "Cafes, shopping, indoor attractions"
        hour < 20 -> "Evening" to "🌇" to "Sunset viewpoints, street food, malls"
        else -> "Night" to "🌙" to "Restaurants, rooftops, late-night spots"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📍", fontSize = 18.sp)
                    Text(
                        "Today in Pune",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = crowdStatus.second.copy(alpha = 0.15f)
                ) {
                    Text(
                        crowdStatus.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = crowdStatus.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Compact info strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(season.second, fontSize = 18.sp)
                        Text(season.first, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(timeSlot.first.second, fontSize = 18.sp)
                        Text(timeSlot.first.first, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(seasonTip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }

            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAskAi("What should I do in Pune right now? It's ${timeSlot.first.first.lowercase()} and ${season.first} season. Give me 3 specific places with exact cost.") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("What to do now", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { onAskAi("Plan my full day in Pune today — ${timeSlot.first.first.lowercase()} slot, ${season.first} weather, with budget breakdown. Make it practical.") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Plan my day", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// QUICK PLAN CARDS
// ─────────────────────────────────────────────

private data class QuickPlan(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val duration: String,
    val budget: String,
    val gradient: List<Long>,
    val query: String
)

private fun getQuickPlans(): List<QuickPlan> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

    return buildList {
        if (isWeekend) {
            add(QuickPlan("🏔️", "Weekend Trek", "Sinhagad or Torna Fort", "Full Day", "₹500", listOf(0xFF1A237E, 0xFF3949AB),
                "Plan a weekend trek near Pune — Sinhagad or Rajmachi. Include transport, food, total cost breakdown."))
        }
        add(QuickPlan("🍛", "Pune Food Trail", "FC Road → Camp → MG Road", "4 Hours", "₹600", listOf(0xFF4A148C, 0xFF7B1FA2),
            "Plan a food trail in Pune starting from FC Road to Camp. List 5 must-try dishes, exact shops, and total cost."))
        add(QuickPlan("🏛️", "Heritage Walk", "Shaniwar Wada to Aga Khan", "3 Hours", "₹150", listOf(0xFF880E4F, 0xFFE91E63),
            "Plan a heritage walk in Pune old city visiting Shaniwar Wada, Lal Mahal, Kasba Ganpati, Aga Khan Palace. Include travel tips and entry fees."))
        add(QuickPlan("☕", "Café Crawl", "Koregaon Park cafes", "2-3 Hours", "₹800", listOf(0xFF1B5E20, 0xFF388E3C),
            "Plan a café hopping tour in Koregaon Park Pune — list top 4 cafes, what to order at each, total spend."))
        add(QuickPlan("💰", "₹300 Day", "Full day for ₹300", "Full Day", "₹300", listOf(0xFFE65100, 0xFFF57C00),
            "Plan a complete day in Pune on a ₹300 budget — breakfast, sightseeing, lunch, evening activity. Be very specific with locations and prices."))
        add(QuickPlan("🌅", "Sunset Views", "Best viewpoints today", "Evening", "₹100", listOf(0xFF37474F, 0xFF546E7A),
            "What are the best sunset viewpoints in Pune? List top 5 with how to reach them and when to go."))
    }
}

@Composable
private fun QuickPlanCard(plan: QuickPlan, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(plan.gradient.map { Color(it) }),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.emoji, fontSize = 28.sp)
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    plan.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            plan.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            plan.budget,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tap for AI plan", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F))
                }
            }
        }
    }
}

@Composable
private fun DiscoveryFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 2)
            }
        }
    }
}
