package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.data.model.Plan
import com.pranav.punecityguide.data.model.PlanPlace
import com.pranav.punecityguide.data.repository.PlanRepository
import com.pranav.punecityguide.ui.viewmodel.PlanViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.service.SupabaseClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    planId: String,
    onNavigateBack: () -> Unit,
) {
    val sessionManager = remember { SupabaseClient.getSessionManager() }
    val userId by sessionManager.userIdFlow.collectAsState(initial = "user_default")
    val repository = remember { PlanRepository() }
    val viewModel: PlanViewModel = viewModel(
        factory = PlanViewModel.factory(repository, userId ?: "user_default")
    )
    val uiState by viewModel.uiState.collectAsState()

    var showCopySuccess by remember { mutableStateOf(false) }

    LaunchedEffect(planId) {
        // Need to find the plan object first
        repository.getPublicPlans().onSuccess { list ->
            val found = list.find { it.id == planId } ?: uiState.myPlans.find { it.id == planId }
            found?.let { viewModel.selectPlan(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentPlan?.title ?: "Plan Details", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.currentPlan != null && uiState.currentPlan!!.createdBy != userId) {
                        IconButton(onClick = { 
                            viewModel.copyPlan(planId) 
                            showCopySuccess = true
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Plan")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.currentPlan != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    item {
                        PlanHeader(uiState.currentPlan!!)
                        Spacer(Modifier.height(32.dp))
                    }
                    
                    itemsIndexed(uiState.currentPlaces) { index, place ->
                        TimelineStopItem(
                            place = place,
                            isLast = index == uiState.currentPlaces.size - 1
                        )
                    }
                }
            } else {
                Text("Plan not found", modifier = Modifier.align(Alignment.Center))
            }

            if (showCopySuccess) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { showCopySuccess = false }) { Text("Dismiss") } }
                ) {
                    Text("Plan copied to your private list!")
                }
            }
        }
    }
}

@Composable
private fun PlanHeader(plan: Plan) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(plan.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(plan.duration ?: "Flexible time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (plan.description != null) {
            Spacer(Modifier.height(16.dp))
            Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun TimelineStopItem(place: PlanPlace, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            ) {}
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f).padding(bottom = 32.dp)) {
            Text(place.timeSlot ?: "Next Stop", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(place.placeName, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    if (place.description != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(place.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
