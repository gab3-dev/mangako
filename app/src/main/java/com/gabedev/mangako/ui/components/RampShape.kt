package com.gabedev.mangako.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun RampRectangle(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    rampWidth: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RampRectangleShape(rampWidth))
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text (
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview
@Composable
fun RampRectanglePreview() {
    RampRectangle(
        text = "MY EXMAPLE MANAGA",
        icon = androidx.compose.material.icons.Icons.Default.Check,
        modifier = Modifier.padding(16.dp),
        backgroundColor = MaterialTheme.colorScheme.primary,
        rampWidth = 32.dp
    )
}

class RampRectangleShape(
    private val rampWidth: Dp,
    private val cornerRadius: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rampWidthPx = with(density) { rampWidth.toPx() }
        val radiusPx = with(density) { cornerRadius.toPx() }

        val path = Path().apply {
            // Start from the ramp top point
            moveTo(rampWidthPx, 0f)

            // Top edge with rounded top-right corner
            lineTo(size.width - radiusPx, 0f)
            quadraticTo(size.width, 0f, size.width, radiusPx)

            // Right edge with rounded bottom-right corner
            lineTo(size.width, size.height - radiusPx)
            quadraticTo(size.width, size.height, size.width - radiusPx, size.height)

            // Bottom edge with rounded bottom-left corner
            lineTo(radiusPx, size.height)
            quadraticTo(0f, size.height, 0f, size.height - radiusPx)

            // Left edge up to where ramp begins
            lineTo(0f, size.height - radiusPx)

            lineTo(rampWidthPx - radiusPx*1.4f, radiusPx)
            quadraticTo(rampWidthPx-radiusPx, 0f, rampWidthPx, 0f)

            // Straight diagonal ramp line (no curve)
            // lineTo(rampWidthPx, 0f)

            close()

//            // Start from top-left with ramp (accounting for corner radius)
//            moveTo(rampWidthPx, 0.0F)
//
//            // Top edge with rounded top-right corner
//            lineTo(size.width - radiusPx, 0f)
//            quadraticTo(size.width, 0f, size.width, radiusPx)
//
//            // Right edge with rounded bottom-right corner
//            lineTo(size.width, size.height - radiusPx)
//            quadraticTo(size.width, size.height, size.width - radiusPx, size.height)
//
//            // Bottom edge with rounded bottom-left corner
//            lineTo(radiusPx, size.height)
//            quadraticTo(0f, size.height, 0f, size.height - radiusPx)
//
//            // Left edge and ramp with rounded corners
//            lineTo(0f, size.height-10 - radiusPx+10)
//
//            // Create the ramp with smooth curve
//            val controlPointX = rampWidthPx * 1.4f
//            val controlPointY = size.height * -0.8f
//            quadraticTo(controlPointX, controlPointY, rampWidthPx+radiusPx, 0.0F)
//
//            close()
        }

        return Outline.Generic(path)
    }
}
