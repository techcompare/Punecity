package com.pranav.punecityguide.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Premium gradient-based visual card.
 *
 * Strategy: Always show a beautiful gradient with category icon.
 * If a valid image URL is available, overlay it on top.
 * This ensures the card NEVER looks broken — the gradient
 * is the "floor" and images are a bonus.
 */
@Composable
fun LoadableImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    height: Int = 250,
    errorBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    category: String = "",
) {
    val context = LocalContext.current
    val displayUrl = when {
        model.isNullOrBlank() -> ""
        model.startsWith("http") -> model
        else -> "${com.pranav.punecityguide.AppConfig.Supabase.COMMUNITY_SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/posts/$model"
    }

    val palette = categoryPalette(category, contentDescription ?: "")

    var imageLoaded by remember(displayUrl) { mutableStateOf(false) }
    var retryKey by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Always-visible gradient background
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
            // Category icon with subtle transparency
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(80.dp)
            )
        }

        // Layer 2: Try to load the actual image on top
        if (displayUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(displayUrl)
                    .crossfade(true)
                    .memoryCacheKey("${displayUrl}_$retryKey")
                    .diskCacheKey("${displayUrl}_$retryKey")
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    imageLoaded = state is AsyncImagePainter.State.Success
                }
            )
        }

        // Layer 3: Shimmer overlay while loading
        if (!imageLoaded && displayUrl.isNotBlank()) {
            ShimmerBackground(modifier = Modifier.fillMaxSize())
        }

        // Layer 4: Bottom gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((height / 3).dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )
    }
}

// ── Category Visual System ──

data class CategoryPalette(
    val startColor: Color,
    val endColor: Color,
    val icon: ImageVector
)

@Composable
private fun categoryPalette(category: String, name: String): CategoryPalette {
    val cat = category.lowercase()
    return when {
        "histori" in cat -> CategoryPalette(
            Color(0xFF8B5E3C), Color(0xFFD4A574), Icons.Filled.Castle
        )
        "religi" in cat || "temple" in cat -> CategoryPalette(
            Color(0xFFFF8F00), Color(0xFFFFCA28), Icons.Filled.Church
        )
        "adventure" in cat || "trek" in cat || "fort" in cat -> CategoryPalette(
            Color(0xFF2E7D32), Color(0xFF66BB6A), Icons.Filled.Hiking
        )
        "nature" in cat || "park" in cat || "garden" in cat -> CategoryPalette(
            Color(0xFF1B5E20), Color(0xFF43A047), Icons.Filled.Forest
        )
        "food" in cat || "street food" in cat -> CategoryPalette(
            Color(0xFFE65100), Color(0xFFFF8A65), Icons.Filled.Fastfood
        )
        "cultural" in cat || "museum" in cat -> CategoryPalette(
            Color(0xFF4A148C), Color(0xFFAB47BC), Icons.Filled.Museum
        )
        "modern" in cat || "mall" in cat -> CategoryPalette(
            Color(0xFF1565C0), Color(0xFF42A5F5), Icons.Filled.ShoppingBag
        )
        "nightlife" in cat || "bar" in cat || "pub" in cat -> CategoryPalette(
            Color(0xFF311B92), Color(0xFF7C4DFF), Icons.Filled.NightlightRound
        )
        "study" in cat || "library" in cat -> CategoryPalette(
            Color(0xFF004D40), Color(0xFF26A69A), Icons.Filled.MenuBook
        )
        "hidden" in cat || "view" in cat || "scenic" in cat -> CategoryPalette(
            Color(0xFF0D47A1), Color(0xFF29B6F6), Icons.Filled.Visibility
        )
        "cafe" in cat || "coffee" in cat -> CategoryPalette(
            Color(0xFF5D4037), Color(0xFFA1887F), Icons.Filled.LocalCafe
        )
        "spa" in cat || "wellness" in cat -> CategoryPalette(
            Color(0xFF00695C), Color(0xFF80CBC4), Icons.Filled.Spa
        )
        else -> {
            // Generate deterministic colors from the name hash
            val hash = (name.hashCode() and 0x7FFFFFFF)
            val hue = (hash % 360).toFloat()
            CategoryPalette(
                startColor = Color.hsl(hue, 0.6f, 0.35f),
                endColor = Color.hsl((hue + 30f) % 360f, 0.5f, 0.55f),
                icon = Icons.Filled.Place
            )
        }
    }
}
