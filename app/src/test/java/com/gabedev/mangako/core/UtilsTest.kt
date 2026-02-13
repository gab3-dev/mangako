package com.gabedev.mangako.core

import com.gabedev.mangako.data.dto.AttributesDto
import com.gabedev.mangako.data.dto.LinksDto
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    private fun createLinksDto() = LinksDto(null, null, null, null, null, null, null)

    private fun createAttributesDto(
        title: Map<String, String?>? = null,
        altTitles: List<Map<String, String?>>? = null,
        description: Map<String, String?>? = null
    ) = AttributesDto(
        title = title,
        altTitles = altTitles,
        description = description,
        isLocked = false,
        links = createLinksDto(),
        originalLanguage = "ja",
        lastVolume = null,
        lastChapter = null,
        publicationDemographic = null,
        status = "ongoing",
        year = 2020,
        contentRating = "safe",
        tags = emptyList(),
        state = "published",
        chapterNumbersResetOnNewVolume = false,
        createdAt = "2020-01-01",
        updatedAt = "2024-01-01",
        version = 1,
        availableTranslatedLanguages = listOf("en"),
        latestUploadedChapter = null
    )

    // --- handleFloatVolume tests ---

    @Test
    fun `handleFloatVolume removes dot zero suffix`() {
        assertEquals("5", Utils.handleFloatVolume(5.0f))
    }

    @Test
    fun `handleFloatVolume keeps decimal for non-integer`() {
        assertEquals("1.5", Utils.handleFloatVolume(1.5f))
    }

    @Test
    fun `handleFloatVolume handles zero`() {
        assertEquals("0", Utils.handleFloatVolume(0.0f))
    }

    @Test
    fun `handleFloatVolume handles null`() {
        assertEquals("null", Utils.handleFloatVolume(null))
    }

    @Test
    fun `handleFloatVolume handles large number`() {
        assertEquals("100", Utils.handleFloatVolume(100.0f))
    }

    @Test
    fun `handleFloatVolume handles fraction`() {
        assertEquals("0.5", Utils.handleFloatVolume(0.5f))
    }

    // --- handleMangaTitle tests ---

    @Test
    fun `handleMangaTitle returns English title when available`() {
        val attrs = createAttributesDto(title = mapOf("en" to "One Piece"))

        assertEquals("One Piece", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle falls back to altTitles en`() {
        val attrs = createAttributesDto(
            title = mapOf("ja" to "ワンピース"),
            altTitles = listOf(mapOf("en" to "One Piece Alt"))
        )

        assertEquals("One Piece Alt", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle falls back to ja-ro`() {
        val attrs = createAttributesDto(
            title = mapOf("ja-ro" to "Wan Pīsu")
        )

        assertEquals("Wan Pīsu", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle falls back to pt-br in title`() {
        val attrs = createAttributesDto(
            title = mapOf("pt-br" to "Uma Peça")
        )

        assertEquals("Uma Peça", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle falls back to altTitles pt-br`() {
        val attrs = createAttributesDto(
            title = mapOf("ja" to "ワンピース"),
            altTitles = listOf(mapOf("pt-br" to "Uma Peça Alt"))
        )

        assertEquals("Uma Peça Alt", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle returns fallback when no title found`() {
        val attrs = createAttributesDto(
            title = mapOf("zh" to "海贼王")
        )

        assertEquals("Titulo não encontrado", Utils.handleMangaTitle(attrs))
    }

    @Test
    fun `handleMangaTitle with null title and altTitles`() {
        val attrs = createAttributesDto(title = null, altTitles = null)

        assertEquals("Titulo não encontrado", Utils.handleMangaTitle(attrs))
    }

    // --- handleMangaDescription tests ---

    @Test
    fun `handleMangaDescription returns pt-br when available`() {
        val attrs = createAttributesDto(
            description = mapOf("pt-br" to "Descrição em português", "en" to "English description")
        )

        assertEquals("Descrição em português", Utils.handleMangaDescription(attrs))
    }

    @Test
    fun `handleMangaDescription falls back to en`() {
        val attrs = createAttributesDto(
            description = mapOf("en" to "English description")
        )

        assertEquals("English description", Utils.handleMangaDescription(attrs))
    }

    @Test
    fun `handleMangaDescription falls back to ja-ro`() {
        val attrs = createAttributesDto(
            description = mapOf("ja-ro" to "Japanese romanized")
        )

        assertEquals("Japanese romanized", Utils.handleMangaDescription(attrs))
    }

    @Test
    fun `handleMangaDescription returns fallback when no description found`() {
        val attrs = createAttributesDto(
            description = mapOf("zh" to "中文描述")
        )

        assertEquals("Nenhuma descrição disponível", Utils.handleMangaDescription(attrs))
    }

    @Test
    fun `handleMangaDescription with null description`() {
        val attrs = createAttributesDto(description = null)

        assertEquals("Nenhuma descrição disponível", Utils.handleMangaDescription(attrs))
    }

    @Test
    fun `handleMangaDescription with empty description map`() {
        val attrs = createAttributesDto(description = emptyMap())

        assertEquals("Nenhuma descrição disponível", Utils.handleMangaDescription(attrs))
    }
}
