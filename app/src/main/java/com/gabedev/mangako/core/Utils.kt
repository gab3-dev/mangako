package com.gabedev.mangako.core

import com.gabedev.mangako.data.dto.AttributesDto

object Utils {
    // Remove ".0" from the string, just for exhibition
    fun handleFloatVolume(volume: Float?) :String {
        val volumeStr = volume.toString()
        if (volumeStr.isEmpty()) {
            return "N/A"
        }
        return volumeStr.removeSuffix(".0")
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

    // Search and return the correct manga description
    fun handleMangaDescription(attributes: AttributesDto) :String  {
        val mangaDescription = attributes.description?.get("pt-br")
            ?: attributes.description?.get("en")
            ?: attributes.description?.get("ja-ro")
        return mangaDescription ?: "Nenhuma descrição disponível"
    }
}