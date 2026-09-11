package com.gabedev.mangako.ui.screens.search_list

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MangaSearchCacheLocaleTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        MangaSearchCache.clear()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        MangaSearchCache.clear()
    }

    @Test
    fun `cached results are isolated by device locale`() {
        Locale.setDefault(Locale.US)
        MangaSearchCache.put("manga", 0, emptyList())
        assertEquals(emptyList<Any>(), MangaSearchCache.get("manga", 0))

        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        assertNull(MangaSearchCache.get("manga", 0))

        Locale.setDefault(Locale.US)
        assertEquals(emptyList<Any>(), MangaSearchCache.get("manga", 0))
    }

    @Test
    fun `invalidating a query removes all its locales`() {
        Locale.setDefault(Locale.US)
        MangaSearchCache.put("manga", 0, emptyList())
        Locale.setDefault(Locale.JAPAN)
        MangaSearchCache.put("manga", 0, emptyList())
        MangaSearchCache.invalidate("manga")
        assertNull(MangaSearchCache.get("manga", 0))
        Locale.setDefault(Locale.US)
        assertNull(MangaSearchCache.get("manga", 0))
    }
}
