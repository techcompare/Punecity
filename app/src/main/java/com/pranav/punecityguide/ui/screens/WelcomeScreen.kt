package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.punecityguide.ui.theme.BuzzAccent
import com.pranav.punecityguide.ui.theme.BuzzBackgroundEnd
import com.pranav.punecityguide.ui.theme.BuzzBackgroundStart
import com.pranav.punecityguide.ui.theme.BuzzCard
import com.pranav.punecityguide.ui.theme.BuzzPrimary
import com.pranav.punecityguide.ui.theme.BuzzSecondary
import com.pranav.punecityguide.ui.theme.BuzzTextMuted
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var showLogo by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    var currentFeature by remember { mutableIntStateOf(0) }
    
    // Staggered animations
    LaunchedEffect(Unit) {
        delay(200)
        showLogo = true
        delay(400)
        showTagline = true
        delay(300)
        showFeatures = true
        delay(400)
        showButton = true
    }
    
    // Auto-cycle features
    LaunchedEffect(showFeatures) {
        if (showFeatures) {
            while (true) {
                delay(3000)
                currentFeature = (currentFeature + 1) % 3
            }
        }
    }
    
    // Floating animation for decorative elements
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BuzzBackgroundStart, BuzzBackgroundEnd))),
    ) {
        // Animated background decorations
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = 100.dp + floatOffset.dp)
                .alpha(0.1f)
                .background(
                    Brush.radialGradient(listOf(BuzzPrimary, Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = 200.dp - floatOffset.dp)
                .alpha(0.08f)
                .background(
                    Brush.radialGradient(listOf(BuzzAccent, Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 30.dp, y = floatOffset.dp * 2)
                .alpha(0.06f)
                .background(
                    Brush.radialGradient(listOf(BuzzSecondary, Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Logo
                AnimatedVisibility(
                    visible = showLogo,
                    enter = fadeIn(tween(600)) + slideInVertically(
                        initialOffsetY = { -100 },
                        animationSpec = spring(dampingRatio = 0.6f)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Logo icon
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(BuzzPrimary, BuzzAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            "pune Buzz",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = BuzzPrimary,
                        )
                        Text(
                            "Your Pune, Reimagined",
                            style = MaterialTheme.typography.titleMedium,
                            color = BuzzTextMuted,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Animated tagline
                AnimatedVisibility(
                    visible = showTagline,
                    enter = fadeIn(tween(800)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = spring(dampingRatio = 0.7f)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Discover Places.\nJoin the Community.\nExplore Pune.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(BuzzPrimary, BuzzAccent))
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Animated Feature Cards
                AnimatedVisibility(
                    visible = showFeatures,
                    enter = fadeIn(tween(600)) + slideInVertically(
                        initialOffsetY = { 80 },
                        animationSpec = spring(dampingRatio = 0.65f)
                    )
                ) {
                    val features = listOf(
                        Triple(Icons.Default.Explore, "Live Discovery", "Real-time updates on cafes, forts, and hidden spots"),
                        Triple(Icons.Default.Groups, "Community", "See what Punekars are talking about today"),
                        Triple(Icons.Default.Map, "Curated Plans", "Ready-to-follow itineraries for every mood")
                    )
                    
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BuzzCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            features.forEachIndexed { index, (icon, title, desc) ->
                                val isActive = index == currentFeature
                                val scale by animateFloatAsState(
                                    targetValue = if (isActive) 1.02f else 1f,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "featureScale"
                                )
                                val alpha by animateFloatAsState(
                                    targetValue = if (isActive) 1f else 0.6f,
                                    label = "featureAlpha"
                                )
                                
                                FeatureRow(
                                    icon = icon,
                                    title = title,
                                    description = desc,
                                    isActive = isActive,
                                    modifier = Modifier
                                        .scale(scale)
                                        .alpha(alpha)
                                )
                                
                                if (index < features.size - 1) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Animated CTA Button
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(dampingRatio = 0.5f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BuzzPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Start Exploring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == 1) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == 1) BuzzPrimary else BuzzTextMuted.copy(alpha = 0.3f)
                                    )
                            )
                            if (index < 2) Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    
                    Text(
                        "Made with ❤️ for Punekars",
                        color = BuzzTextMuted.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isActive) 
                        Brush.linearGradient(listOf(BuzzPrimary.copy(alpha = 0.2f), BuzzAccent.copy(alpha = 0.2f)))
                    else 
                        Brush.linearGradient(listOf(BuzzCard, BuzzCard))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) BuzzPrimary else BuzzTextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else BuzzTextMuted
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = BuzzTextMuted.copy(alpha = if (isActive) 0.8f else 0.5f)
            )
        }
    }
}
