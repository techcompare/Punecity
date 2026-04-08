package com.pranav.punecityguide.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.pranav.punecityguide.model.ProfileData
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    isLoading: Boolean,
    profile: ProfileData?,
    error: String?,
    isGuest: Boolean,
    sessionDisplayName: String,
    sessionEmail: String?,
    postsCount: Int = 0,
    savedCount: Int = 0,
    visitsCount: Int = 0,
    xp: Int = 0,
    updateSuccess: String? = null,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToAuth: () -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onUpdatePhoto: (Uri) -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onClearUpdateSuccess: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
) {
    var showContent by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(sessionDisplayName) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUpdatePhoto(it) }
    }
    
    // Show success message
    LaunchedEffect(updateSuccess) {
        updateSuccess?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                onClearUpdateSuccess()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Update edit text when session name changes
    LaunchedEffect(sessionDisplayName) {
        editNameText = sessionDisplayName
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    // Calculate level from XP
    val level = when {
        xp >= 500 -> 5
        xp >= 200 -> 4
        xp >= 100 -> 3
        xp >= 50 -> 2
        xp >= 10 -> 1
        else -> 0
    }
    val levelName = when (level) {
        0 -> "Newcomer"
        1 -> "Explorer"
        2 -> "Local"
        3 -> "Expert"
        4 -> "Ambassador"
        else -> "Legend"
    }
    val nextLevelXp = when (level) {
        0 -> 10
        1 -> 50
        2 -> 100
        3 -> 200
        4 -> 500
        else -> 1000
    }
    val prevLevelXp = when (level) {
        0 -> 0
        1 -> 10
        2 -> 50
        3 -> 100
        4 -> 200
        else -> 500
    }
    val progress = if (nextLevelXp > prevLevelXp) {
        ((xp - prevLevelXp).toFloat() / (nextLevelXp - prevLevelXp)).coerceIn(0f, 1f)
    } else 1f
    
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400)) + slideInVertically { -40 }
                ) {
                    Text(
                        "Pune Passport",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            // Profile Card
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500)) + slideInVertically { -30 }
                ) {
                    ProfileCard(
                        displayName = profile?.displayName ?: sessionDisplayName,
                        profilePhotoUri = profile?.profilePhotoUri,
                        email = profile?.email ?: sessionEmail,
                        isGuest = isGuest,
                        level = level,
                        levelName = levelName,
                        floatOffset = floatOffset,
                        onSwitchToAuth = onSwitchToAuth,
                        onEditName = { showEditNameDialog = true },
                        onEditPhoto = { showPhotoOptionsDialog = true }
                    )
                }
            }
        
        // XP Progress Card
        if (!isGuest) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600)) + slideInVertically { -20 }
                ) {
                    XPProgressCard(
                        xp = xp,
                        level = level,
                        levelName = levelName,
                        progress = progress,
                        nextLevelXp = nextLevelXp
                    )
                }
            }
        }
        
        // Stats Grid
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(700)) + slideInVertically { -10 }
            ) {
                StatsGrid(
                    postsCount = postsCount,
                    savedCount = savedCount,
                    visitsCount = visitsCount
                )
            }
        }
        
        // Achievements Section
        if (!isGuest && (postsCount > 0 || savedCount > 0)) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(800))
                ) {
                    AchievementsCard(postsCount = postsCount, savedCount = savedCount)
                }
            }
        }
        
        // Guest CTA
        if (isGuest) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(800))
                ) {
                    GuestUpgradeCard(onSwitchToAuth = onSwitchToAuth)
                }
            }
        }
        
        // Settings Section
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(900))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BuzzTextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        onClick = onOpenPrivacyPolicy
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = "Terms of Service",
                        onClick = onOpenTerms
                    )
                    
                    val context = LocalContext.current
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Contact Support",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:workwithme785@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Pune Buzz Support")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
        
        // Logout Button
        item {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(1000))
            ) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isGuest) "Exit Guest Mode" else "Sign Out",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Version
        item {
            Text(
                "Pune Buzz v10",
                style = MaterialTheme.typography.labelSmall,
                color = BuzzTextMuted.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
    
    // Snackbar Host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { 
                Text(
                    "Edit Display Name",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { if (it.length <= 30) editNameText = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuzzPrimary,
                            focusedLabelColor = BuzzPrimary,
                            cursorColor = BuzzPrimary
                        ),
                        supportingText = { 
                            Text(
                                "${editNameText.length}/30",
                                color = if (editNameText.length < 2) BuzzSecondary else BuzzTextMuted
                            ) 
                        }
                    )
                    if (editNameText.trim().length < 2) {
                        Text(
                            "Name must be at least 2 characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = BuzzSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.trim().length >= 2) {
                            onUpdateDisplayName(editNameText.trim())
                            showEditNameDialog = false
                        }
                    },
                    enabled = editNameText.trim().length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = BuzzPrimary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    editNameText = sessionDisplayName
                    showEditNameDialog = false 
                }) {
                    Text("Cancel", color = BuzzTextMuted)
                }
            }
        )
    }
    
    // Photo Options Dialog
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { 
                Text(
                    "Profile Photo",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Choose from Gallery
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptionsDialog = false
                                imagePickerLauncher.launch("image/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = BuzzPrimary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = BuzzPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Choose from Gallery",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Remove Photo (only if photo exists)
                    if (profile?.profilePhotoUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPhotoOptionsDialog = false
                                    onRemovePhoto()
                                },
                            colors = CardDefaults.cardColors(containerColor = BuzzSecondary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = BuzzSecondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Remove Photo",
                                    fontWeight = FontWeight.Medium,
                                    color = BuzzSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Cancel", color = BuzzTextMuted)
                }
            }
        )
    }
    } // End of Box
}

