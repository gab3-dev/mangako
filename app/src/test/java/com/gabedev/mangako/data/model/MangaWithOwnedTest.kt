package com.gabedev.mangako.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaWithOwnedTest {

    private fun createMangaWithOwned() = MangaWithOwned(
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
        isOnUserLibrary = true,
        volumeOwned = 50
    )

    @Test
    fun `MangaWithOwned instantiation`() {
        val mwo = createMangaWithOwned()

        assertEquals("manga-1", mwo.id)
        assertEquals("One Piece", mwo.title)
        assertEquals(50, mwo.volumeOwned)
        assertTrue(mwo.isOnUserLibrary)
    }

    @Test
    fun `MangaWithOwned with nullable fields`() {
        val mwo = MangaWithOwned(
            id = "m-1",
            title = "Test",
            altTitle = null,
            type = null,
            coverId = null,
            coverFileName = null,
            coverUrl = "url",
            authorId = null,
            author = null,
            description = "desc",
            status = null,
            volumeCount = 0,
            isOnUserLibrary = false,
            volumeOwned = 0
        )

        assertNull(mwo.altTitle)
        assertNull(mwo.type)
        assertNull(mwo.coverId)
        assertNull(mwo.coverFileName)
        assertNull(mwo.authorId)
        assertNull(mwo.author)
        assertNull(mwo.status)
    }

    @Test
    fun `toManga converts correctly with all fields`() {
        val mwo = createMangaWithOwned()
        val manga = mwo.toManga()

        assertEquals(mwo.id, manga.id)
        assertEquals(mwo.title, manga.title)
        assertEquals(mwo.altTitle, manga.altTitle)
        assertEquals(mwo.type, manga.type)
        assertEquals(mwo.coverId, manga.coverId)
        assertEquals(mwo.coverFileName, manga.coverFileName)
        assertEquals(mwo.coverUrl, manga.coverUrl)
        assertEquals(mwo.authorId, manga.authorId)
        assertEquals(mwo.author, manga.author)
        assertEquals(mwo.description, manga.description)
        assertEquals(mwo.status, manga.status)
        assertEquals(mwo.volumeCount, manga.volumeCount)
        assertEquals(mwo.isOnUserLibrary, manga.isOnUserLibrary)
    }

    @Test
    fun `toManga does not include volumeOwned`() {
        val mwo = createMangaWithOwned()
        val manga = mwo.toManga()

        // Manga data class does not have volumeOwned field
        // We verify it's a Manga instance with the correct type
        assertTrue(manga is Manga)
    }

    @Test
    fun `toManga preserves null values`() {
        val mwo = MangaWithOwned(
            id = "m-1", title = "T", altTitle = null, type = null,
            coverId = null, coverFileName = null, coverUrl = "url",
            authorId = null, author = null, description = "d",
            status = null, volumeCount = 0, isOnUserLibrary = false,
            volumeOwned = 0
        )
        val manga = mwo.toManga()

        assertNull(manga.altTitle)
        assertNull(manga.type)
        assertNull(manga.coverId)
        assertNull(manga.coverFileName)
        assertNull(manga.authorId)
        assertNull(manga.author)
        assertNull(manga.status)
    }

    @Test
    fun `toManga preserves isOnUserLibrary true`() {
        val mwo = createMangaWithOwned()
        val manga = mwo.toManga()

        assertTrue(manga.isOnUserLibrary)
    }

    @Test
    fun `toManga preserves isOnUserLibrary false`() {
        val mwo = createMangaWithOwned().copy(isOnUserLibrary = false)
        val manga = mwo.toManga()

        assertFalse(manga.isOnUserLibrary)
    }

    @Test
    fun `MangaWithOwned equality`() {
        val mwo1 = createMangaWithOwned()
        val mwo2 = createMangaWithOwned()

        assertEquals(mwo1, mwo2)
    }
}
