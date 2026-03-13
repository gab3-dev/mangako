package com.gabedev.mangako.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SkeletonSearchItemTest {

    private val baseColor = Color.Gray
    private val highlightColor = Color.White

    @Test
    fun `shimmerGradientColors at progress 0 starts with base color`() {
        val colors = shimmerGradientColors(baseColor, highlightColor, 0f)

        assertEquals(3, colors.size)
        assertEquals(baseColor, colors[0].second)
        assertEquals(highlightColor, colors[1].second)
        assertEquals(baseColor, colors[2].second)
        assertEquals(0f, colors[0].first)
        assertEquals(0.3f, colors[1].first)
        assertEquals(0.6f, colors[2].first)
    }

    @Test
    fun `shimmerGradientColors at progress 0_5 shifts offsets`() {
        val colors = shimmerGradientColors(baseColor, highlightColor, 0.5f)

        assertEquals(3, colors.size)
        assertEquals(baseColor, colors[0].second)
        assertEquals(highlightColor, colors[1].second)
        assertEquals(baseColor, colors[2].second)
        assertEquals(1f, colors[0].first)
        assertEquals(1f, colors[1].first, 0.01f)
        assertEquals(1f, colors[2].first, 0.01f)
    }

    @Test
    fun `shimmerGradientColors at progress 1 clamps offsets to 1`() {
        val colors = shimmerGradientColors(baseColor, highlightColor, 1f)

        assertEquals(3, colors.size)
        colors.forEach { (offset, _) ->
            assert(offset in 0f..1f) { "Offset $offset is out of [0, 1] range" }
        }
    }

    @Test
    fun `shimmerGradientColors preserves color order at all progress values`() {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
            val colors = shimmerGradientColors(baseColor, highlightColor, progress)
            assertEquals(baseColor, colors[0].second)
            assertEquals(highlightColor, colors[1].second)
            assertEquals(baseColor, colors[2].second)
        }
    }

    @Test
    fun `shimmerGradientColors offsets are monotonically non-decreasing`() {
        listOf(0f, 0.1f, 0.3f, 0.5f, 0.8f, 1f).forEach { progress ->
            val colors = shimmerGradientColors(baseColor, highlightColor, progress)
            for (i in 0 until colors.size - 1) {
                assert(colors[i].first <= colors[i + 1].first) {
                    "At progress=$progress, offset ${colors[i].first} > ${colors[i + 1].first}"
                }
            }
        }
    }
}
