package com.pranav.punecityguide.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Pune Connect brand palette
private val SplashBg        = Color(0xFF0F0A1E)
private val SplashBgMid     = Color(0xFF1A0F35)
private val SplashIndigo    = Color(0xFF4338CA)
private val SplashIndigoLt  = Color(0xFF6366F1)
private val SplashRose      = Color(0xFFE11D48)
private val SplashTeal      = Color(0xFF0D9488)
private val SplashGold      = Color(0xFFF59E0B)
private val SplashWhite     = Color(0xFFFFFFFF)

@Composable
fun BrandSplashScreen(onFinished: () -> Unit) {
    // ── Animate-in state ──────────────────────────────────────────────────
    val exitAlpha   = remember { Animatable(1f) }
    val logoScale   = remember { Animatable(0f) }
    val logoAlpha   = remember { Animatable(0f) }
    val ring1Scale  = remember { Animatable(0f) }
    val ring2Scale  = remember { Animatable(0f) }
    val ring1Alpha  = remember { Animatable(0f) }
    val ring2Alpha  = remember { Animatable(0f) }
    val titleAlpha  = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(40f) }
    val tagAlpha    = remember { Animatable(0f) }
    val tagOffset   = remember { Animatable(20f) }
    val pillAlpha   = remember { Animatable(0f) }
    val dotAlpha    = remember { Animatable(0f) }

    // ── Infinite ──────────────────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "inf")
    val pulse by inf.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val orbRotation by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbRot"
    )
    val shimmer by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        // Logo burst-in with bounce
        launch { logoAlpha.animateTo(1f, tween(450)) }
        logoScale.animateTo(1.18f, tween(550, easing = FastOutSlowInEasing))
        logoScale.animateTo(0.95f, tween(150))
        logoScale.animateTo(1.02f, tween(100))
        logoScale.animateTo(1f, tween(80))

        // Rings expand outward
        launch {
            delay(100)
            ring1Alpha.animateTo(1f, tween(400))
            ring1Scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            delay(250)
            ring2Alpha.animateTo(0.55f, tween(500))
            ring2Scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }

        // Dots pulse in
        delay(400)
        dotAlpha.animateTo(1f, tween(300))

        // Title slides up
        delay(100)
        launch { titleOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
        titleAlpha.animateTo(1f, tween(450))

        // Tagline
        delay(150)
        launch { tagOffset.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
        tagAlpha.animateTo(1f, tween(400))

        // Pill badge
        delay(100)
        pillAlpha.animateTo(1f, tween(350))

        // Hold
        delay(1200)

        // Fade out
        exitAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(SplashBgMid, SplashBg),
                    center = Offset.Unspecified,
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Ambient orb particles ──────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize().alpha(dotAlpha.value)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radBase = Math.toRadians(orbRotation.toDouble())

            val orbs = listOf(
                Triple(SplashIndigo,    200f, 8f),
                Triple(SplashRose,      260f, 6f),
                Triple(SplashTeal,      310f, 5f),
                Triple(SplashGold,      170f, 7f),
                Triple(SplashIndigoLt,  350f, 4f),
                Triple(SplashRose,      140f, 3f),
                Triple(SplashTeal,      400f, 6f),
                Triple(SplashGold,      290f, 4.5f),
            )

            orbs.forEachIndexed { i, (color, dist, radius) ->
                val angle = radBase + i * (Math.PI * 2 / orbs.size)
                val x = cx + dist * cos(angle).toFloat()
                val y = cy + dist * sin(angle).toFloat()
                drawCircle(color = color.copy(alpha = 0.55f * pulse), radius = radius * pulse, center = Offset(x, y))
            }

            // Static sparkle dots in corners
            val sparks = listOf(
                Offset(cx - 280f, cy - 420f) to SplashGold,
                Offset(cx + 300f, cy - 360f) to SplashWhite,
                Offset(cx - 320f, cy + 280f) to SplashTeal,
                Offset(cx + 260f, cy + 380f) to SplashRose,
                Offset(cx - 120f, cy - 560f) to SplashIndigoLt,
                Offset(cx + 180f, cy - 500f) to SplashGold,
                Offset(cx - 400f, cy - 140f) to SplashWhite,
                Offset(cx + 380f, cy + 100f) to SplashTeal,
            )
            sparks.forEach { (pos, color) ->
                drawCircle(color = color.copy(alpha = 0.6f * pulse), radius = 3.5f, center = pos)
            }
        }

        // ── Connection rings behind logo ───────────────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(ring2Scale.value)
                .alpha(ring2Alpha.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = SplashIndigoLt.copy(alpha = 0.25f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(ring1Scale.value)
                .alpha(ring1Alpha.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = SplashIndigo.copy(alpha = 0.45f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // ── Main content column ────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo mark ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Glow behind logo
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SplashIndigo.copy(alpha = 0.45f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2f
                    )
                }

                // Main logo surface
                Surface(
                    shape = CircleShape,
                    color = SplashWhite,
                    modifier = Modifier.size(112.dp),
                    shadowElevation = 24.dp,
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // "PC" monogram in brand indigo
                        Text(
                            text = "PC",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = SplashIndigo,
                            letterSpacing = (-1.5).sp
                        )
                    }
                }

                // Gold AI sparkle badge (top-right)
                Surface(
                    shape = CircleShape,
                    color = SplashGold,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 40.dp, y = (-40).dp)
                        .scale(pulse),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SplashBg
                        )
                    }
                }

                // Location pin badge (below)
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .offset(y = 62.dp)
                        .alpha(logoAlpha.value)
                        .scale(pulse),
                    tint = SplashGold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── App Name ──────────────────────────────────────────────────
            Text(
                text = "Pune Connect",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SplashWhite,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffset.value.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Tagline ───────────────────────────────────────────────────
            Text(
                text = "Your Hyperlocal Pune Community",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = SplashWhite.copy(alpha = 0.60f),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(tagAlpha.value)
                    .offset(y = tagOffset.value.dp)
                    .padding(horizontal = 48.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── AI-Powered pill badge ────────────────────────────────────
            Surface(
                color = SplashIndigo.copy(alpha = 0.30f),
                shape = CircleShape,
                modifier = Modifier
                    .alpha(pillAlpha.value)
                    .scale(pulse)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = SplashGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "AI-POWERED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SplashWhite.copy(alpha = 0.85f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // ── Bottom city/version tag ────────────────────────────────────────
        Text(
            text = "Pune, India",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SplashWhite.copy(alpha = 0.30f),
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(pillAlpha.value)
        )
    }
}
