package com.gabedev.mangako.ui.screens.collection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.local.CoverLanguage
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.model.Volume
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

data class MangaVolumeGroup(
    val manga: Manga,
    val volumes: List<Volume>,
)

internal fun localizeVolumeGroups(
    groups: List<MangaVolumeGroup>,
    globalLanguage: CoverLanguage,
): List<MangaVolumeGroup> = groups.mapNotNull { group ->
    val language = group.manga.coverLanguage
        ?.let(CoverLanguage::fromStored)
        ?: globalLanguage
    group.copy(volumes = language.filterVolumes(group.volumes, group.manga.originalLanguage))
        .takeIf { it.volumes.isNotEmpty() }
}

class MangaCollectionViewModel (
        private val repository: LibraryRepository,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): ViewModel() {
    private val _mangaCollection = mutableStateOf<List<MangaWithOwned>>(emptyList())
    private val _fullMangaCollection = mutableStateOf<List<MangaWithOwned>>(emptyList())
    val mangaCollection: State<List<MangaWithOwned>> = _mangaCollection
    private val _volumeGroups = mutableStateOf<List<MangaVolumeGroup>>(emptyList())
    private val _fullVolumeGroups = mutableStateOf<List<MangaVolumeGroup>>(emptyList())
    val volumeGroups: State<List<MangaVolumeGroup>> = _volumeGroups

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _showIncompleteOnly = mutableStateOf(false)
    val showIncompleteOnly: State<Boolean> = _showIncompleteOnly

    private val _showSpecialEditionsOnly = mutableStateOf(false)
    val showSpecialEditionsOnly: State<Boolean> = _showSpecialEditionsOnly
    private val _showUnownedVolumesOnly = mutableStateOf(false)
    val showUnownedVolumesOnly: State<Boolean> = _showUnownedVolumesOnly

    private val _sortOption = mutableStateOf(MangaCollectionSortOption.TITLE_ASC)
    val sortOption: State<MangaCollectionSortOption> = _sortOption

    private var mangaIdsWithSpecialEditions: Set<String> = emptySet()

    // Multi-select state
    var selectedIds = mutableStateOf(setOf<String>())
        private set
    var isMultiSelectActive = mutableStateOf(false)

    var selectedVolumeIds = mutableStateOf(setOf<String>())
        private set
    var isVolumeMultiSelectActive = mutableStateOf(false)
        private set

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
            _fullVolumeGroups.value = _fullVolumeGroups.value.filter {
                !selectedIds.value.contains(it.manga.id)
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
            val volumeResult = withContext(ioDispatcher) {
                repository.getLibraryMangaWithVolumes()
            }
            _fullMangaCollection.value = result
            _fullVolumeGroups.value = volumeResult.map { mangaWithVolumes ->
                MangaVolumeGroup(
                    manga = mangaWithVolumes.manga,
                    volumes = mangaWithVolumes.volumes.sortedWith(volumeComparator()),
                )
            }

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

    fun toggleUnownedVolumesFilter() {
        _showUnownedVolumesOnly.value = !_showUnownedVolumesOnly.value
        applyVolumeFilters()
    }

    fun toggleVolumeOwned(volume: Volume) {
        viewModelScope.launch {
            val updated = volume.copy(owned = !volume.owned)
            withContext(ioDispatcher) { repository.updateVolume(updated) }
            updateVolumeInGroups(updated)
        }
    }

    fun toggleVolumeSelection(id: String) {
        selectedVolumeIds.value = if (selectedVolumeIds.value.contains(id)) {
            selectedVolumeIds.value - id
        } else {
            selectedVolumeIds.value + id
        }
        isVolumeMultiSelectActive.value = selectedVolumeIds.value.isNotEmpty()
    }

    fun selectAllVolumes(visibleIds: Set<String>) {
        selectedVolumeIds.value = visibleIds
        isVolumeMultiSelectActive.value = visibleIds.isNotEmpty()
    }

    fun clearVolumeSelection() {
        selectedVolumeIds.value = emptySet()
    }

    fun finishVolumeMultiSelect() {
        isVolumeMultiSelectActive.value = false
        clearVolumeSelection()
    }

    fun markSelectedVolumesAsOwned(isOwned: Boolean) {
        val selected = selectedVolumeIds.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val updates = _fullVolumeGroups.value.flatMap { it.volumes }
                .filter { it.id in selected }
                .map { it.copy(owned = isOwned) }
            withContext(ioDispatcher) { repository.updateVolumeList(updates) }
            updates.forEach(::updateVolumeInGroups)
            clearVolumeSelection()
        }
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
        applyVolumeFilters()
    }

    private fun applyVolumeFilters() {
        var groups = _fullVolumeGroups.value
        if (_searchQuery.value.isNotBlank()) {
            groups = groups.filter { group ->
                group.manga.title.contains(_searchQuery.value, ignoreCase = true) ||
                    group.manga.altTitle?.contains(_searchQuery.value, ignoreCase = true) == true
            }
        }
        if (_showUnownedVolumesOnly.value) {
            groups = groups.map { group -> group.copy(volumes = group.volumes.filterNot { it.owned }) }
                .filter { it.volumes.isNotEmpty() }
        }
        if (!_showSpecialEditionsOnly.value) {
            groups = groups.map { group ->
                group.copy(volumes = group.volumes.filterNot { it.isSpecialEdition })
            }.filter { it.volumes.isNotEmpty() }
        }
        _volumeGroups.value = groups.sortedBy { it.manga.title.lowercase() }
    }

    private fun updateVolumeInGroups(updated: Volume) {
        _fullVolumeGroups.value = _fullVolumeGroups.value.map { group ->
            if (group.manga.id == updated.mangaId) {
                group.copy(volumes = group.volumes.map { if (it.id == updated.id) updated else it })
            } else {
                group
            }
        }
        val ownedCounts = _fullVolumeGroups.value.associate { group ->
            group.manga.id to group.volumes.count { it.owned }
        }
        _fullMangaCollection.value = _fullMangaCollection.value.map { manga ->
            manga.copy(volumeOwned = ownedCounts[manga.id] ?: manga.volumeOwned)
        }
        applyFilters()
    }

    private fun volumeComparator(): Comparator<Volume> = compareBy<Volume> { it.isSpecialEdition }
        .thenBy { it.volume ?: Float.MAX_VALUE }
        .thenBy { it.locale }
        .thenBy { it.id }

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
