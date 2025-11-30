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
            print("Carregando mais mangas...\n")
            isLoadingMoreManga.value = true
        } else {
            print("Carregando lista de mangas...\n")
            isMangaLoading.value = true
        }

        activeQuery = currentQuery

        loadMangaJob = viewModelScope.launch {
            try {
                // Fetch manga list from the API
                val resultMangaList: List<Manga> = apiRepository.searchManga(
                    queryString.value,
                    currentOffset
                )

                // If the query as changed, ignore the results
                if (activeQuery != currentQuery || !isActive) return@launch

                if (resultMangaList.isEmpty()) {
                    noMoreManga.value = true
                    isMangaLoading.value = false
                    isLoadingMoreManga.value = false
                    return@launch
                }

                if (currentOffset != 0) {
                    mangaList.value += resultMangaList.map { it.copy() }
                } else {
                    mangaList.value = resultMangaList.map { it.copy() }
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
        queryString.value = newQuery
        mangaList.value = emptyList()
        currentOffset = 0
        loadMangaList()
    }
}
