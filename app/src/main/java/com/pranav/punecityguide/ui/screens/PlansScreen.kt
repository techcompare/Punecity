package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pranav.punecityguide.model.CuratedPlan
import com.pranav.punecityguide.model.PuneSpot
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted

data class PlanCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
)

@Composable
fun PlansScreen(
    plans: List<CuratedPlan>,
    isLoading: Boolean,
    onPlanSelected: (CuratedPlan) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf(
        PlanCategory("all", "All", Icons.Default.Explore),
        PlanCategory("cafe", "Cafe Hopping", Icons.Default.LocalCafe),
        PlanCategory("food", "Food Trail", Icons.Default.Restaurant),
        PlanCategory("nature", "Nature", Icons.Default.Nature),
        PlanCategory("heritage", "Heritage", Icons.Default.Map),
    )
    
    val filteredPlans = if (selectedCategory == null || selectedCategory == "all") {
        plans
    } else {
        plans.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(
                    "Curated Plans",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Ready-to-follow itineraries for Pune",
                    color = BuzzTextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category.id || 
                        (selectedCategory == null && category.id == "all")
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category.id },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BuzzPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = BuzzPrimary,
                            selectedLeadingIconColor = BuzzPrimary,
                        )
                    )
                }
            }
        }
        
        if (isLoading) {
            item { PlansLoadingState() }
        } else if (filteredPlans.isEmpty()) {
            item { PlansEmptyState() }
        } else {
            items(filteredPlans, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    onClick = { onPlanSelected(plan) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PlanCard(
    plan: CuratedPlan,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Column {
            if (!plan.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    AsyncImage(
                        model = plan.imageUrl,
                        contentDescription = plan.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = null, // Gradient overlay will show on error
                        placeholder = null
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                    startY = 100f
                                )
                            )
                    )
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanBadge(
                            icon = Icons.Default.AccessTime,
                            text = "${plan.durationHours}h"
                        )
                        if (plan.estimatedCost != null) {
                            PlanBadge(
                                icon = Icons.Default.CurrencyRupee,
                                text = "${plan.estimatedCost}"
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            plan.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            plan.category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = BuzzPrimary
                        )
                    }
                }
                
                Text(
                    plan.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BuzzTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                if (plan.imageUrl.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BuzzTextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${plan.durationHours} hours",
                                style = MaterialTheme.typography.labelMedium,
                                color = BuzzTextMuted
                            )
                        }
                        if (plan.estimatedCost != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BuzzTextMuted
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "${plan.estimatedCost}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BuzzTextMuted
                                )
                            }
                        }
                    }
                }
                
                if (plan.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(plan.tags.take(4)) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BuzzAccent.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BuzzAccent
                                )
                            }
                        }
                    }
                }
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BuzzPrimary)
                ) {
                    Text("View Plan", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanBadge(
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlansLoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading plans...", color = BuzzTextMuted)
    }
}

@Composable
private fun PlansEmptyState() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuzzSecondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = BuzzSecondary
                )
            }
            Text(
                "No Plans Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Check back soon for curated Pune itineraries!",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
