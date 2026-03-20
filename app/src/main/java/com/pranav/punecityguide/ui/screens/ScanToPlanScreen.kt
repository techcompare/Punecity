package com.pranav.punecityguide.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.ItineraryRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.ui.components.ErrorMessage
import com.pranav.punecityguide.ui.viewmodel.ScanToPlanViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToPlanScreen(
    database: PuneCityDatabase,
    onNavigateBack: () -> Unit,
) {
    val attractionRepo = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val itineraryRepo = remember { ItineraryRepository(database.itineraryDao(), SyncAuditRepository(database.syncAuditDao())) }
    val auditRepo = remember { SyncAuditRepository(database.syncAuditDao()) }
    val vm: ScanToPlanViewModel = viewModel(factory = ScanToPlanViewModel.factory(attractionRepo, itineraryRepo, auditRepo))
    val uiState by vm.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // Legacy: Store current URI for TakePicture
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val scanPulse = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(uiState.isWorking) {
        if (uiState.isWorking) {
            scanPulse.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                )
            )
        } else {
            scanPulse.snapTo(0f)
        }
    }

    fun createTempPictureUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("SCAN_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            vm.scan(bitmap)
            title = "Pune Discovery"
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                val bitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (e: Exception) {
                    null
                }
                bitmap?.let { vm.scan(it); title = "Pune Discovery" }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempPictureUri()
            capturedImageUri = uri
            takePicture.launch(uri)
        } else {
            vm.handlePermissionDenied("Camera access is vital for the AI discovery engine.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI Vision ⚡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Instant City Integration", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── AI Preview Window ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black)
            ) {
                if (uiState.capturedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = uiState.capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Scanning",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    if (uiState.isWorking) {
                        // Scanning Line Pulse
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 300.dp * scanPulse.value)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Cyan, Color.White, Color.Cyan, Color.Transparent)
                                    )
                                )
                                .shadow(8.dp, spotColor = Color.Cyan)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Point at a ticket or sign", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Action Buttons ──
                if (!uiState.isWorking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            onClick = { 
                                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    val uri = createTempPictureUri()
                                    capturedImageUri = uri
                                    takePicture.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Capture", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Surface(
                            onClick = { pickImage.launch("image/*") },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Image, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Gallery", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── AI Result Section ──
                AnimatedVisibility(
                    visible = uiState.recognizedText.isNotEmpty() || uiState.error != null,
                    enter = expandVertically() + fadeIn()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (uiState.error != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(12.dp))
                                    Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (uiState.matchedAttraction != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("AI MATCH FOUND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                }
                                
                                com.pranav.punecityguide.ui.components.AttractionCard(
                                    attraction = uiState.matchedAttraction!!,
                                    onNavigateToDetail = { /* Option to view full details */ }
                                )
                                
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("Save to Itinerary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                        OutlinedTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            placeholder = { Text("Trip Title (e.g. My Visit to ${uiState.matchedAttractionName})") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Button(
                                            onClick = { vm.savePlan(title, notes) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            enabled = uiState.savedItineraryId == null
                                        ) {
                                            Text(if (uiState.savedItineraryId == null) "Confirm Plan" else "Stored Successfully ✅", fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        } else if (uiState.recognizedText.isNotEmpty()) {
                            // Raw text result if no attraction matched
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Insights Extracted", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                    Text(uiState.recognizedText, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                                    Text("No exact landmark match, but text logged.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

