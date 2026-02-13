package com.gabedev.mangako.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MangaDtoTest {

    private fun createLinksDto() = LinksDto(
        al = "12345",
        ap = "slug",
        bw = "bw-id",
        kt = "kt-id",
        mu = "mu-id",
        mal = "mal-id",
        raw = "https://example.com"
    )

    private fun createTagAttributesDto() = TagAttributesDto(
        name = mapOf("en" to "Action"),
        description = mapOf("en" to "Action tag"),
        group = "genre",
        version = 1
    )

    private fun createTagDto() = TagDto(
        id = "tag-1",
        type = "tag",
        attributes = createTagAttributesDto()
    )

    private fun createAttributesDto() = AttributesDto(
        title = mapOf("en" to "One Piece"),
        altTitles = listOf(mapOf("ja-ro" to "Wan Pīsu")),
        description = mapOf("en" to "A pirate adventure"),
        isLocked = false,
        links = createLinksDto(),
        originalLanguage = "ja",
        lastVolume = "105",
        lastChapter = "1100",
        publicationDemographic = "shounen",
        status = "ongoing",
        year = 1997,
        contentRating = "safe",
        tags = listOf(createTagDto()),
        state = "published",
        chapterNumbersResetOnNewVolume = false,
        createdAt = "2020-01-01T00:00:00+00:00",
        updatedAt = "2024-01-01T00:00:00+00:00",
        version = 10,
        availableTranslatedLanguages = listOf("en", "pt-br"),
        latestUploadedChapter = "chapter-uuid"
    )

    private fun createRelationshipDto() = RelationshipDto(
        id = "rel-1",
        type = "author"
    )

    private fun createMangaDto() = MangaDto(
        id = "manga-1",
        type = "manga",
        attributes = createAttributesDto(),
        relationships = listOf(createRelationshipDto())
    )

    // --- MangaResponseDto tests ---

    @Test
    fun `MangaResponseDto instantiation with valid data`() {
        val mangaDto = createMangaDto()
        val response = MangaResponseDto(
            result = "ok",
            response = "entity",
            data = mangaDto
        )

        assertEquals("ok", response.result)
        assertEquals("entity", response.response)
        assertEquals("manga-1", response.data.id)
    }

    @Test
    fun `MangaResponseDto equality`() {
        val mangaDto = createMangaDto()
        val response1 = MangaResponseDto("ok", "entity", mangaDto)
        val response2 = MangaResponseDto("ok", "entity", mangaDto)

        assertEquals(response1, response2)
    }

    @Test
    fun `MangaResponseDto copy`() {
        val mangaDto = createMangaDto()
        val response = MangaResponseDto("ok", "entity", mangaDto)
        val copy = response.copy(result = "error")

        assertEquals("error", copy.result)
        assertEquals("entity", copy.response)
    }

    // --- MangaDto tests ---

    @Test
    fun `MangaDto instantiation with valid data`() {
        val dto = createMangaDto()

        assertEquals("manga-1", dto.id)
        assertEquals("manga", dto.type)
        assertEquals("One Piece", dto.attributes.title?.get("en"))
        assertEquals(1, dto.relationships.size)
    }

    @Test
    fun `MangaDto with null type`() {
        val dto = createMangaDto().copy(type = null)

        assertNull(dto.type)
        assertEquals("manga-1", dto.id)
    }

    // --- AttributesDto tests ---

    @Test
    fun `AttributesDto has correct fields`() {
        val attrs = createAttributesDto()

        assertEquals("One Piece", attrs.title?.get("en"))
        assertEquals("Wan Pīsu", attrs.altTitles?.get(0)?.get("ja-ro"))
        assertEquals("A pirate adventure", attrs.description?.get("en"))
        assertFalse(attrs.isLocked)
        assertEquals("ja", attrs.originalLanguage)
        assertEquals("105", attrs.lastVolume)
        assertEquals("1100", attrs.lastChapter)
        assertEquals("shounen", attrs.publicationDemographic)
        assertEquals("ongoing", attrs.status)
        assertEquals(1997, attrs.year)
        assertEquals("safe", attrs.contentRating)
        assertEquals(1, attrs.tags.size)
        assertEquals("published", attrs.state)
        assertFalse(attrs.chapterNumbersResetOnNewVolume)
        assertEquals(10, attrs.version)
        assertEquals(2, attrs.availableTranslatedLanguages.size)
    }

    @Test
    fun `AttributesDto with null optional fields`() {
        val attrs = createAttributesDto().copy(
            title = null,
            altTitles = null,
            description = null,
            originalLanguage = null,
            lastVolume = null,
            lastChapter = null,
            publicationDemographic = null,
            status = null,
            year = null,
            contentRating = null,
            state = null,
            createdAt = null,
            updatedAt = null,
            version = null,
            latestUploadedChapter = null
        )

        assertNull(attrs.title)
        assertNull(attrs.altTitles)
        assertNull(attrs.description)
        assertNull(attrs.status)
    }

    // --- LinksDto tests ---

    @Test
    fun `LinksDto instantiation`() {
        val links = createLinksDto()

        assertEquals("12345", links.al)
        assertEquals("slug", links.ap)
        assertEquals("bw-id", links.bw)
    }

    @Test
    fun `LinksDto with all null values`() {
        val links = LinksDto(
            al = null, ap = null, bw = null, kt = null,
            mu = null, mal = null, raw = null
        )

        assertNull(links.al)
        assertNull(links.ap)
        assertNull(links.raw)
    }

    // --- TagDto tests ---

    @Test
    fun `TagDto instantiation`() {
        val tag = createTagDto()

        assertEquals("tag-1", tag.id)
        assertEquals("tag", tag.type)
        assertEquals("Action", tag.attributes.name["en"])
    }

    @Test
    fun `TagDto with null id and type`() {
        val tag = createTagDto().copy(id = null, type = null)

        assertNull(tag.id)
        assertNull(tag.type)
    }

    // --- TagAttributesDto tests ---

    @Test
    fun `TagAttributesDto default description is empty map`() {
        val attrs = TagAttributesDto(
            name = mapOf("en" to "Action"),
            group = "genre",
            version = 1
        )

        assertEquals(emptyMap<String, String?>(), attrs.description)
    }

    @Test
    fun `TagAttributesDto with null group and version`() {
        val attrs = TagAttributesDto(
            name = mapOf("en" to "Test"),
            group = null,
            version = null
        )

        assertNull(attrs.group)
        assertNull(attrs.version)
    }

    // --- RelationshipDto tests ---

    @Test
    fun `RelationshipDto instantiation`() {
        val rel = createRelationshipDto()

        assertEquals("rel-1", rel.id)
        assertEquals("author", rel.type)
    }

    @Test
    fun `RelationshipDto with null values`() {
        val rel = RelationshipDto(id = null, type = null)

        assertNull(rel.id)
        assertNull(rel.type)
    }
}
