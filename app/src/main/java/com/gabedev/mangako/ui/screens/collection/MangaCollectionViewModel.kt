package com.gabedev.mangako.ui.screens.collection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.CoroutineDispatcher

class MangaCollectionViewModel (
        private val repository: LibraryRepository,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): ViewModel() {
    private val _mangaCollection = mutableStateOf<List<MangaWithOwned>>(emptyList())
    private val _fullMangaCollection = mutableStateOf<List<MangaWithOwned>>(emptyList())
    val mangaCollection: State<List<MangaWithOwned>> = _mangaCollection

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _showIncompleteOnly = mutableStateOf(false)
    val showIncompleteOnly: State<Boolean> = _showIncompleteOnly

    private val _showSpecialEditionsOnly = mutableStateOf(false)
    val showSpecialEditionsOnly: State<Boolean> = _showSpecialEditionsOnly

    private var mangaIdsWithSpecialEditions: Set<String> = emptySet()

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(ioDispatcher) {
                repository.getMangaOnLibrary()
            }
            _fullMangaCollection.value = result

            // Load manga IDs with special editions
            mangaIdsWithSpecialEditions = withContext(ioDispatcher) {
                repository.getMangaIdsWithSpecialEditions().toSet()
            }

            applyFilters()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun toggleIncompleteFilter() {
        _showIncompleteOnly.value = !_showIncompleteOnly.value
        applyFilters()
    }

    fun toggleSpecialEditionsFilter() {
        _showSpecialEditionsOnly.value = !_showSpecialEditionsOnly.value
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _fullMangaCollection.value

        // Apply search filter
        if (_searchQuery.value.isNotBlank()) {
            filtered = filtered.filter { manga ->
                manga.title.contains(_searchQuery.value, ignoreCase = true) ||
                manga.altTitle?.contains(_searchQuery.value, ignoreCase = true) == true
            }
        }

        // Apply incomplete filter (show only manga with unacquired volumes)
        if (_showIncompleteOnly.value) {
            filtered = filtered.filter { manga ->
                manga.volumeOwned < manga.volumeCount
            }
        }

        // Apply special editions filter
        if (_showSpecialEditionsOnly.value) {
            filtered = filtered.filter { manga ->
                mangaIdsWithSpecialEditions.contains(manga.id)
            }
        }

        _mangaCollection.value = filtered
    }

    init {
        loadLibrary()
    }
}