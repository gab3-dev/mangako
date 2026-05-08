package com.gabedev.mangako.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private const val ShimmerTravelDistance = 1400f
private const val ShimmerGradientWidth = 900f

fun shimmerGradientColors(
    baseColor: Color,
    highlightColor: Color,
): List<Pair<Float, Color>> {
    return listOf(
        0f to baseColor,
        0.42f to baseColor,
        0.5f to highlightColor,
        0.58f to baseColor,
        1f to baseColor,
    )
}

fun shimmerGradientOffsets(progress: Float): Pair<Offset, Offset> {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val center = -ShimmerTravelDistance + (ShimmerTravelDistance * 2f * clampedProgress)

    return Offset(center - ShimmerGradientWidth / 2f, 0f) to
        Offset(center + ShimmerGradientWidth / 2f, 0f)
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    val baseColor = lerp(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.tertiaryContainer,
        0.45f,
    )
    val highlightColor = lerp(
        baseColor,
        MaterialTheme.colorScheme.tertiary,
        0.28f,
    )

    val colorStops = shimmerGradientColors(baseColor, highlightColor)
    val (startOffset, endOffset) = shimmerGradientOffsets(progress)

    return Brush.linearGradient(
        colorStops = colorStops.toTypedArray(),
        start = startOffset,
        end = endOffset,
    )
}

@Composable
fun SkeletonSearchItem(
    modifier: Modifier = Modifier,
    brush: Brush = shimmerBrush(),
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 128.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.23f)
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
fun SkeletonSearchList(count: Int = 5) {
    val brush = shimmerBrush()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(count) {
            SkeletonSearchItem(brush = brush)
        }
    }
}
