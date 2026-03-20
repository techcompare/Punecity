package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.pranav.punecityguide.ui.components.ErrorMessage
import com.pranav.punecityguide.ui.components.LoadableImage
import com.pranav.punecityguide.ui.components.LoadingPlaceholder
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.repository.RecentlyViewedRepository
import com.pranav.punecityguide.ui.viewmodel.DetailViewModel
import com.pranav.punecityguide.util.ShareHelper
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    attractionId: Int,
    database: PuneCityDatabase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val recentlyViewedRepository = remember { RecentlyViewedRepository(database.recentlyViewedDao(), SyncAuditRepository(database.syncAuditDao())) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.factory(repository, recentlyViewedRepository, attractionId, auditRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Fetch nearby places
    val allAttractions by repository.getTopAttractions(50).collectAsState(initial = emptyList())
    val nearbyPlaces = remember(uiState.attraction, allAttractions) {
        val current = uiState.attraction ?: return@remember emptyList()
        allAttractions
            .filter { it.id != current.id && it.latitude != 0.0 }
            .map { it to haversineKm(current.latitude, current.longitude, it.latitude, it.longitude) }
            .filter { it.second < 8.0 } // within 8km
            .sortedBy { it.second }
            .take(5)
    }

    val isFavorite = uiState.attraction?.isFavorite == true
    val favoriteIconScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favoriteIconScale"
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            uiState.attraction?.name ?: "Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF6B6B) else Color.White,
                                modifier = Modifier.scale(favoriteIconScale)
                            )
                        }
                        IconButton(onClick = { uiState.attraction?.let { ShareHelper.shareAttraction(context, it) } }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            uiState.error != null -> ErrorMessage(
                error = uiState.error ?: "Unknown error",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            uiState.attraction != null -> DetailScreenContent(
                attraction = uiState.attraction!!,
                nearbyPlaces = nearbyPlaces,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Attraction not found", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DetailScreenContent(
    attraction: Attraction,
    nearbyPlaces: List<Pair<Attraction, Double>>,
    modifier: Modifier = Modifier
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Hero Image with gradient overlay ──
        item {
            Box(modifier = Modifier.height(340.dp)) {
                LoadableImage(
                    model = attraction.imageUrl,
                    contentDescription = attraction.name,
                    modifier = Modifier.fillMaxSize(),
                    height = 340,
                    contentScale = ContentScale.Crop,
                    category = attraction.category
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                startY = 0f,
                                endY = Float.MAX_VALUE
                            )
                        )
                )
                // Rating & review badge on image
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        Text("${attraction.rating}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("(${formatReviewCount(attraction.reviewCount)})", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
                // Category chip
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = attraction.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Title & Native name ──
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                if (attraction.nativeLanguageName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = attraction.nativeLanguageName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Quick Stats Row ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatChip(
                    icon = Icons.Filled.Star,
                    label = "${attraction.rating}",
                    color = Color(0xFFFFC107),
                    modifier = Modifier.weight(1f)
                )
                QuickStatChip(
                    icon = Icons.Filled.People,
                    label = formatReviewCount(attraction.reviewCount),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QuickStatChip(
                    icon = Icons.Filled.AttachMoney,
                    label = if (attraction.entryFee.contains("free", ignoreCase = true)) "Free" else attraction.entryFee.take(10),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Info Card ──
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(icon = Icons.Filled.CalendarMonth, label = "Best Time", value = attraction.bestTimeToVisit)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    InfoRow(icon = Icons.Filled.AttachMoney, label = "Entry Fee", value = attraction.entryFee)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    InfoRow(icon = Icons.Filled.AccessTime, label = "Opening Hours", value = attraction.openingHours)
                }
            }
        }

        // ── Description ──
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = attraction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (attraction.description.length > 200) {
                    Text(
                        text = if (isDescriptionExpanded) "Show less" else "Read more",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // ── Action Buttons ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val context = LocalContext.current
                ElevatedButton(
                    onClick = {
                        val uri = "geo:${attraction.latitude},${attraction.longitude}?q=${Uri.encode(attraction.name)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        intent.setPackage("com.google.android.apps.maps")
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Directions")
                }

                Button(
                    onClick = {
                        // Open Google Maps navigation
                        val uri = "google.navigation:q=${attraction.latitude},${attraction.longitude}&mode=d"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        intent.setPackage("com.google.android.apps.maps")
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Navigate")
                }
            }
        }

        // ── Nearby Places ──
        if (nearbyPlaces.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nearby Places",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    items(nearbyPlaces) { (place, distanceKm) ->
                        NearbyPlaceCard(place, distanceKm)
                    }
                }
            }
        }

        // ── Bottom spacer ──
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NearbyPlaceCard(attraction: Attraction, distanceKm: Double) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Column {
            Box {
                LoadableImage(
                    model = attraction.imageUrl,
                    contentDescription = attraction.name,
                    modifier = Modifier.fillMaxWidth(),
                    height = 100,
                    contentScale = ContentScale.Crop,
                    category = attraction.category
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "%.1f km".format(distanceKm),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    attraction.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    attraction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Haversine distance calculation ──
private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}

private fun formatReviewCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> "$count"
    }
}
