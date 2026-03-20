package com.pranav.punecityguide.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.BuildConfig
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.service.LocalAiGuideService
import com.pranav.punecityguide.data.service.RealtimeChatService
import com.pranav.punecityguide.data.service.SupabaseClient
import com.pranav.punecityguide.ui.viewmodel.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.ui.viewmodel.ChatbotViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext

@Composable
fun AiChatOverlay(
    database: PuneCityDatabase,
    onClose: () -> Unit,
    initialMessage: String = "",
    modifier: Modifier = Modifier
) {
    val sessionManager = remember { com.pranav.punecityguide.data.service.SupabaseClient.getSessionManager() }
    val userId by sessionManager.userIdFlow.collectAsState(initial = "user_default") 
    
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val tokenQuotaService = remember { com.pranav.punecityguide.data.service.AiTokenQuotaService(database.aiTokenQuotaDao()) }
    
    val activity = LocalContext.current as ComponentActivity
    val viewModel: ChatbotViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ChatbotViewModel.factory(repository, auditRepository, tokenQuotaService, userId ?: "user_default")
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // Auto-send the initial message from HomeScreen prompts
    LaunchedEffect(initialMessage) {
        if (initialMessage.isNotBlank() && !uiState.isLoadingContext) {
            viewModel.sendMessage(initialMessage)
        }
    }
    // If context was loading when we first tried, retry when it finishes
    LaunchedEffect(uiState.isLoadingContext) {
        if (!uiState.isLoadingContext && initialMessage.isNotBlank() && uiState.messages.size <= 1) {
            viewModel.sendMessage(initialMessage)
        }
    }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Premium Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Pune Connect AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF4ADE80), CircleShape))
                                Text("Online • Savvy Assistant", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // --- Chat Area ---
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatBubble(message)
                }
                
                if (uiState.isSending) {
                    item {
                        ThinkingIndicator()
                    }
                }

                if (uiState.errorMessage != null) {
                    item {
                        ErrorState(message = uiState.errorMessage!!, onRetry = { viewModel.sendMessage(input) })
                    }
                }
            }

            // --- Input Area ---
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about any spot in Pune...", style = MaterialTheme.typography.bodyMedium) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    
                    IconButton(
                        onClick = {
                            val prompt = input.trim()
                            if (prompt.isEmpty() || uiState.isSending) return@IconButton
                            viewModel.sendMessage(prompt) {
                                input = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(830),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.size(8.dp)
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        }
        Text(
            "Pune AI is weaving a response...",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val isBudgetError = message.contains("402", ignoreCase = true) || message.contains("credits", ignoreCase = true)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBudgetError) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) 
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = if (isBudgetError) Icons.Filled.Info else Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = if (isBudgetError) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (isBudgetError) "OpenRouter Credit Limit Reached" else "Connection Interrupted",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isBudgetError) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = if (isBudgetError) 
                  "The AI budget for this request was exceeded (1282 token limit). Please try a shorter question or check your OpenRouter credits."
                  else message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBudgetError) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}