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

    fun handleMangaTitle(attributes: AttributesDto, locale: Locale = Locale.getDefault()): String? {
        return localizedTitle(
            attributes.title.orEmpty().toList() + attributes.altTitles.orEmpty().flatMap { it.toList() },
            locale,
        )
    }

    fun handleMangaAlternativeTitle(attributes: AttributesDto): String? = romanizedTitle(
        attributes.title.orEmpty().toList() + attributes.altTitles.orEmpty().flatMap { it.toList() },
    )

    fun romanizedTitle(titles: List<Pair<String, String?>>): String? = titles.firstNotNullOfOrNull { (tag, text) ->
        text?.takeIf { tag.replace('_', '-').equals("ja-ro", ignoreCase = true) && it.isNotBlank() }
    }

    fun localizedTitle(titles: List<Pair<String, String?>>, locale: Locale = Locale.getDefault()): String? {
        val available = titles.mapNotNull { (language, text) ->
            text?.takeIf { it.isNotBlank() }?.let {
                language.replace('_', '-').lowercase(Locale.ROOT) to it
            }
        }
        val tag = locale.toLanguageTag().lowercase(Locale.ROOT)
        val language = locale.language.lowercase(Locale.ROOT)
        // Japanese must never match the romanized ja-ro tag or another language.
        if (language == "ja") return available.firstOrNull { it.first == "ja" }?.second
        val localTitle = available.firstOrNull { it.first == tag }?.second
            ?: available.firstOrNull { tag != "pt-br" && it.first == language }?.second
        // pt-BR has a strict chain: pt-br -> en -> ja-ro.
        val regionalTitle = if (tag == "pt-br") null else
            available.firstOrNull { it.first.startsWith("$language-") }?.second
        return localTitle
            ?: regionalTitle
            ?: available.firstOrNull { it.first == "en" }?.second
            ?: available.firstOrNull { it.first.startsWith("en-") }?.second
            ?: available.firstOrNull { it.first == "ja-ro" }?.second
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
        if (language == "ja") return available.firstOrNull { it.first == "ja" }?.second.orEmpty()
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
