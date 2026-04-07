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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.model.CityCost
import com.pranav.punecityguide.data.model.CommunityMessage
import com.pranav.punecityguide.data.service.*
import com.pranav.punecityguide.ui.components.*
import com.pranav.punecityguide.ui.theme.*
import com.pranav.punecityguide.ui.viewmodel.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    database: PuneCityDatabase,
    onNavigateToCompare: () -> Unit = {},
    onNavigateToTrips: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onAskAi: (String) -> Unit = {}
) {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(ServiceLocator) as T
        }
    })
    val dashState by dashboardViewModel.uiState.collectAsState()
    
    val expenseViewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.factory(database.expenseDao()))
    val expenseState by expenseViewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { DashboardHeader(dashState.userName, dashState.streak) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CostPilotCyan,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Hero Stats Card ──
            item {
                HeroStatsCard(
                    totalExpenses = expenseState.total,
                    citiesAvailable = dashState.trendingCities.size,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // ── Daily Missions ──
            item {
                DailyMissionsSection(dashState.missions)
            }

            // ── Spot of the Day / Pulse ──
            item {
                SpotOfTheDaySection(
                    city = dashState.spotOfTheDay,
                    pulse = dashState.pulseData,
                    onClick = onNavigateToCompare
                )
            }

            // ── Community Snippet ──
            item {
                CommunitySnippetSection(
                    message = dashState.latestCommunityMessage,
                    onClick = onNavigateToInsights
                )
            }

            // ── Quick Actions ──
            item {
                ActionGridSection(
                    onNavigateToCompare, onNavigateToTrips, onAskAi, onNavigateToInsights
                )
            }

            // ── Recent Expenses ──
            item {
                DashboardSectionHeader("LATEST TRACKING")
            }

            if (expenseState.expenses.isEmpty()) {
                item { DashboardEmptyState() }
            } else {
                items(expenseState.expenses.take(3), key = { it.id }) { expense ->
                    ExpenseItemCard(
                        expense = expense,
                        onDelete = { expenseViewModel.deleteExpense(expense) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, cat ->
                expenseViewModel.addExpense(title, amount, cat)
                showAddDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────
// Retention Components
// ─────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(userName: String, streak: Int) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            else -> "Evening"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "COSTPILOT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = CostPilotCyan
            )
            Text(
                text = "Good $greeting, $userName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
        }
        
        // Streak counter - highly visible
        Surface(
            shape = CircleShape,
            color = if (streak > 0) Color(0xFFFF9800) else Color.Gray.copy(alpha = 0.1f),
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = if (streak > 0) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                Text(
                    text = "$streak",
                    color = if (streak > 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DailyMissionsSection(missions: MissionsState) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardSectionHeader("DAILY MISSIONS", modifier = Modifier.padding(0.dp))
            Text(
                "${missions.totalDone}/3 DONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (missions.allDone) CostPilotSuccess else CostPilotCyan
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MissionIcon(Icons.Default.Compare, "Compare", missions.compareDone)
                MissionIcon(Icons.Default.AddBusiness, "Track", missions.expenseDone)
                MissionIcon(Icons.Default.Forum, "Community", missions.communityDone)
            }
        }
    }
}

@Composable
private fun MissionIcon(icon: ImageVector, label: String, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (isDone) CostPilotSuccess else Color.Gray.copy(alpha = 0.2f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDone) Color.White else Color.Gray
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpotOfTheDaySection(city: CityCost?, pulse: PulseService.PulseData?, onClick: () -> Unit) {
    if (city == null) return
    
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        DashboardSectionHeader("SPOT OF THE DAY", modifier = Modifier.padding(0.dp))
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shadowElevation = 8.dp
        ) {
            Box {
                // Background image placeholder with gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                )
                
                // Weather / Pulse overlay
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.3f)
                        ) {
                            Text(
                                "LIVE PULSE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        if (pulse != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${pulse.temp}°", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(pulse.condition, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    
                    Column {
                        Text(
                            city.cityName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black,
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(city.country, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySnippetSection(message: CommunityMessage?, onClick: () -> Unit) {
    if (message == null) return
    
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardSectionHeader("COMMUNITY PULSE", modifier = Modifier.padding(0.dp))
            Text(
                "JOIN CHAT",
                modifier = Modifier.clickable { onClick() },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CostPilotCyan
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = CostPilotCyan.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(message.userName.take(1), fontWeight = FontWeight.Bold, color = CostPilotCyan)
                    }
                }
                Column {
                    Text(message.userName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        message.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionGridSection(onComp: () -> Unit, onTrip: () -> Unit, onAi: (String) -> Unit, onChat: () -> Unit) {
    DashboardSectionHeader("QUICK ACCESS")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ActionCard("Compare", Icons.Default.CompareArrows, GradientPrimary, onComp) }
        item { ActionCard("Plan", Icons.Default.FlightTakeoff, GradientGold, onTrip) }
        item { ActionCard("AI Advisor", Icons.Default.AutoAwesome, GradientPremium) { onAi("Give me a travel budget tip") } }
        item { ActionCard("Insights", Icons.Default.Insights, GradientSuccess, onChat) }
    }
}

@Composable
private fun ActionCard(title: String, icon: ImageVector, gradient: List<Color>, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(120.dp).height(100.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.background(Brush.linearGradient(gradient)).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(title: String, modifier: Modifier = Modifier.padding(horizontal = 20.dp)) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}

@Composable
private fun DashboardEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = CostPilotCyan.copy(alpha = 0.08f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FlightTakeoff, null, modifier = Modifier.size(36.dp), tint = CostPilotCyan.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Ready for takeoff!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text("Start tracking expenses from\nyour next adventure.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}

@Composable
private fun HeroStatsCard(totalExpenses: Double, citiesAvailable: Int, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, CostPilotCyan.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.background(Brush.linearGradient(GradientPrimary)).padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TOTAL TRACKED", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$%,.2f".format(totalExpenses), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-1).sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(alpha = 0.12f), CircleShape).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(CostPilotSuccess, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("$citiesAvailable Global Cities Live", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
