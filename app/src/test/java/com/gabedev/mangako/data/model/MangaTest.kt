package com.gabedev.mangako.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaTest {

    private fun createManga() = Manga(
        id = "manga-1",
        title = "One Piece",
        altTitle = "Wan Pīsu",
        type = "manga",
        coverId = "cover-1",
        coverFileName = "cover.jpg",
        coverUrl = "https://uploads.mangadex.org/covers/manga-1/cover.jpg",
        authorId = "author-1",
        author = "Eiichiro Oda",
        description = "A pirate adventure",
        status = "ongoing",
        volumeCount = 105,
        isOnUserLibrary = true
    )

    @Test
    fun `Manga instantiation with all fields`() {
        val manga = createManga()

        assertEquals("manga-1", manga.id)
        assertEquals("One Piece", manga.title)
        assertEquals("Wan Pīsu", manga.altTitle)
        assertEquals("manga", manga.type)
        assertEquals("cover-1", manga.coverId)
        assertEquals("cover.jpg", manga.coverFileName)
        assertEquals("https://uploads.mangadex.org/covers/manga-1/cover.jpg", manga.coverUrl)
        assertEquals("author-1", manga.authorId)
        assertEquals("Eiichiro Oda", manga.author)
        assertEquals("A pirate adventure", manga.description)
        assertEquals("ongoing", manga.status)
        assertEquals(105, manga.volumeCount)
        assertTrue(manga.isOnUserLibrary)
    }

    @Test
    fun `Manga default values`() {
        val manga = Manga(
            id = "m-1",
            title = "Test",
            coverUrl = "url",
            description = "desc"
        )

        assertNull(manga.altTitle)
        assertNull(manga.type)
        assertNull(manga.coverId)
        assertNull(manga.coverFileName)
        assertNull(manga.authorId)
        assertNull(manga.author)
        assertNull(manga.status)
        assertEquals(0, manga.volumeCount)
        assertFalse(manga.isOnUserLibrary)
    }

    @Test
    fun `Manga equality`() {
        val manga1 = createManga()
        val manga2 = createManga()

        assertEquals(manga1, manga2)
    }

    @Test
    fun `Manga inequality on different id`() {
        val manga1 = createManga()
        val manga2 = createManga().copy(id = "manga-2")

        assertNotEquals(manga1, manga2)
    }

    @Test
    fun `Manga copy preserves unchanged fields`() {
        val original = createManga()
        val copy = original.copy(title = "New Title")

        assertEquals("New Title", copy.title)
        assertEquals(original.id, copy.id)
        assertEquals(original.coverUrl, copy.coverUrl)
        assertEquals(original.isOnUserLibrary, copy.isOnUserLibrary)
    }

    @Test
    fun `Manga copy changes isOnUserLibrary`() {
        val manga = createManga().copy(isOnUserLibrary = false)

        assertFalse(manga.isOnUserLibrary)
    }

    @Test
    fun `Manga hashCode consistency`() {
        val manga1 = createManga()
        val manga2 = createManga()

        assertEquals(manga1.hashCode(), manga2.hashCode())
    }
}