@Composable
private fun ProfileCard(
    displayName: String,
    profilePhotoUri: String?,
    email: String?,
    isGuest: Boolean,
    level: Int,
    levelName: String,
    floatOffset: Float,
    onSwitchToAuth: () -> Unit,
    onEditName: () -> Unit = {},
    onEditPhoto: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            // Decorative background circles
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = (-30).dp, y = (-30).dp + floatOffset.dp)
                    .alpha(0.1f)
                    .background(BuzzPrimary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 40.dp - floatOffset.dp)
                    .alpha(0.08f)
                    .background(BuzzAccent, CircleShape)
            )
            
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with photo
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Profile Photo or Initial
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                if (profilePhotoUri == null) {
                                    Brush.linearGradient(listOf(BuzzPrimary, BuzzAccent))
                                } else {
                                    Brush.linearGradient(listOf(BuzzPrimary.copy(alpha = 0.3f), BuzzAccent.copy(alpha = 0.3f)))
                                }
                            )
                            .clickable { onEditPhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhotoUri != null) {
                            // Display actual photo using Coil
                            AsyncImage(
                                model = profilePhotoUri,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                displayName.firstOrNull()?.uppercase() ?: "P",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Camera Icon Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BuzzPrimary)
                            .border(2.dp, BuzzCard, CircleShape)
                            .clickable { onEditPhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display Name with Edit Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onEditName,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit name",
                            modifier = Modifier.size(16.dp),
                            tint = BuzzPrimary
                        )
                    }
                }
                
                if (!isGuest && email != null) {
                    Text(
                        email,
                        style = MaterialTheme.typography.bodySmall,
                        color = BuzzTextMuted
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Level Badge
                val levelBgBrush = if (isGuest) {
                    Brush.linearGradient(listOf(BuzzTextMuted.copy(alpha = 0.2f), BuzzTextMuted.copy(alpha = 0.2f)))
                } else {
                    Brush.linearGradient(listOf(BuzzPrimary.copy(alpha = 0.2f), BuzzAccent.copy(alpha = 0.2f)))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(levelBgBrush)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isGuest) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BuzzSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            if (isGuest) "Guest Mode" else "Level $level • $levelName",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isGuest) BuzzTextMuted else BuzzPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XPProgressCard(
    xp: Int,
    level: Int,
    levelName: String,
    progress: Float,
    nextLevelXp: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "xpProgress"
    )
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Experience Points",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$xp / $nextLevelXp XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = BuzzTextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BuzzAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "+${(nextLevelXp - xp)} to next",
                        style = MaterialTheme.typography.labelSmall,
                        color = BuzzAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = BuzzPrimary,
                trackColor = BuzzTextMuted.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Earn XP by posting & saving places",
                    style = MaterialTheme.typography.labelSmall,
                    color = BuzzTextMuted
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    postsCount: Int,
    savedCount: Int,
    visitsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            value = postsCount.toString(),
            label = "Posts",
            icon = Icons.Default.Edit,
            color = BuzzPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = savedCount.toString(),
            label = "Saved",
            icon = Icons.Default.Bookmark,
            color = BuzzSecondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = visitsCount.toString(),
            label = "Visits",
            icon = Icons.Default.Place,
            color = BuzzAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BuzzTextMuted
            )
        }
    }
}

@Composable
private fun AchievementsCard(postsCount: Int, savedCount: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = BuzzSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (postsCount >= 1) {
                    AchievementBadge("🎯", "First Post", Modifier.weight(1f))
                }
                if (savedCount >= 1) {
                    AchievementBadge("⭐", "Collector", Modifier.weight(1f))
                }
                if (postsCount >= 5) {
                    AchievementBadge("🔥", "Active", Modifier.weight(1f))
                }
                if (savedCount >= 10) {
                    AchievementBadge("💎", "Curator", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(emoji: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BuzzPrimary.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = BuzzPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GuestUpgradeCard(onSwitchToAuth: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BuzzPrimary.copy(alpha = 0.8f), BuzzAccent.copy(alpha = 0.8f))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Unlock Full Experience",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Create an account to post in community, save places, and earn XP!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSwitchToAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Create Account",
                        color = BuzzPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = BuzzPrimary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = BuzzTextMuted
            )
        }
    }
}
