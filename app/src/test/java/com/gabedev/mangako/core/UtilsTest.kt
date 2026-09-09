package com.gabedev.mangako.core

import com.gabedev.mangako.data.dto.AttributesDto
import com.gabedev.mangako.data.dto.LinksDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

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

    @Test
    fun `handleVolumeLabel appends locale only for special editions`() {
        assertEquals("1", Utils.handleVolumeLabel(1.0f, "ja", false))
        assertEquals("1 (en)", Utils.handleVolumeLabel(1.0f, "en", true))
        assertEquals("1.5 (ja)", Utils.handleVolumeLabel(1.5f, "ja", true))
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
    fun `handleMangaDescription returns pt-br for Brazilian devices`() {
        val attrs = createAttributesDto(
            description = mapOf("pt-br" to "Descrição em português", "en" to "English description")
        )

        assertEquals("Descrição em português", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-BR")))
    }

    @Test
    fun `handleMangaDescription falls back to en`() {
        val attrs = createAttributesDto(
            description = mapOf("en" to "English description")
        )

        assertEquals("English description", Utils.handleMangaDescription(attrs, Locale.JAPANESE))
    }

    @Test
    fun `handleMangaDescription falls back to ja-ro`() {
        val attrs = createAttributesDto(
            description = mapOf("ja-ro" to "Japanese romanized")
        )

        assertEquals("Japanese romanized", Utils.handleMangaDescription(attrs, Locale.ENGLISH))
    }

    @Test
    fun `handleMangaDescription uses another available language as last fallback`() {
        val attrs = createAttributesDto(
            description = mapOf("zh" to "中文描述")
        )

        assertEquals("中文描述", Utils.handleMangaDescription(attrs, Locale.ENGLISH))
    }

    @Test
    fun `handleMangaDescription with null description`() {
        val attrs = createAttributesDto(description = null)

        assertEquals("", Utils.handleMangaDescription(attrs, Locale.ENGLISH))
    }

    @Test
    fun `handleMangaDescription with empty description map`() {
        val attrs = createAttributesDto(description = emptyMap())

        assertEquals("", Utils.handleMangaDescription(attrs, Locale.ENGLISH))
    }

    @Test
    fun `description follows device language instead of always preferring Portuguese`() {
        val attrs = createAttributesDto(description = mapOf(
            "pt-br" to "Portuguese", "en" to "English", "ja" to "Japanese",
        ))
        assertEquals("English", Utils.handleMangaDescription(attrs, Locale.US))
        assertEquals("Japanese", Utils.handleMangaDescription(attrs, Locale.JAPAN))
        assertEquals("English", Utils.handleMangaDescription(attrs, Locale.GERMAN))
    }

    @Test
    fun `description matches normalized region before base language`() {
        val attrs = createAttributesDto(description = mapOf(
            "pt" to "Generic", "PT_br" to "Brazilian", "pt-PT" to "Portugal", "en" to "English",
        ))
        assertEquals("Brazilian", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-BR")))
        assertEquals("Portugal", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-PT")))
        assertEquals("Generic", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-AO")))
    }

    @Test
    fun `description uses another regional variant before English`() {
        val attrs = createAttributesDto(description = mapOf("en" to "English", "pt-br" to "Brazilian"))
        assertEquals("Brazilian", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun `description skips null empty and whitespace values`() {
        val attrs = createAttributesDto(description = mapOf(
            "pt-br" to "  ", "pt" to "", "pt-pt" to null, "en" to "English",
        ))
        assertEquals("English", Utils.handleMangaDescription(attrs, Locale.forLanguageTag("pt-BR")))
        assertEquals("", Utils.localizedDescription(listOf("en" to "\n", "ja" to null), Locale.US))
    }
}
