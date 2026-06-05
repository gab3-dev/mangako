package com.gabedev.mangako.ui.screens.detail.covertheme

import androidx.palette.graphics.Palette

fun Palette.getBestColor(): Int? {
    val vibrantPopulation = vibrantSwatch?.population ?: -1
    val dominantLuminance = dominantSwatch?.hsl?.get(2) ?: -1f
    val mutedPopulation = mutedSwatch?.population ?: -1
    val mutedSaturationLimit = if (mutedPopulation > vibrantPopulation * 3f) 0.1f else 0.25f

    return when {
        (dominantSwatch?.hsl?.get(1) ?: 0f) >= 0.25f &&
            dominantLuminance <= 0.8f &&
            dominantLuminance > 0.2f -> dominantSwatch?.rgb

        vibrantPopulation >= mutedPopulation * 0.75f -> vibrantSwatch?.rgb

        mutedPopulation > vibrantPopulation * 1.5f &&
            (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit -> mutedSwatch?.rgb

        else -> listOf(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch)
            .maxByOrNull { swatch ->
                if (swatch === vibrantSwatch) (swatch?.population ?: -1) * 3 else swatch?.population ?: -1
            }
            ?.rgb
    }
}
