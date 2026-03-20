package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.model.SyncAuditLog
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.data.service.BackendHealthService
import com.pranav.punecityguide.ui.viewmodel.DiagnosticViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    healthService: BackendHealthService,
    auditRepository: SyncAuditRepository,
    onNavigateBack: () -> Unit
) {
    val vm: DiagnosticViewModel = viewModel(factory = DiagnosticViewModel.factory(healthService, auditRepository))
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("System Internal Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Real-time Diagnostics & Logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { vm.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Health Section ──
            item {
                SectionHeader("Backend Service Probes", Icons.Default.CloudSync)
                HealthPanel(uiState.healthReport, uiState.isCheckingHealth)
            }

            // ── Cache Stats ──
            item {
                SectionHeader("Performance & Cache", Icons.Default.Speed)
                CacheStatsPanel(uiState.cacheStats)
            }

            // ── Recent Logs ──
            item {
                SectionHeader("Live Activity Stream", Icons.Default.ReceiptLong)
            }

            if (uiState.recentLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No audit logs available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(uiState.recentLogs) { log ->
                AuditLogItem(log)
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HealthPanel(report: BackendHealthService.HealthReport?, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
                Spacer(Modifier.height(12.dp))
            }
            
            if (report == null) {
                Text("Waiting for health probe...", style = MaterialTheme.typography.bodySmall)
            } else {
                val statusColor = when (report.overallStatus) {
                    "HEALTHY" -> Color(0xFF4CAF50)
                    "DEGRADED" -> Color(0xFFFFA000)
                    else -> Color(0xFFF44336)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.2f), modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Dns, null, modifier = Modifier.size(14.dp), tint = statusColor)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(report.overallStatus, fontWeight = FontWeight.Black, color = statusColor)
                    Spacer(Modifier.weight(1f))
                    Text(SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date(report.timestamp)), style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(Modifier.height(16.dp))

                report.checks.forEach { check ->
                    HealthCheckItem(check)
                }
            }
        }
    }
}

@Composable
private fun HealthCheckItem(check: BackendHealthService.HealthCheck) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = when (check.status) {
            "UP" -> Color(0xFF4CAF50)
            "DEGRADED" -> Color(0xFFFFA000)
            else -> Color(0xFFF44336)
        }
        
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(check.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(
            "${check.latencyMs}ms", 
            style = MaterialTheme.typography.labelSmall, 
            color = if (check.latencyMs > 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CacheStatsPanel(stats: com.pranav.punecityguide.data.service.AttractionCache.CacheStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        if (stats == null) {
            Text("No cache data...", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Capacity", "${stats.size}/500")
                StatColumn("Hit Rate", "${"%.1f".format(stats.hitRate)}%")
                StatColumn("Misses", stats.misses.toString())
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AuditLogItem(log: SyncAuditLog) {
    var expanded by remember { mutableStateOf(false) }
    
    val color = when (log.eventType) {
        "AI_QUERY_SUCCESS" -> Color(0xFF4CAF50)
        "AI_QUERY_FAILURE" -> Color(0xFFF44336)
        "SYNC_SUCCESS" -> Color(0xFF2196F3)
        "SYNC_FAILURE" -> Color(0xFFF44336)
        "HEALTH_CHECK" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.eventType,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(log.message, style = MaterialTheme.typography.bodySmall, maxLines = if (expanded) 10 else 2)
            
            if (expanded && !log.metadata.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        log.metadata,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
