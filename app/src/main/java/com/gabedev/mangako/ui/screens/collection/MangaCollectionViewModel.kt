package com.gabedev.mangako.ui.screens.collection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MangaCollectionSortOption {
    TITLE_ASC,
    TITLE_DESC,
    PROGRESS_DESC,
    PROGRESS_ASC
}

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

    private val _sortOption = mutableStateOf(MangaCollectionSortOption.TITLE_ASC)
    val sortOption: State<MangaCollectionSortOption> = _sortOption

    private var mangaIdsWithSpecialEditions: Set<String> = emptySet()

    // Multi-select state
    var selectedIds = mutableStateOf(setOf<String>())
        private set
    var isMultiSelectActive = mutableStateOf(false)

    fun toggleSelection(id: String) {
        selectedIds.value =
            if (selectedIds.value.contains(id)) {
                selectedIds.value - id
            } else {
                selectedIds.value + id
            }
        isMultiSelectActive.value = selectedIds.value.isNotEmpty()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun finishMultiSelect() {
        isMultiSelectActive.value = false
        clearSelection()
    }

    fun selectAll(visibleIds: Set<String>) {
        selectedIds.value = visibleIds
    }

    fun removeSelectedFromLibrary() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                for (id in selectedIds.value) {
                    repository.removeMangaFromLibrary(id)
                }
            }
            // Remove from in-memory list
            _fullMangaCollection.value = _fullMangaCollection.value.filter {
                !selectedIds.value.contains(it.id)
            }
            applyFilters()
            finishMultiSelect()
        }
    }

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

    fun clearSearchQuery() {
        _searchQuery.value = ""
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

    fun setSortOption(option: MangaCollectionSortOption) {
        _sortOption.value = option
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

        _mangaCollection.value = filtered.sortedWith(sortComparator())
    }

    private fun sortComparator(): Comparator<MangaWithOwned> {
        val titleComparator = compareBy<MangaWithOwned> { it.title.lowercase() }
            .thenBy { it.title }
        return when (_sortOption.value) {
            MangaCollectionSortOption.TITLE_ASC -> titleComparator
            MangaCollectionSortOption.TITLE_DESC -> titleComparator.reversed()
            MangaCollectionSortOption.PROGRESS_DESC -> compareByDescending<MangaWithOwned> {
                it.completionProgress()
            }.then(titleComparator)
            MangaCollectionSortOption.PROGRESS_ASC -> compareBy<MangaWithOwned> {
                it.completionProgress()
            }.then(titleComparator)
        }
    }

    private fun MangaWithOwned.completionProgress(): Float {
        if (volumeCount <= 0) return 0f
        return volumeOwned.toFloat() / volumeCount.toFloat()
    }

    init {
        loadLibrary()
    }
}
