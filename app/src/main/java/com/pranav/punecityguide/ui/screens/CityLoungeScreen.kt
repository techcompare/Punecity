package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pranav.punecityguide.ui.viewmodel.AuthUiState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.model.GlobalChatMessage
import com.pranav.punecityguide.data.repository.PuneConnectRepository
import com.pranav.punecityguide.ui.viewmodel.LoungeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityLoungeScreen() {
    val repository = remember { PuneConnectRepository() }
    val viewModel: LoungeViewModel = viewModel(factory = LoungeViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: com.pranav.punecityguide.ui.viewmodel.AuthViewModel = viewModel(
        factory = com.pranav.punecityguide.ui.viewmodel.AuthViewModelFactory(context.applicationContext as android.app.Application)
    )
    val authState by authViewModel.uiState.collectAsState()
    val currentUserId = authState.userId ?: ""
    val currentUserName = authState.userEmail?.substringBefore("@") ?: "Punekar"

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var messageField by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4ADE80).copy(alpha = 0.15f),
                            modifier = Modifier.size(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4ADE80), CircleShape))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Pune Live Lounge", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Chat with everyone in the city", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Info, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = messageField,
                        onValueChange = { messageField = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp)),
                        placeholder = { Text("Say something to Pune...") },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        maxLines = 3,
                        enabled = !uiState.isSending
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageField.isNotBlank()) {
                                viewModel.sendMessage(messageField, currentUserName)
                                messageField = ""
                            }
                        },
                        containerColor = if (messageField.isBlank() || uiState.isSending) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (messageField.isBlank() || uiState.isSending) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    )
                )
        ) {
            if (uiState.messages.isEmpty() && uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.messages.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), // Padding for tab bar
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.messages) { msg ->
                        LoungeMessageBubble(msg, currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
fun LoungeMessageBubble(message: GlobalChatMessage, currentUserId: String) {
    val isMe = message.userId == currentUserId
    
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp, topEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }
    
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
    
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe) {
                Text(
                    text = message.userName ?: "Anonymous Punekar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }
            
            Surface(
                shape = bubbleShape,
                color = bgColor,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (message.createdAt ?: "").takeLast(8).take(5), // Simple time extract
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
