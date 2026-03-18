package com.gabedev.mangako.ui.screens.detail

import com.gabedev.mangako.data.model.Volume
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterVolumesTest {

    private fun volume(
        id: String,
        owned: Boolean = false,
        isSpecialEdition: Boolean = false
    ) = Volume(
        id = id,
        mangaId = "manga-1",
        title = "Title",
        coverUrl = "url",
        volume = 1f,
        locale = "en",
        owned = owned,
        isSpecialEdition = isSpecialEdition
    )

    @Test
    fun `no filters returns all non-special volumes`() {
        val volumes = listOf(
            volume("1", owned = true),
            volume("2", owned = false),
            volume("3", owned = true, isSpecialEdition = true)
        )
        val result = filterVolumes(volumes, showSpecialEditions = false, showNotOwnedOnly = false)
        assertEquals(listOf(volumes[0], volumes[1]), result)
    }

    @Test
    fun `not owned only filter returns unowned non-special volumes`() {
        val volumes = listOf(
            volume("1", owned = true),
            volume("2", owned = false),
            volume("3", owned = false, isSpecialEdition = true)
        )
        val result = filterVolumes(volumes, showSpecialEditions = false, showNotOwnedOnly = true)
        assertEquals(listOf(volumes[1]), result)
    }

    @Test
    fun `both filters active returns all unowned volumes including special`() {
        val volumes = listOf(
            volume("1", owned = true),
            volume("2", owned = false),
            volume("3", owned = false, isSpecialEdition = true)
        )
        val result = filterVolumes(volumes, showSpecialEditions = true, showNotOwnedOnly = true)
        assertEquals(listOf(volumes[1], volumes[2]), result)
    }

    @Test
    fun `special editions filter shows special and not-owned filters compose`() {
        val volumes = listOf(
            volume("1", owned = true, isSpecialEdition = true),
            volume("2", owned = false, isSpecialEdition = true),
            volume("3", owned = false)
        )
        val result = filterVolumes(volumes, showSpecialEditions = true, showNotOwnedOnly = true)
        assertEquals(listOf(volumes[1], volumes[2]), result)
    }

    @Test
    fun `special editions only returns all volumes`() {
        val volumes = listOf(
            volume("1", owned = true),
            volume("2", owned = false, isSpecialEdition = true)
        )
        val result = filterVolumes(volumes, showSpecialEditions = true, showNotOwnedOnly = false)
        assertEquals(volumes, result)
    }

    @Test
    fun `empty list returns empty list`() {
        val result = filterVolumes(emptyList(), showSpecialEditions = true, showNotOwnedOnly = true)
        assertEquals(emptyList<Volume>(), result)
    }

    @Test
    fun `all owned with not-owned filter returns empty`() {
        val volumes = listOf(
            volume("1", owned = true),
            volume("2", owned = true)
        )
        val result = filterVolumes(volumes, showSpecialEditions = false, showNotOwnedOnly = true)
        assertEquals(emptyList<Volume>(), result)
    }
}
