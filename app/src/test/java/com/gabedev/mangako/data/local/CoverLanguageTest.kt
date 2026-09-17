package com.gabedev.mangako.data.local

import com.gabedev.mangako.data.model.Volume
import org.junit.Assert.*
import org.junit.Test

class CoverLanguageTest {
    private fun volume(id: String, locale: String, special: Boolean = false) = Volume(
        id = id, mangaId = "m1", title = "Manga", coverUrl = "url/$id",
        volume = if (special) 1.5f else 1f, locale = locale, isSpecialEdition = special,
    )

    @Test
    fun `default and invalid stored preference preserve Japanese API default`() {
        assertEquals(CoverLanguage.JAPANESE, CoverLanguage.fromStored(null))
        assertEquals(CoverLanguage.JAPANESE, CoverLanguage.fromStored("unknown"))
        assertNull(CoverLanguage.JAPANESE.apiLocale)
        assertEquals("original", CoverLanguage.ORIGINAL.apiLocale)
        assertEquals("pt-BR", CoverLanguage.PORTUGUESE.apiLocale)
        CoverLanguage.entries.forEach { assertEquals(it, CoverLanguage.fromStored(it.name)) }
    }

    @Test
    fun `every selected language preserves specials in all languages without changing ownership`() {
        val ja = volume("ja", "ja")
        val en = volume("en", "en")
        val pt = volume("pt", "PT_br").copy(owned = true)
        val specials = listOf(volume("s-ja", "ja", true), volume("s-fr", "fr", true).copy(owned = true))
        val all = listOf(ja, en, pt) + specials
        assertEquals(listOf(ja) + specials, CoverLanguage.JAPANESE.filterVolumes(all, "ja"))
        assertEquals(listOf(en) + specials, CoverLanguage.ENGLISH.filterVolumes(all, "ja"))
        assertEquals(listOf(pt) + specials, CoverLanguage.PORTUGUESE.filterVolumes(all, "ja"))
        assertEquals(specials, CoverLanguage.GERMAN.filterVolumes(all, "ja"))
        assertEquals(5, all.size)
        assertTrue(pt.owned)
        assertTrue(specials.last().owned)
    }

    @Test
    fun `Japanese specials do not block normal original language fallback`() {
        val ko = volume("ko", "ko")
        val en = volume("en", "en")
        val special = volume("s-ja", "ja", true)
        val all = listOf(ko, en, special)
        assertEquals(listOf(ko, special), CoverLanguage.JAPANESE.filterVolumes(all, "ko"))
        assertEquals(listOf(ko, special), CoverLanguage.ORIGINAL.filterVolumes(all, "KO"))
        assertEquals(listOf(special), CoverLanguage.FRENCH.filterVolumes(all, "ko"))
    }

    @Test
    fun `legacy data without original language remains usable`() {
        val all = listOf(volume("pt", "pt-br"), volume("special", "en", true))
        assertEquals(all, CoverLanguage.ORIGINAL.filterVolumes(all, null))
        assertEquals(all, CoverLanguage.JAPANESE.filterVolumes(all, "und"))
        assertEquals(listOf(all.last()), CoverLanguage.GERMAN.filterVolumes(all, null))
    }
}
