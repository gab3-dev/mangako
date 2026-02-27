package com.gabedev.mangako.ui.screens.search_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MangaSearchListViewModel(
    private val apiRepository: MangaDexRepository,
) : ViewModel() {
    val queryString = MutableStateFlow("")
    val isMangaLoading = MutableStateFlow(false)
    val isLoadingMoreManga = MutableStateFlow(false)
    val mangaList = MutableStateFlow<List<Manga>>(emptyList())
    val noMoreManga = MutableStateFlow(false)
    private var loadMangaJob: Job? = null
    private var activeQuery: String? = null
    private var currentOffset = 0
    private val limit = 6


    fun refreshMangaList() {
        // Clear cache for this query so we force a fresh fetch
        val query = queryString.value.trim()
        MangaSearchCache.invalidate(query)

        isMangaLoading.value = true
        viewModelScope.launch {
            val tmpOffset = currentOffset
            try {
                currentOffset = 0
                // Fetch cover list from the API
                val coverList: List<Manga> = apiRepository.searchManga(
                    queryString.value,
                    currentOffset
                )
                mangaList.value = coverList.map { it.copy() }
                MangaSearchCache.put(query, currentOffset, mangaList.value)
                currentOffset += limit
            } catch (e: Exception) {
                currentOffset = tmpOffset
                e.printStackTrace()
            } finally {
                isMangaLoading.value = false
            }
        }
    }

    fun loadMangaList() {
        val currentQuery = queryString.value.trim()

        // Cancel last execution
        loadMangaJob?.cancel()
        noMoreManga.value = false
        if (currentOffset != 0) {
            isLoadingMoreManga.value = true
        } else {
            isMangaLoading.value = true
        }

        activeQuery = currentQuery

        val cached = MangaSearchCache.get(currentQuery, currentOffset)

        if (cached != null) {
            // Serve from cache — skip API call
            if (currentOffset != 0) {
                mangaList.value += cached
            } else {
                mangaList.value = cached
            }
            currentOffset += limit
            isMangaLoading.value = false
            isLoadingMoreManga.value = false
            return
        }

        loadMangaJob = viewModelScope.launch {
            try {
                // Fetch manga list from the API
                val resultMangaList: List<Manga> = apiRepository.searchManga(
                    queryString.value,
                    currentOffset
                )

                // If the query has changed, ignore the results
                if (activeQuery != currentQuery || !isActive) return@launch

                if (resultMangaList.isEmpty()) {
                    noMoreManga.value = true
                    isMangaLoading.value = false
                    isLoadingMoreManga.value = false
                    return@launch
                }

                val results = resultMangaList.map { it.copy() }
                MangaSearchCache.put(currentQuery, currentOffset, results)

                if (currentOffset != 0) {
                    mangaList.value += results
                } else {
                    mangaList.value = results
                }
                currentOffset += limit
                isMangaLoading.value = false
                isLoadingMoreManga.value = false
            } catch (_: CancellationException) {
                // Cancellation is expected, don't treat as error
                apiRepository.log(Exception("Carregamento cancelado."))
                mangaList.value = emptyList()
                currentOffset = 0
                return@launch
            } catch (e: Exception) {
                apiRepository.log(e)
            }
        }
    }

    fun setQueryString(newQuery: String) {
        // Skip if the query hasn't changed
        if (newQuery == queryString.value) return

        queryString.value = newQuery
        mangaList.value = emptyList()
        currentOffset = 0
        loadMangaList()
    }
}
