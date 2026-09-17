package com.gabedev.mangako.data.local

import com.gabedev.mangako.data.model.Volume
import java.util.Locale

enum class CoverLanguage(val tag: String) {
    JAPANESE("ja"),
    ORIGINAL("original"),
    PORTUGUESE("pt-BR"),
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    KOREAN("ko"),
    CHINESE("zh");

    // Omitting locale preserves the API's Japanese default with original-language fallback.
    val apiLocale: String? get() = tag.takeUnless { this == JAPANESE }

    fun language(originalLanguage: String?): String? =
        baseLanguage(if (this == ORIGINAL) originalLanguage else tag)

    fun filterVolumes(volumes: List<Volume>, originalLanguage: String?): List<Volume> {
        val requested = language(originalLanguage)
        val hasPreferred = volumes.any { !it.isSpecialEdition && baseLanguage(it.locale) == requested }
        val selected = if (this == JAPANESE && !hasPreferred) baseLanguage(originalLanguage) else requested
        return volumes.filter {
            it.isSpecialEdition || selected == null || baseLanguage(it.locale) == selected
        }
    }

    companion object {
        fun fromStored(value: String?): CoverLanguage = entries.firstOrNull { it.name == value } ?: JAPANESE

        private fun baseLanguage(value: String?): String? = value
            ?.trim()?.replace('_', '-')?.lowercase(Locale.ROOT)?.substringBefore('-')
            ?.takeIf { it.isNotBlank() && it != "und" }
    }
}
