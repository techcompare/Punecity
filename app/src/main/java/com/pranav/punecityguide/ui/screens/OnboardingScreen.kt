package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.pranav.punecityguide.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundRes: Int? = null
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Explore Pune\nLike a Local",
            description = "Discover the best attractions, historical sites, and cultural landmarks curated just for you.",
            icon = Icons.Filled.Explore,
            color = Color(0xFFE91E63) // Pink/Magenta for culture
        ),
        OnboardingPage(
            title = "Plan Your Day\nEasily",
            description = "Find places near you, check entry fees, and create your perfect itinerary in minutes.",
            icon = Icons.Filled.Map,
            color = Color(0xFF6C63FF) // Purple for AI/Tech
        ),
        OnboardingPage(
            title = "Discover Hidden\nGems",
            description = "From secret study spots to the best street food, find the places that make Pune unique.",
            icon = Icons.Filled.LocationOn,
            color = Color(0xFFFF9800) // Orange for activity/social
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated Background Gradient
        val currentPageColor = pages[pagerState.currentPage].color
        val animatedColor by animateColorAsState(
            targetValue = currentPageColor,
            animationSpec = tween(1000),
            label = "color"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            animatedColor.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Floating blobs for decoration
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-50).dp)
                .size(200.dp)
                .alpha(0.1f)
                .background(animatedColor, CircleShape)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .size(250.dp)
                .alpha(0.1f)
                .background(animatedColor, CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // Skip button (Always visible to allow exit at any point)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished) {
                    Text(
                        if (pagerState.currentPage < pages.size - 1) "Skip" else "Finish",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                OnboardingPageView(pages[index], isCurrent = pagerState.currentPage == index)
            }

            // Bottom Control Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 10.dp,
                            label = "width",
                            animationSpec = spring(stiffness = Spring.StiffnessLow)
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) pages[index].color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            label = "color"
                        )

                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(width)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                }

                // Next / Finish Button
                val isLastPage = pagerState.currentPage == pages.size - 1
                val buttonColor = pages[pagerState.currentPage].color
                
                Button(
                    onClick = {
                        if (!isLastPage) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        label = "buttonText"
                    ) { last ->
                        if (last) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Get Started",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageView(page: OnboardingPage, isCurrent: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.5f,
        animationSpec = tween(500),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = animatedAlpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Circle with ripple effect
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(page.color.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(page.color.copy(alpha = 0.2f))
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = page.color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
