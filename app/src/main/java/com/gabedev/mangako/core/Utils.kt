package com.gabedev.mangako.core

import com.gabedev.mangako.data.dto.AttributesDto
import java.util.Locale

object Utils {
    // Remove ".0" from the string, just for exhibition
    fun handleFloatVolume(volume: Float?) :String {
        val volumeStr = volume.toString()
        if (volumeStr.isEmpty()) {
            return "N/A"
        }
        return volumeStr.removeSuffix(".0")
    }

    fun handleVolumeLabel(volume: Float?, locale: String, isSpecialEdition: Boolean): String {
        val volumeLabel = handleFloatVolume(volume)
        return if (isSpecialEdition) {
            "$volumeLabel ($locale)"
        } else {
            volumeLabel
        }
    }

    // Search and return the correct manga title
    fun handleMangaTitle(attributes: AttributesDto) :String  {
        val mangatitle = attributes.title?.get("en")
            ?: attributes.altTitles?.find { it.containsKey("en") }
                ?.get("en")
            ?: attributes.title?.get("ja-ro")
            ?: attributes.title?.get("pt-br")
            ?: attributes.altTitles?.find { it.containsKey("pt-br") }
                ?.get("pt-br")
        return mangatitle ?: "Titulo não encontrado"
    }

    fun handleMangaDescription(attributes: AttributesDto, locale: Locale = Locale.getDefault()): String {
        return localizedDescription(attributes.description.orEmpty().toList(), locale)
    }

    fun localizedDescription(
        descriptions: List<Pair<String, String?>>,
        locale: Locale = Locale.getDefault(),
    ): String {
        val available = descriptions.mapNotNull { (language, text) ->
            text?.takeIf { it.isNotBlank() }?.let {
                language.replace('_', '-').lowercase(Locale.ROOT) to it
            }
        }
        val tag = locale.toLanguageTag().lowercase(Locale.ROOT)
        val language = locale.language.lowercase(Locale.ROOT)
        // Prefer the exact region, then the base language and its other regional variants.
        return available.firstOrNull { it.first == tag }?.second
            ?: available.firstOrNull { it.first == language }?.second
            ?: available.firstOrNull { it.first.startsWith("$language-") }?.second
            ?: available.firstOrNull { it.first == "en" }?.second
            ?: available.firstOrNull { it.first.startsWith("en-") }?.second
            ?: available.firstOrNull()?.second
            ?: ""
    }
}
