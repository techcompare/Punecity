package com.pranav.punecityguide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Optimized for CostPilot brand identity.
 */
@Composable
fun LoadableImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    height: Int = 220,
    category: String = "",
) {
    val context = LocalContext.current
    val displayUrl = if (model != null && model.startsWith("http")) model else ""

    val palette = categoryPalette(category, contentDescription ?: "")

    var imageLoaded by remember(displayUrl) { mutableStateOf(false) }
    var imageFailed by remember(displayUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(palette.startColor, palette.endColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            )
        }

        // Layer 2: Async Image
        if (displayUrl.isNotBlank() && !imageFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    imageLoaded = state is AsyncImagePainter.State.Success
                    imageFailed = state is AsyncImagePainter.State.Error
                }
            )
        }

        // Layer 3: Shimmer
        if (!imageLoaded && displayUrl.isNotBlank() && !imageFailed) {
            ShimmerBackground(modifier = Modifier.fillMaxSize())
        }

        // Layer 4: Readability Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                    )
                )
        )
    }
}

data class CategoryPalette(
    val startColor: Color,
    val endColor: Color,
    val icon: ImageVector
)

@Composable
private fun categoryPalette(category: String, name: String): CategoryPalette {
    val cat = category.lowercase()
    return when {
        "food" in cat || "expense" in cat -> CategoryPalette(
            Color(0xFFE65100), Color(0xFFFF851B), Icons.Filled.Fastfood
        )
        "travel" in cat || "transport" in cat -> CategoryPalette(
            Color(0xFF1565C0), Color(0xFF0074D9), Icons.Filled.LocalTaxi
        )
        "saving" in cat || "budget" in cat -> CategoryPalette(
            Color(0xFF2E7D32), Color(0xFF2ECC40), Icons.Filled.Savings
        )
        else -> {
            val hash = (name.hashCode() and 0x7FFFFFFF)
            val hue = (hash % 360).toFloat()
            CategoryPalette(
                startColor = Color.hsl(hue, 0.5f, 0.3f),
                endColor = Color.hsl((hue + 40f) % 360f, 0.4f, 0.5f),
                icon = Icons.Filled.Place
            )
        }
    }
}
