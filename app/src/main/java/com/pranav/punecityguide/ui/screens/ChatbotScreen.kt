package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import com.pranav.punecityguide.ui.components.ErrorMessage
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.ui.components.LoadableImage
import com.pranav.punecityguide.ui.viewmodel.ChatMessage
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.ui.viewmodel.ChatbotViewModel
import com.pranav.punecityguide.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    database: PuneCityDatabase,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { com.pranav.punecityguide.data.service.SupabaseClient.getSessionManager() }
    val userId by sessionManager.userIdFlow.collectAsState(initial = "user_default")
    
    val repository = remember { AttractionRepository(database.attractionDao(), database.recentlyViewedDao()) }
    val auditRepository = remember { SyncAuditRepository(database.syncAuditDao()) }
    val tokenQuotaService = remember { com.pranav.punecityguide.data.service.AiTokenQuotaService(database.aiTokenQuotaDao()) }
    
    val viewModel: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.factory(repository, auditRepository, tokenQuotaService, userId ?: "user_default")
    )
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "AI Pune Assistant",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Powered by Claude AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Token indicator removed
                        // Share button
                        AnimatedVisibility(
                            visible = uiState.recommendations.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = {
                                    val lastUserMessage = uiState.messages.lastOrNull { it.isUser }?.text ?: "Recommendations"
                                    ShareHelper.shareChatRecommendations(context, lastUserMessage, uiState.recommendations)
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share Recommendations", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                // Token limit warning banner
                // Token limit banner removed
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { if (it.length <= 500) input = it },
                        placeholder = {
                            Text(
                                if (uiState.tokenLimitReached) "Daily limit reached — try tomorrow"
                                else "Ask about Pune plans...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !uiState.isSending && !uiState.tokenLimitReached,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        trailingIcon = if (input.isNotEmpty()) {
                            { Text("${input.length}/500", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(end = 8.dp)) }
                        } else null
                    )
                    FloatingActionButton(
                        onClick = {
                            if (input.trim().isNotEmpty() && !uiState.isSending && !uiState.tokenLimitReached) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (uiState.tokenLimitReached || uiState.isSending)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        val messageCount = uiState.messages.size + if (uiState.isSending) 1 else 0
        LaunchedEffect(messageCount) {
            if (messageCount > 0) listState.animateScrollToItem(messageCount - 1)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoadingContext) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text("Loading city intelligence...")
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.quickPrompts) { prompt ->
                            AssistChip(
                                onClick = { viewModel.sendMessage(prompt) },
                                label = { Text(prompt) }
                            )
                        }
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    AnimatedChatBubble(message = message)
                }

                if (uiState.isSending) {
                    item { TypingIndicator() }
                }

                if (!uiState.isSending && uiState.errorMessage != null) {
                    item {
                        Text(
                            text = "Live API unavailable. Please try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (uiState.recommendations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recommended for you",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(uiState.recommendations, key = { it.id }) { attraction ->
                                RecommendationCard(
                                    attraction = attraction,
                                    onClick = { onNavigateToDetail(attraction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedChatBubble(message: ChatMessage) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(300))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!message.isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth(if (message.isUser) 0.78f else 0.86f)
            ) {
                Box(
                    modifier = Modifier.background(
                        brush = if (message.isUser) {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                ) {
                    Text(
                        text = message.text,
                        color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0, 150, 300).forEach { delayMs ->
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, delayMillis = delayMs, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$delayMs"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset(y = offsetY.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    attraction: Attraction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .height(150.dp)
            .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LoadableImage(
                model = attraction.imageUrl,
                contentDescription = attraction.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                height = 92,
                contentScale = ContentScale.Crop,
                category = attraction.category
            )
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = attraction.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Text(
                text = "${attraction.category} - ${attraction.rating}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
