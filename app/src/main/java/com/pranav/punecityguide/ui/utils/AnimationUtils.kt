package com.pranav.punecityguide.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AnimatedContent(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
        exit = fadeOut() + slideOutVertically()
    ) {
        content()
    }
}

/**
 * Create a staggered animation for list items
 */
@Composable
fun rememberAnimatedContentAlpha(delayMillis: Int = 0): Float {
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    return alpha.value
}

/**
 * Create a scale animation for cards
 */
@Composable
fun rememberAnimatedContentScale(delayMillis: Int = 0): Float {
    val scale = remember { Animatable(0.95f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    return scale.value
}

/**
 * Create a Y translation animation
 */
@Composable
fun rememberAnimatedContentY(delayMillis: Int = 0): Float {
    val translateY = remember { Animatable(20f) }
    
    LaunchedEffect(Unit) {
        translateY.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    return translateY.value
}
