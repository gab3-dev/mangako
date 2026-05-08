package com.gabedev.mangako.ui.screens.search_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MangaSearchListViewModel(
    private val apiRepository: MangaDexRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val queryString = MutableStateFlow("")
    val isMangaLoading = MutableStateFlow(false)
    val isLoadingMoreManga = MutableStateFlow(false)
    val mangaList = MutableStateFlow<List<Manga>>(emptyList())
    val noMoreManga = MutableStateFlow(false)
    private var loadMangaJob: Job? = null
    private val enrichmentJobs = mutableSetOf<Job>()
    private var activeQuery: String? = null
    private var activeSearchGeneration = 0
    private var currentOffset = 0
    private val limit = 6


    fun refreshMangaList() {
        val query = queryString.value.trim()

        cancelActiveJobs()
        activeSearchGeneration += 1
        activeQuery = query
        currentOffset = 0
        mangaList.value = emptyList()
        noMoreManga.value = false
        MangaSearchCache.invalidate(query)

        loadMangaList()
    }

    fun loadMangaList() {
        val currentQuery = queryString.value.trim()
        if (noMoreManga.value) return
        if (isMangaLoading.value || isLoadingMoreManga.value) return

        val pageOffset = currentOffset
        val generation = activeSearchGeneration
        val isLoadingMore = pageOffset != 0

        if (isLoadingMore) {
            isLoadingMoreManga.value = true
        } else {
            isMangaLoading.value = true
        }

        activeQuery = currentQuery

        val cached = MangaSearchCache.get(currentQuery, pageOffset)

        if (cached != null) {
            applyPage(pageOffset, cached)
            currentOffset += limit
            noMoreManga.value = cached.size < limit
            isMangaLoading.value = false
            isLoadingMoreManga.value = false
            launchEnrichment(currentQuery, pageOffset, cached, generation)
            return
        }

        loadMangaJob = viewModelScope.launch(dispatcher) {
            try {
                val resultMangaList: List<Manga> = apiRepository.searchMangaPage(
                    currentQuery,
                    pageOffset,
                    limit
                )

                if (!isSearchCurrent(currentQuery, generation) || !isActive) return@launch

                if (resultMangaList.isEmpty()) {
                    noMoreManga.value = true
                    isMangaLoading.value = false
                    isLoadingMoreManga.value = false
                    return@launch
                }

                val results = resultMangaList
                    .distinctBy { it.id }
                    .map { it.copy() }
                MangaSearchCache.put(currentQuery, pageOffset, results)

                applyPage(pageOffset, results)
                currentOffset += limit
                noMoreManga.value = resultMangaList.size < limit
                isMangaLoading.value = false
                isLoadingMoreManga.value = false
                launchEnrichment(currentQuery, pageOffset, results, generation)
            } catch (_: CancellationException) {
                apiRepository.log(Exception("Carregamento cancelado."))
                return@launch
            } catch (e: Exception) {
                apiRepository.log(e)
                isMangaLoading.value = false
                isLoadingMoreManga.value = false
            }
        }
    }

    fun setQueryString(newQuery: String) {
        if (newQuery == queryString.value) {
            if (mangaList.value.isEmpty()) loadMangaList()
            return
        }

        cancelActiveJobs()
        activeSearchGeneration += 1
        queryString.value = newQuery
        mangaList.value = emptyList()
        currentOffset = 0
        noMoreManga.value = false
        loadMangaList()
    }

    private fun applyPage(offset: Int, results: List<Manga>) {
        val uniqueResults = results.distinctBy { it.id }
        if (offset != 0) {
            val existingIds = mangaList.value.map { it.id }.toSet()
            mangaList.value += uniqueResults.filterNot { it.id in existingIds }
        } else {
            mangaList.value = uniqueResults
        }
    }

    private fun launchEnrichment(
        query: String,
        offset: Int,
        results: List<Manga>,
        generation: Int
    ) {
        results.forEach { manga ->
            val job = viewModelScope.launch(dispatcher) {
                try {
                    val enriched = apiRepository.enrichManga(manga)
                    if (!isSearchCurrent(query, generation) || !isActive) return@launch

                    MangaSearchCache.updateItem(query, offset, enriched)
                    mangaList.value = mangaList.value.map { current ->
                        if (current.id == enriched.id) enriched else current
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    apiRepository.log(e)
                }
            }
            enrichmentJobs += job
            job.invokeOnCompletion {
                enrichmentJobs.remove(job)
            }
        }
    }

    private fun isSearchCurrent(query: String, generation: Int): Boolean {
        return activeQuery == query && activeSearchGeneration == generation
    }

    private fun cancelActiveJobs() {
        loadMangaJob?.cancel()
        enrichmentJobs.toList().forEach { it.cancel() }
        enrichmentJobs.clear()
        isMangaLoading.value = false
        isLoadingMoreManga.value = false
    }
}
