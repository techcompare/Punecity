package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.model.*
import com.pranav.punecityguide.ui.theme.*
import com.pranav.punecityguide.ui.viewmodel.ComparatorViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparatorScreen(
    viewModel: ComparatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "COMPARE CITIES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = CostPilotCyan
                )
                Text(
                    "Cost of Living",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── City Selection ──
            item {
                CitySelectionRow(
                    query1 = uiState.searchQuery1,
                    query2 = uiState.searchQuery2,
                    results1 = uiState.filteredCities1,
                    results2 = uiState.filteredCities2,
                    selected1 = uiState.selectedCity1,
                    selected2 = uiState.selectedCity2,
                    isSearching1 = uiState.isSearching1,
                    isSearching2 = uiState.isSearching2,
                    onSearch1 = viewModel::onSearchCity1,
                    onSearch2 = viewModel::onSearchCity2,
                    onSelect1 = viewModel::selectCity1,
                    onSelect2 = viewModel::selectCity2,
                    onClear1 = viewModel::clearSearch1,
                    onClear2 = viewModel::clearSearch2,
                    onSwap = viewModel::swapCities
                )
            }

            // ── Comparison Results ──
            if (uiState.comparison != null) {
                val comp = uiState.comparison!!

                // Summary Banner
                item {
                    ComparisonSummaryBanner(comp, modifier = Modifier.padding(horizontal = 20.dp))
                }

                // Category-by-category cards
                items(comp.categoryBreakdown) { cat ->
                    ComparisonCategoryCard(cat, modifier = Modifier.padding(horizontal = 20.dp))
                }

                // Trip Budget CTA
                item {
                    TripBudgetCTA(
                        city1 = comp.city1,
                        city2 = comp.city2,
                        onPlanTrip = { viewModel.showTripBudget(it) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else if (uiState.selectedCity1 == null && uiState.selectedCity2 == null) {
                // ── Popular Comparisons ──
                item {
                    Text(
                        "POPULAR COMPARISONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                items(viewModel.getPopularComparisons()) { (city1, city2) ->
                    PopularComparisonCard(
                        city1 = city1,
                        city2 = city2,
                        onClick = {
                            viewModel.selectCity1(city1)
                            viewModel.selectCity2(city2)
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        // Trip Budget Bottom Sheet
        if (uiState.showTripSheet && uiState.selectedTripCity != null) {
            TripBudgetSheet(
                tripCity = uiState.selectedTripCity!!,
                tripDays = uiState.tripDays,
                travelStyle = uiState.travelStyle,
                groupSize = uiState.groupSize,
                budget = uiState.tripBudget,
                onDaysChange = viewModel::onTripDaysChange,
                onStyleChange = viewModel::onTravelStyleChange,
                onGroupChange = viewModel::onGroupSizeChange,
                onDismiss = viewModel::hideTripSheet
            )
        }
    }
}

// ─────────────────────────────────────────────────────
// City Selection UI
// ─────────────────────────────────────────────────────

@Composable
private fun CitySelectionRow(
    query1: String, query2: String,
    results1: List<CityCost>, results2: List<CityCost>,
    selected1: CityCost?, selected2: CityCost?,
    isSearching1: Boolean, isSearching2: Boolean,
    onSearch1: (String) -> Unit, onSearch2: (String) -> Unit,
    onSelect1: (CityCost) -> Unit, onSelect2: (CityCost) -> Unit,
    onClear1: () -> Unit, onClear2: () -> Unit,
    onSwap: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // City 1 Search
        CitySearchField(
            query = query1,
            results = results1,
            placeholder = "From city...",
            isSearching = isSearching1,
            selectedCity = selected1,
            onSearch = onSearch1,
            onSelect = onSelect1,
            onClear = onClear1,
            accentColor = CostPilotCyan
        )

        // Swap Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onSwap,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = CostPilotCyan.copy(alpha = 0.1f),
                    contentColor = CostPilotCyan
                )
            ) {
                Icon(Icons.Default.SwapVert, "Swap", modifier = Modifier.size(24.dp))
            }
        }

        // City 2 Search
        CitySearchField(
            query = query2,
            results = results2,
            placeholder = "To city...",
            isSearching = isSearching2,
            selectedCity = selected2,
            onSearch = onSearch2,
            onSelect = onSelect2,
            onClear = onClear2,
            accentColor = CostPilotGold
        )
    }
}

@Composable
private fun CitySearchField(
    query: String,
    results: List<CityCost>,
    placeholder: String,
    isSearching: Boolean,
    selectedCity: CityCost?,
    onSearch: (String) -> Unit,
    onSelect: (CityCost) -> Unit,
    onClear: () -> Unit,
    accentColor: Color
) {
    Column {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                1.dp,
                if (selectedCity != null) accentColor.copy(alpha = 0.4f) else Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = if (selectedCity != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                TextField(
                    value = query,
                    onValueChange = onSearch,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (selectedCity != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Search Results Dropdown
        AnimatedVisibility(visible = isSearching && results.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                    results.forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(city) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                city.countryCode,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = accentColor
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(city.cityName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(city.country, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "$${city.midRangeDaily.toInt()}/d",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Comparison Results
// ─────────────────────────────────────────────────────

@Composable
private fun ComparisonSummaryBanner(comp: CityComparison, modifier: Modifier = Modifier) {
    val diff = comp.percentageDifference.absoluteValue
    val isCheaper = comp.percentageDifference > 0
    val bannerText = if (isCheaper) {
        "${comp.city1.cityName} is ${diff.toInt()}% cheaper than ${comp.city2.cityName}"
    } else {
        "${comp.city2.cityName} is ${diff.toInt()}% cheaper than ${comp.city1.cityName}"
    }
    val bannerGradient = if (isCheaper) GradientSuccess else GradientDanger

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(bannerGradient))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isCheaper) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        bannerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        "Based on ${comp.categoryBreakdown.size} cost categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonCategoryCard(cat: CategoryComparison, modifier: Modifier = Modifier) {
    val isMore = cat.percentDiff > 0
    val diffColor = if (isMore) CostPilotDanger else CostPilotSuccess

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        cat.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = diffColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        "${if (isMore) "+" else ""}${cat.percentDiff.toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = diffColor
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        cat.city1Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$${String.format("%.2f", cat.city1Value)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CostPilotCyan
                    )
                }
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        cat.city2Label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$${String.format("%.2f", cat.city2Value)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CostPilotGold
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularComparisonCard(
    city1: CityCost,
    city2: CityCost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${city1.cityName} vs ${city2.cityName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${city1.country} • ${city2.country}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = CostPilotCyan)
        }
    }
}

@Composable
private fun TripBudgetCTA(
    city1: CityCost,
    city2: CityCost,
    onPlanTrip: (CityCost) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(GradientPremium))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlightTakeoff, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Plan Your Trip Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Text(
                    "Get an AI-generated daily budget breakdown for your destination.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onPlanTrip(city1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(city1.cityName, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = { onPlanTrip(city2) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(city2.cityName, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Trip Budget Bottom Sheet
// ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TripBudgetSheet(
    tripCity: CityCost,
    tripDays: String,
    travelStyle: TravelStyle,
    groupSize: Int,
    budget: TripBudget?,
    onDaysChange: (String) -> Unit,
    onStyleChange: (TravelStyle) -> Unit,
    onGroupChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlightTakeoff, null, tint = CostPilotCyan, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Trip to ${tripCity.cityName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(tripCity.country, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Duration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = CostPilotCyan)
                TextField(
                    value = tripDays,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onDaysChange(it) },
                    label = { Text("Number of days") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // Travel Style
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRAVEL STYLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = CostPilotCyan)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TravelStyle.entries.forEach { style ->
                        FilterChip(
                            selected = travelStyle == style,
                            onClick = { onStyleChange(style) },
                            label = { Text("${style.emoji} ${style.label}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CostPilotCyan,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Group Size
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GROUP SIZE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = CostPilotCyan)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (groupSize > 1) onGroupChange(groupSize - 1) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text("$groupSize", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp))
                    Text("traveler${if (groupSize > 1) "s" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (groupSize < 10) onGroupChange(groupSize + 1) }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }

            // Budget Result
            if (budget != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Total
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(GradientPrimary))
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("ESTIMATED TOTAL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(
                                "$${"%,.0f".format(budget.totalBudget)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "$${"%,.0f".format(budget.dailyBudget)}/day • ${budget.durationDays} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Breakdown
                budget.breakdown.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("$${"%,.0f".format(cat.dailyCost)}/day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("$${"%,.0f".format(cat.totalCost)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
