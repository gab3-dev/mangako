package com.gabedev.mangako.ui.screens.detail.covertheme

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun generateCoverTheme(
    context: Context,
    coverData: Any,
    isDarkMode: Boolean,
    backgroundColor: Int,
    toolbarBaseColor: Int,
): CoverTheme? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(coverData)
        .allowHardware(false)
        .build()
    val result = ImageLoader.Builder(context).build().execute(request) as? SuccessResult ?: return@withContext null
    val bitmap = result.drawable.toBitmap()
    val sourceColor = Palette.from(bitmap).generate().getBestColor() ?: return@withContext null
    val accentColor = deriveAccentColor(sourceColor, isDarkMode)

    CoverTheme(
        sourceColor = sourceColor,
        accentColor = accentColor,
        onAccentColor = readableTextColor(accentColor),
        backdropColor = ColorUtils.blendARGB(sourceColor, backgroundColor, if (isDarkMode) 0.82f else 0.88f),
        headerColor = deriveHeaderColor(sourceColor, toolbarBaseColor),
    )
}

private fun deriveAccentColor(sourceColor: Int, isDarkMode: Boolean): Int {
    val luminance = ColorUtils.calculateLuminance(sourceColor)
    return when {
        isDarkMode && luminance < 0.35 -> ColorUtils.blendARGB(sourceColor, Color.WHITE, 0.38f)
        !isDarkMode && luminance > 0.65 -> ColorUtils.blendARGB(sourceColor, Color.BLACK, 0.32f)
        !isDarkMode && luminance < 0.18 -> ColorUtils.blendARGB(sourceColor, Color.WHITE, 0.18f)
        else -> sourceColor
    }
}

private fun deriveHeaderColor(sourceColor: Int, toolbarBaseColor: Int): Int {
    val sourceHsl = FloatArray(3)
    val baseHsl = FloatArray(3)
    ColorUtils.colorToHSL(sourceColor, sourceHsl)
    ColorUtils.colorToHSL(toolbarBaseColor, baseHsl)
    baseHsl[0] = sourceHsl[0]
    return ColorUtils.HSLToColor(baseHsl)
}

private fun readableTextColor(backgroundColor: Int): Int {
    val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, backgroundColor)
    val blackContrast = ColorUtils.calculateContrast(Color.BLACK, backgroundColor)
    return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
}
