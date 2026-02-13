package com.gabedev.mangako.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverDtoTest {

    // --- CoverArtAttributesDTO tests ---

    @Test
    fun `CoverArtAttributesDTO instantiation`() {
        val attrs = CoverArtAttributesDTO(
            description = "Volume 1 cover",
            volume = "1",
            fileName = "cover-file.jpg",
            locale = "ja",
            createdAt = "2020-01-01T00:00:00+00:00",
            updatedAt = "2024-01-01T00:00:00+00:00",
            version = 1
        )

        assertEquals("Volume 1 cover", attrs.description)
        assertEquals("1", attrs.volume)
        assertEquals("cover-file.jpg", attrs.fileName)
        assertEquals("ja", attrs.locale)
        assertEquals(1, attrs.version)
    }

    // --- CoverArtDTO tests ---

    @Test
    fun `CoverArtDTO instantiation`() {
        val dto = CoverArtDTO(
            id = "cover-1",
            type = "cover_art",
            attributes = CoverArtAttributesDTO(
                description = "Cover",
                volume = "1",
                fileName = "file.jpg",
                locale = "ja",
                createdAt = "2020-01-01",
                updatedAt = "2024-01-01",
                version = 1
            )
        )

        assertEquals("cover-1", dto.id)
        assertEquals("cover_art", dto.type)
        assertEquals("file.jpg", dto.attributes.fileName)
    }

    @Test
    fun `CoverArtDTO equality`() {
        val attrs = CoverArtAttributesDTO("", "1", "f.jpg", "ja", "c", "u", 1)
        val dto1 = CoverArtDTO("id", "cover_art", attrs)
        val dto2 = CoverArtDTO("id", "cover_art", attrs)

        assertEquals(dto1, dto2)
    }

    // --- CoverArtListResponseDTO tests ---

    @Test
    fun `CoverArtListResponseDTO instantiation`() {
        val attrs = CoverArtAttributesDTO("", "1", "f.jpg", "ja", "c", "u", 1)
        val coverArt = CoverArtDTO("cover-1", "cover_art", attrs)

        val response = CoverArtListResponseDTO(
            result = "ok",
            response = "collection",
            data = listOf(coverArt),
            limit = 50,
            offset = 0,
            total = 1
        )

        assertEquals("ok", response.result)
        assertEquals(1, response.data.size)
        assertEquals(50, response.limit)
        assertEquals(0, response.offset)
        assertEquals(1, response.total)
    }

    @Test
    fun `CoverArtListResponseDTO with empty data`() {
        val response = CoverArtListResponseDTO(
            result = "ok",
            response = "collection",
            data = emptyList(),
            limit = 50,
            offset = 0,
            total = 0
        )

        assertTrue(response.data.isEmpty())
        assertEquals(0, response.total)
    }

    // --- CoverArtAttributes (second variant) tests ---

    @Test
    fun `CoverArtAttributes instantiation with nullable volume`() {
        val attrs = CoverArtAttributes(
            description = "Desc",
            volume = null,
            fileName = "file.jpg",
            locale = "en",
            createdAt = "2020-01-01",
            updatedAt = "2024-01-01",
            version = 2
        )

        assertNull(attrs.volume)
        assertEquals("file.jpg", attrs.fileName)
    }

    @Test
    fun `CoverArtAttributes with volume set`() {
        val attrs = CoverArtAttributes(
            description = "Desc",
            volume = "5",
            fileName = "file.jpg",
            locale = "en",
            createdAt = "2020-01-01",
            updatedAt = "2024-01-01",
            version = 2
        )

        assertEquals("5", attrs.volume)
    }

    // --- CoverArtResponseDTO tests ---

    @Test
    fun `CoverArtResponseDTO instantiation`() {
        val relationship = Relationship(id = "manga-1", type = "manga")
        val data = CoverArtData(
            id = "cover-1",
            type = "cover_art",
            attributes = CoverArtAttributes("", null, "f.jpg", "ja", "c", "u", 1),
            relationships = listOf(relationship)
        )
        val response = CoverArtResponseDTO(
            result = "ok",
            response = "entity",
            data = data
        )

        assertEquals("ok", response.result)
        assertEquals("cover-1", response.data.id)
        assertEquals(1, response.data.relationships.size)
        assertEquals("manga", response.data.relationships[0].type)
    }

    // --- Relationship tests ---

    @Test
    fun `Relationship instantiation and equality`() {
        val rel1 = Relationship(id = "id-1", type = "manga")
        val rel2 = Relationship(id = "id-1", type = "manga")

        assertEquals(rel1, rel2)
        assertEquals("id-1", rel1.id)
        assertEquals("manga", rel1.type)
    }

    // --- CoverArtData tests ---

    @Test
    fun `CoverArtData instantiation with empty relationships`() {
        val data = CoverArtData(
            id = "cover-1",
            type = "cover_art",
            attributes = CoverArtAttributes("", "1", "f.jpg", "ja", "c", "u", 1),
            relationships = emptyList()
        )

        assertEquals("cover-1", data.id)
        assertTrue(data.relationships.isEmpty())
    }
}
