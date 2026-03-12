package com.gabedev.mangako.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun shimmerGradientColors(
    baseColor: Color,
    highlightColor: Color,
    progress: Float
): List<Pair<Float, Color>> {
    val offset = progress * 2f
    return listOf(
        (0f + offset).coerceIn(0f, 1f) to baseColor,
        (0.3f + offset).coerceIn(0f, 1f) to highlightColor,
        (0.6f + offset).coerceIn(0f, 1f) to baseColor,
    )
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    val colorStops = shimmerGradientColors(baseColor, highlightColor, progress)

    return Brush.linearGradient(
        colorStops = colorStops.toTypedArray(),
        start = Offset.Zero,
        end = Offset(1000f, 0f),
    )
}

@Composable
fun SkeletonSearchItem() {
    val brush = shimmerBrush()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.23f)
                    .heightIn(128.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush),
                )
                // Author placeholder
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush),
                )
                // Type badge placeholder
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(0.25f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush),
                )
            }
        }
    }
}

@Composable
fun SkeletonSearchList(count: Int = 3) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) {
            SkeletonSearchItem()
        }
    }
}
