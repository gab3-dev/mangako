package com.gabedev.mangako.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorResponseDtoTest {

    private fun createAuthorAttributes() = AuthorAttributes(
        name = "Eiichiro Oda",
        imageUrl = "https://example.com/oda.jpg",
        biography = mapOf("en" to "Creator of One Piece"),
        twitter = "https://twitter.com/oda",
        pixiv = null,
        melonBook = null,
        fanBox = null,
        booth = null,
        namicomi = null,
        nicoVideo = null,
        skeb = null,
        fantia = null,
        tumblr = null,
        youtube = null,
        weibo = null,
        naver = null,
        website = "https://example.com",
        createdAt = "2020-01-01T00:00:00+00:00",
        updatedAt = "2024-01-01T00:00:00+00:00",
        version = 3
    )

    // --- AuthorAttributes tests ---

    @Test
    fun `AuthorAttributes instantiation with all fields`() {
        val attrs = createAuthorAttributes()

        assertEquals("Eiichiro Oda", attrs.name)
        assertEquals("https://example.com/oda.jpg", attrs.imageUrl)
        assertEquals("Creator of One Piece", attrs.biography?.get("en"))
        assertEquals("https://twitter.com/oda", attrs.twitter)
        assertEquals("https://example.com", attrs.website)
        assertEquals(3, attrs.version)
    }

    @Test
    fun `AuthorAttributes default values for optional fields`() {
        val attrs = AuthorAttributes(
            name = "Author Name",
            createdAt = "2020-01-01",
            updatedAt = "2024-01-01",
            version = 1
        )

        assertNull(attrs.imageUrl)
        assertNull(attrs.biography)
        assertNull(attrs.twitter)
        assertNull(attrs.pixiv)
        assertNull(attrs.melonBook)
        assertNull(attrs.fanBox)
        assertNull(attrs.booth)
        assertNull(attrs.namicomi)
        assertNull(attrs.nicoVideo)
        assertNull(attrs.skeb)
        assertNull(attrs.fantia)
        assertNull(attrs.tumblr)
        assertNull(attrs.youtube)
        assertNull(attrs.weibo)
        assertNull(attrs.naver)
        assertNull(attrs.website)
    }

    @Test
    fun `AuthorAttributes equality`() {
        val attrs1 = AuthorAttributes(name = "Oda", createdAt = "c", updatedAt = "u", version = 1)
        val attrs2 = AuthorAttributes(name = "Oda", createdAt = "c", updatedAt = "u", version = 1)

        assertEquals(attrs1, attrs2)
    }

    // --- AuthorData tests ---

    @Test
    fun `AuthorData instantiation`() {
        val relationship = Relationship(id = "manga-1", type = "manga")
        val data = AuthorData(
            id = "author-1",
            type = "author",
            attributes = createAuthorAttributes(),
            relationships = listOf(relationship)
        )

        assertEquals("author-1", data.id)
        assertEquals("author", data.type)
        assertEquals("Eiichiro Oda", data.attributes.name)
        assertEquals(1, data.relationships.size)
    }

    @Test
    fun `AuthorData with empty relationships`() {
        val data = AuthorData(
            id = "author-1",
            type = "author",
            attributes = createAuthorAttributes(),
            relationships = emptyList()
        )

        assertTrue(data.relationships.isEmpty())
    }

    // --- AuthorResponseDTO tests ---

    @Test
    fun `AuthorResponseDTO instantiation`() {
        val data = AuthorData(
            id = "author-1",
            type = "author",
            attributes = createAuthorAttributes(),
            relationships = emptyList()
        )
        val response = AuthorResponseDTO(
            result = "ok",
            response = "entity",
            data = data
        )

        assertEquals("ok", response.result)
        assertEquals("entity", response.response)
        assertEquals("author-1", response.data.id)
    }

    @Test
    fun `AuthorResponseDTO equality`() {
        val data = AuthorData(
            id = "author-1",
            type = "author",
            attributes = createAuthorAttributes(),
            relationships = emptyList()
        )
        val response1 = AuthorResponseDTO("ok", "entity", data)
        val response2 = AuthorResponseDTO("ok", "entity", data)

        assertEquals(response1, response2)
    }

    @Test
    fun `AuthorResponseDTO copy with different result`() {
        val data = AuthorData(
            id = "a-1",
            type = "author",
            attributes = createAuthorAttributes(),
            relationships = emptyList()
        )
        val response = AuthorResponseDTO("ok", "entity", data)
        val copy = response.copy(result = "error")

        assertEquals("error", copy.result)
        assertEquals(response.data, copy.data)
    }
}
