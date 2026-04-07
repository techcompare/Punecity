package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.punecityguide.data.model.*
import com.pranav.punecityguide.ui.theme.*
import com.pranav.punecityguide.ui.viewmodel.CommunityViewModel
import com.pranav.punecityguide.ui.viewmodel.UserIdentity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen() {
    val viewModel: CommunityViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentInput by remember { mutableStateOf("") }

    // Auto-scroll to latest message on update
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CommunityTopBar(
                activeChannel = uiState.activeChannel,
                channels = uiState.channels,
                onSelectChannel = { viewModel.selectChannel(it) }
            )
        },
        bottomBar = {
            // "users cant msg" issue resolved via identity system (even guests get IDs)
            ChatInputBar(
                input = currentInput,
                onInputChange = { currentInput = it },
                onSend = {
                    viewModel.sendMessage(currentInput)
                    currentInput = ""
                },
                isSending = uiState.isSending,
                isEnabled = uiState.currentUserIdentity != null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CostPilotCyan)
            } else if (uiState.messages.isEmpty()) {
                EmptyChannelState(uiState.activeChannel)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        val isMine = message.userId == uiState.currentUserIdentity?.id
                        CommunityChatBubble(
                            message = message,
                            isMine = isMine,
                            onReact = { viewModel.reactToMessage(message.id ?: "", "👍") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityTopBar(activeChannel: CommunityChannel?, channels: List<CommunityChannel>, onSelectChannel: (CommunityChannel) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "COMMUNITY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = CostPilotCyan
            )
            Text(
                activeChannel?.name ?: "Discussions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(channels) { channel ->
                val isSelected = activeChannel?.id == channel.id
                Surface(
                    onClick = { onSelectChannel(channel) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CostPilotCyan else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            channel.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityChatBubble(message: CommunityMessage, isMine: Boolean, onReact: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isMine) {
                Surface(shape = CircleShape, modifier = Modifier.size(32.dp), color = CostPilotCyan.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(message.userName.take(1), fontWeight = FontWeight.Bold, color = CostPilotCyan)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (!isMine) {
                    Text(
                        text = message.userName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isMine) 20.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 20.dp
                    ),
                    color = if (isMine) CostPilotCyan else MaterialTheme.colorScheme.surface,
                    border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMine) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Reactions (Social proof)
        Row(
            modifier = Modifier.padding(top = 4.dp, start = if (isMine) 0.dp else 40.dp, end = if (isMine) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onReact,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.height(24.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👍", fontSize = 10.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = (message.reactions.values.sum()).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChannelState(channel: CommunityChannel?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Forum, null, modifier = Modifier.size(64.dp), tint = CostPilotCyan.copy(alpha = 0.2f))
        Spacer(Modifier.height(24.dp))
        Text(
            "Welcome to ${channel?.name ?: "Community"}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            channel?.description ?: "Be the first to say something!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit, isSending: Boolean, isEnabled: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        shadowElevation = 16.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Share tips or questions...") },
                enabled = !isSending,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CostPilotCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                maxLines = 4
            )

            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isSending,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (input.isNotBlank()) CostPilotCyan else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (input.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
