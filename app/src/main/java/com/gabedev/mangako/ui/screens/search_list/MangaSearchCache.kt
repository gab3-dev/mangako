package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga

/**
 * Singleton cache for manga search results. Lives at process level
 * so it persists across navigation (ViewModel recreation).
 *
 * Keyed by "query:offset" → list of results.
 */
object MangaSearchCache {
    private val cache = HashMap<String, List<Manga>>()

    fun get(query: String, offset: Int): List<Manga>? = cache["$query:$offset"]

    fun put(query: String, offset: Int, results: List<Manga>) {
        cache["$query:$offset"] = results
    }

    fun updateItem(query: String, offset: Int, manga: Manga) {
        val key = "$query:$offset"
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
