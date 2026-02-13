package com.gabedev.mangako.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaWithVolumeTest {

    private fun createManga() = Manga(
        id = "manga-1",
        title = "One Piece",
        coverUrl = "https://uploads.mangadex.org/covers/manga-1/cover.jpg",
        description = "A pirate adventure"
    )

    private fun createVolume(id: String, volumeNum: Float?) = Volume(
        id = id,
        mangaId = "manga-1",
        title = "One Piece",
        coverUrl = "https://uploads.mangadex.org/covers/manga-1/$id.jpg",
        volume = volumeNum,
        owned = false
    )

    @Test
    fun `MangaWithVolume instantiation with volumes`() {
        val manga = createManga()
        val volumes = listOf(
            createVolume("v1", 1.0f),
            createVolume("v2", 2.0f),
            createVolume("v3", 3.0f)
        )
        val mangaWithVolume = MangaWithVolume(manga = manga, volumes = volumes)

        assertEquals("manga-1", mangaWithVolume.manga.id)
        assertEquals(3, mangaWithVolume.volumes.size)
        assertEquals(1.0f, mangaWithVolume.volumes[0].volume)
        assertEquals(2.0f, mangaWithVolume.volumes[1].volume)
    }

    @Test
    fun `MangaWithVolume instantiation with empty volumes`() {
        val manga = createManga()
        val mangaWithVolume = MangaWithVolume(manga = manga, volumes = emptyList())

        assertEquals("manga-1", mangaWithVolume.manga.id)
        assertTrue(mangaWithVolume.volumes.isEmpty())
    }

    @Test
    fun `MangaWithVolume volumes belong to same manga`() {
        val manga = createManga()
        val volumes = listOf(createVolume("v1", 1.0f), createVolume("v2", 2.0f))
        val mangaWithVolume = MangaWithVolume(manga = manga, volumes = volumes)

        mangaWithVolume.volumes.forEach { volume ->
            assertEquals(manga.id, volume.mangaId)
        }
    }

    @Test
    fun `MangaWithVolume equality`() {
        val manga = createManga()
        val volumes = listOf(createVolume("v1", 1.0f))
        val mwv1 = MangaWithVolume(manga, volumes)
        val mwv2 = MangaWithVolume(manga, volumes)

        assertEquals(mwv1, mwv2)
    }

    @Test
    fun `MangaWithVolume with single volume`() {
        val manga = createManga()
        val volume = createVolume("v1", 1.0f)
        val mangaWithVolume = MangaWithVolume(manga = manga, volumes = listOf(volume))

        assertEquals(1, mangaWithVolume.volumes.size)
        assertEquals("v1", mangaWithVolume.volumes[0].id)
    }
}
