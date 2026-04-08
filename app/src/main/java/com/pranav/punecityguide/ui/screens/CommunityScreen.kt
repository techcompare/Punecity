package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.model.CommunityPost
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted
import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    currentUserName: String,
    canPost: Boolean,
    isLoading: Boolean,
    isPosting: Boolean,
    posts: List<CommunityPost>,
    error: String?,
    info: String?,
    onRefresh: () -> Unit,
    onCreatePost: (String) -> Unit,
    onUpdatePost: (String, String) -> Unit = { _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onOpenAuth: () -> Unit,
    onLikePost: (String) -> Unit = {},
    onSavePost: (String) -> Unit = {},
    onSharePost: (CommunityPost) -> Unit = {},
) {
    var postText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var postEpoch by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    // Minimum 8 characters, maximum 500
    val textLength = postText.trim().length
    val canSubmit = textLength >= 8 && textLength <= 500 && !isPosting && canPost

    val puneLocations = listOf(
        "Koregaon Park", "FC Road", "JM Road", "Shivajinagar",
        "Deccan", "Kothrud", "Hinjewadi", "Viman Nagar",
        "Aundh", "Baner", "Magarpatta", "Hadapsar"
    )

    // Clear text on successful post
    LaunchedEffect(info, postEpoch) {
        if (info == "Posted successfully") {
            showSuccessAnimation = true
            delay(2000)
            showSuccessAnimation = false
            postText = ""
            selectedLocation = null
            postEpoch++
        }
    }
    
    // Handle pull to refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            delay(1000)
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Community",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Share your Pune experiences",
                        color = BuzzTextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BuzzCard)
                        .clickable(enabled = !isLoading) { onRefresh() }
                        .padding(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = BuzzPrimary,
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh posts",
                            modifier = Modifier.size(18.dp),
                            tint = BuzzPrimary
                        )
                    }
                }
            }
        }

        // Post Composer Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuzzCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // User Info Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canPost) 
                                        Brush.linearGradient(listOf(BuzzPrimary, BuzzAccent))
                                    else 
                                        Brush.linearGradient(listOf(BuzzTextMuted, BuzzTextMuted))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUserName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                currentUserName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (canPost) "Ready to share" else "Guest - Sign in to post",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canPost) Color(0xFF4ADE80) else BuzzTextMuted
                            )
                        }
                    }

                    // Text Input
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { 
                            if (it.length <= 500) {
                                postText = it
                            }
                        },
                        placeholder = { 
                            Text(
                                if (canPost) "What would you like to share about Pune?" 
                                else "Sign in to share your thoughts...",
                                color = BuzzTextMuted
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        enabled = canPost && !isPosting,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuzzPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            disabledContainerColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )

                    // Location & Character Count Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Picker
                        Box {
                            TextButton(
                                onClick = { showLocationPicker = true },
                                enabled = canPost && !isPosting,
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedLocation != null) BuzzPrimary else BuzzTextMuted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    selectedLocation ?: "Add location",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedLocation != null) BuzzPrimary else BuzzTextMuted
                                )
                                if (selectedLocation != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear location",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { selectedLocation = null },
                                        tint = BuzzTextMuted
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showLocationPicker,
                                onDismissRequest = { showLocationPicker = false }
                            ) {
                                puneLocations.forEach { location ->
                                    DropdownMenuItem(
                                        text = { Text(location) },
                                        onClick = {
                                            selectedLocation = location
                                            showLocationPicker = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = BuzzPrimary
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Character count
                        Text(
                            "$textLength/500",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                textLength > 450 -> BuzzSecondary
                                textLength < 8 && textLength > 0 -> BuzzTextMuted
                                else -> BuzzTextMuted
                            }
                        )
                        
                        if (textLength in 1..7) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(min 8)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BuzzTextMuted
                            )
                        }
                    }
                    
                    // Success/Error Messages
                    AnimatedVisibility(
                        visible = info != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4ADE80).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                info ?: "",
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = error != null && !isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = BuzzSecondary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                error ?: "",
                                color = BuzzSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Post Button
                    if (canPost) {
                        Button(
                            onClick = { 
                                val finalContent = if (selectedLocation != null) {
                                    "${postText.trim()} 📍 $selectedLocation"
                                } else {
                                    postText.trim()
                                }
                                onCreatePost(finalContent)
                            },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BuzzPrimary,
                                disabledContainerColor = BuzzPrimary.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Posting...")
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share with Community", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = onOpenAuth,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BuzzAccent)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In to Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Loading State
        if (isLoading && posts.isEmpty()) {
            item { LoadingIndicator() }
        }
        
        // Empty State
        if (!isLoading && posts.isEmpty() && error == null) {
            item { EmptyPosts() }
        }
        
        // Posts List
        if (posts.isNotEmpty()) {
            item {
                Text(
                    "Recent Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(posts, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    currentUserName = currentUserName,
                    canEdit = canPost && post.author == currentUserName,
                    onLike = { onLikePost(post.id) },
                    onSave = { onSavePost(post.id) },
                    onShare = { onSharePost(post) },
                    onEdit = { newContent -> onUpdatePost(post.id, newContent) },
                    onDelete = { onDeletePost(post.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

        // Success Animation Overlay
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BuzzCard),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Posted Successfully!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your post is now live",
                        style = MaterialTheme.typography.bodySmall,
                        color = BuzzTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun PostItem(
    post: CommunityPost,
    currentUserName: String,
    canEdit: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var isSaved by remember { mutableStateOf(post.isSaved) }
    var likeCount by remember { mutableIntStateOf(post.likes) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(post.content) }
    
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "likeScale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE91E63) else BuzzTextMuted,
        label = "likeColor"
    )
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Author Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BuzzAccent.copy(alpha = 0.4f), BuzzPrimary.copy(alpha = 0.4f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.author.firstOrNull()?.uppercase() ?: "P",
                        fontWeight = FontWeight.Black,
                        color = BuzzPrimary,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        relativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = BuzzTextMuted
                    )
                }
                
                // Edit/Delete Menu (only for own posts)
                if (canEdit) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = BuzzTextMuted
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    editText = post.content
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = BuzzSecondary)
                                }
                            )
                        }
                    }
                }
            }
            
            // Content
            Text(
                post.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
            
            // Location Tag (if present)
            if (!post.location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BuzzPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BuzzPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        post.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = BuzzPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                            onLike()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(20.dp).scale(likeScale),
                        tint = likeColor
                    )
                    if (likeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            likeCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = likeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Save Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            isSaved = !isSaved
                            onSave()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSaved) BuzzSecondary else BuzzTextMuted
                    )
                }
                
                // Share Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShare() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp),
                        tint = BuzzTextMuted
                    )
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Post") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { if (it.length <= 500) editText = it },
                    placeholder = { Text("Edit your post...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${editText.length}/500") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editText.trim().length >= 8) {
                            onEdit(editText.trim())
                            showEditDialog = false
                        }
                    },
                    enabled = editText.trim().length >= 8
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = BuzzSecondary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Post?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete this post?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BuzzSecondary)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = BuzzPrimary,
                strokeCap = StrokeCap.Round
            )
            Text(
                "Loading community posts...",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyPosts() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BuzzCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuzzPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = BuzzPrimary
                )
            }
            Text(
                "No posts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Be the first to share something in Pune!",
                color = BuzzTextMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun relativeTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "Just now"

    val candidates = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    val parsed = candidates.firstNotNullOfOrNull { pattern ->
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(raw)
        }.getOrNull()
    } ?: return raw.take(16)

    val diffMillis = (System.currentTimeMillis() - parsed.time).coerceAtLeast(0)
    val minutes = diffMillis / 60_000
    val hours = diffMillis / 3_600_000
    val days = diffMillis / 86_400_000

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 7 -> "$days day ago"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(parsed.time))
    }
}
