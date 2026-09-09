package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga
import java.util.Locale

/**
 * Singleton cache for manga search results. Lives at process level
 * so it persists across navigation (ViewModel recreation).
 *
 * Keyed by query, offset and device locale because descriptions are already localized.
 */
object MangaSearchCache {
    private val cache = HashMap<String, List<Manga>>()

    private fun key(query: String, offset: Int) = "$query:$offset:${Locale.getDefault().toLanguageTag()}"

    fun get(query: String, offset: Int): List<Manga>? = cache[key(query, offset)]

    fun put(query: String, offset: Int, results: List<Manga>) {
        cache[key(query, offset)] = results
    }

    fun updateItem(query: String, offset: Int, manga: Manga) {
        val key = key(query, offset)
        cache[key] = cache[key]?.map {
            if (it.id == manga.id) manga else it
        } ?: return
    }

    fun invalidate(query: String) {
        cache.keys.removeAll { it.startsWith("$query:") }
    }

    fun clear() {
        cache.clear()
    }
}
