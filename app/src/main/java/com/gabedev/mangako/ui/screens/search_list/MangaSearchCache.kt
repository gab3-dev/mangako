package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga
import java.util.Locale

/**
 * Singleton cache for manga search results. Lives at process level
 * so it persists across navigation (ViewModel recreation).
 *
 * Keyed by query, offset, app locale and cover language because catalog metadata is localized.
 */
object MangaSearchCache {
    private val cache = HashMap<String, List<Manga>>()

    private fun key(query: String, offset: Int, localeTag: String, coverLanguageTag: String) =
        "$query:$offset:$localeTag:$coverLanguageTag"

    fun get(query: String, offset: Int, localeTag: String = Locale.getDefault().toLanguageTag(), coverLanguageTag: String = "ja"): List<Manga>? =
        cache[key(query, offset, localeTag, coverLanguageTag)]

    fun put(query: String, offset: Int, results: List<Manga>, localeTag: String = Locale.getDefault().toLanguageTag(), coverLanguageTag: String = "ja") {
        cache[key(query, offset, localeTag, coverLanguageTag)] = results
    }

    fun updateItem(query: String, offset: Int, manga: Manga, localeTag: String = Locale.getDefault().toLanguageTag(), coverLanguageTag: String = "ja") {
        val key = key(query, offset, localeTag, coverLanguageTag)
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
