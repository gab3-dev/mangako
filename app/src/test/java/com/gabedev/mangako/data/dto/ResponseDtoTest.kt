package com.gabedev.mangako.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseDtoTest {

    private fun createMangaDto(): MangaDto {
        return MangaDto(
            id = "manga-1",
            type = "manga",
            attributes = AttributesDto(
                title = mapOf("en" to "Test Manga"),
                altTitles = null,
                description = mapOf("en" to "A test manga"),
                isLocked = false,
                links = LinksDto(null, null, null, null, null, null, null),
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
            ),
            relationships = emptyList()
        )
    }

    @Test
    fun `MangaListResponse instantiation`() {
        val mangaDto = createMangaDto()
        val response = MangaListResponse(
            result = "ok",
            response = "collection",
            data = listOf(mangaDto),
            limit = 10,
            offset = 0,
            total = 1
        )

        assertEquals("ok", response.result)
        assertEquals("collection", response.response)
        assertEquals(1, response.data.size)
        assertEquals("manga-1", response.data[0].id)
        assertEquals(10, response.limit)
        assertEquals(0, response.offset)
        assertEquals(1, response.total)
    }

    @Test
    fun `MangaListResponse with empty data list`() {
        val response = MangaListResponse(
            result = "ok",
            response = "collection",
            data = emptyList(),
            limit = 10,
            offset = 0,
            total = 0
        )

        assertTrue(response.data.isEmpty())
        assertEquals(0, response.total)
    }

    @Test
    fun `MangaListResponse with multiple manga entries`() {
        val manga1 = createMangaDto()
        val manga2 = createMangaDto().copy(id = "manga-2")
        val manga3 = createMangaDto().copy(id = "manga-3")

        val response = MangaListResponse(
            result = "ok",
            response = "collection",
            data = listOf(manga1, manga2, manga3),
            limit = 10,
            offset = 0,
            total = 3
        )

        assertEquals(3, response.data.size)
        assertEquals("manga-2", response.data[1].id)
    }

    @Test
    fun `MangaListResponse equality`() {
        val manga = createMangaDto()
        val response1 = MangaListResponse("ok", "collection", listOf(manga), 10, 0, 1)
        val response2 = MangaListResponse("ok", "collection", listOf(manga), 10, 0, 1)

        assertEquals(response1, response2)
    }

    @Test
    fun `MangaListResponse copy with different offset`() {
        val response = MangaListResponse(
            "ok", "collection", emptyList(), 10, 0, 0
        )
        val copy = response.copy(offset = 20)

        assertEquals(20, copy.offset)
        assertEquals(0, response.offset)
    }
}
