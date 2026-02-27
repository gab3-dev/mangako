package com.gabedev.mangako.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeTest {

    private fun createVolume() = Volume(
        id = "vol-1",
        mangaId = "manga-1",
        title = "One Piece",
        coverUrl = "https://uploads.mangadex.org/covers/manga-1/vol1.jpg",
        volume = 1.0f,
        owned = false,
        locale = "ja"
    )

    @Test
    fun `Volume instantiation with all fields`() {
        val volume = createVolume()

        assertEquals("vol-1", volume.id)
        assertEquals("manga-1", volume.mangaId)
        assertEquals("One Piece", volume.title)
        assertEquals("https://uploads.mangadex.org/covers/manga-1/vol1.jpg", volume.coverUrl)
        assertEquals(1.0f, volume.volume)
        assertFalse(volume.owned)
    }

    @Test
    fun `Volume default owned is false`() {
        val volume = Volume(
            id = "v-1",
            mangaId = "m-1",
            title = "Test",
            coverUrl = "url",
            volume = 1.0f,
            locale = "ja"
        )

        assertFalse(volume.owned)
    }

    @Test
    fun `Volume with null volume number`() {
        val volume = createVolume().copy(volume = null)

        assertNull(volume.volume)
    }

    @Test
    fun `Volume equality`() {
        val vol1 = createVolume()
        val vol2 = createVolume()

        assertEquals(vol1, vol2)
    }

    @Test
    fun `Volume copy toggles owned`() {
        val volume = createVolume()
        val toggled = volume.copy(owned = true)

        assertTrue(toggled.owned)
        assertFalse(volume.owned)
    }

    @Test
    fun `Volume copy preserves other fields`() {
        val original = createVolume()
        val copy = original.copy(owned = true)

        assertEquals(original.id, copy.id)
        assertEquals(original.mangaId, copy.mangaId)
        assertEquals(original.title, copy.title)
        assertEquals(original.coverUrl, copy.coverUrl)
        assertEquals(original.volume, copy.volume)
    }

    @Test
    fun `Volume with fractional volume number`() {
        val volume = createVolume().copy(volume = 1.5f)

        assertEquals(1.5f, volume.volume)
    }

    @Test
    fun `Volume hashCode consistency`() {
        val vol1 = createVolume()
        val vol2 = createVolume()

        assertEquals(vol1.hashCode(), vol2.hashCode())
    }

    @Test
    fun `isSpecialEdition defaults to false`() {
        val volume = createVolume()

        assertFalse(volume.isSpecialEdition)
    }

    @Test
    fun `isSpecialEdition true for special edition volume`() {
        val volume = createVolume().copy(volume = 13.1f, isSpecialEdition = true)

        assertTrue(volume.isSpecialEdition)
        assertEquals(13.1f, volume.volume)
    }
}
