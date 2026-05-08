package com.gabedev.mangako.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkeletonSearchItemTest {

    private val baseColor = Color.Gray
    private val highlightColor = Color.White

    @Test
    fun `shimmerGradientColors returns stable base-highlight-base band`() {
        val colors = shimmerGradientColors(baseColor, highlightColor)

        assertEquals(5, colors.size)
        assertEquals(baseColor, colors[0].second)
        assertEquals(baseColor, colors[1].second)
        assertEquals(highlightColor, colors[2].second)
        assertEquals(baseColor, colors[3].second)
        assertEquals(baseColor, colors[4].second)
        assertEquals(0f, colors[0].first, 0.01f)
        assertEquals(0.42f, colors[1].first, 0.01f)
        assertEquals(0.5f, colors[2].first, 0.01f)
        assertEquals(0.58f, colors[3].first, 0.01f)
        assertEquals(1f, colors[4].first, 0.01f)
    }

    @Test
    fun `shimmerGradientColors offsets are monotonically non-decreasing`() {
        val colors = shimmerGradientColors(baseColor, highlightColor)

        for (i in 0 until colors.size - 1) {
            assertTrue(
                "Offset ${colors[i].first} > ${colors[i + 1].first}",
                colors[i].first <= colors[i + 1].first,
            )
        }
    }

    @Test
    fun `shimmerGradientOffsets at progress 0 starts outside the left edge`() {
        val (start, end) = shimmerGradientOffsets(0f)

        assertTrue(start.x < 0f)
        assertTrue(end.x < 0f)
    }

    @Test
    fun `shimmerGradientOffsets at progress 0_5 centers the band`() {
        val (start, end) = shimmerGradientOffsets(0.5f)

        assertEquals(-450f, start.x, 0.01f)
        assertEquals(450f, end.x, 0.01f)
    }

    @Test
    fun `shimmerGradientOffsets at progress 1 ends outside the right edge`() {
        val (start, end) = shimmerGradientOffsets(1f)

        assertTrue(start.x > 0f)
        assertTrue(end.x > 0f)
    }

    @Test
    fun `shimmerGradientOffsets clamps progress into range`() {
        assertEquals(shimmerGradientOffsets(0f), shimmerGradientOffsets(-1f))
        assertEquals(shimmerGradientOffsets(1f), shimmerGradientOffsets(2f))
    }
}
