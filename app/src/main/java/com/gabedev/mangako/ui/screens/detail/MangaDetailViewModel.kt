package com.gabedev.mangako.ui.screens.detail

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.local.CoverLanguage
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MangaDetailViewModel(
    private val apiRepository: MangaDexRepository,
    private val localRepository: LibraryRepository,
    private var manga: Manga,
    autoLoad: Boolean = true
) : ViewModel() {
    val mangaState = MutableStateFlow(manga)
    private val idManga = mangaState.value.id
    private val _addResult = MutableStateFlow<Result<Unit>?>(null)
    private val _removeResult = MutableStateFlow<Result<Unit>?>(null)
    var selectedIds = mutableStateOf(setOf<String>())
        private set
    val isVolumeLoading = MutableStateFlow(false)
    val addResult: StateFlow<Result<Unit>?> = _addResult
    val removeResult: StateFlow<Result<Unit>?> = _removeResult
    val isMangaInLibrary = MutableStateFlow(false)
    var isMultiSelectActive = mutableStateOf(false)
    val volumeList = MutableStateFlow<List<Volume>>(emptyList())
    val noMoreVolume = MutableStateFlow(false)
    val paginationPaused = MutableStateFlow(false)
    val nextVolumeOffset = MutableStateFlow(0)
    private var pagesWithoutProgress = 0
    private var volumeLoadJob: Job? = null
    private var activeCoverLanguage: CoverLanguage? = null
    private val limit = 50

    /**
     * Deduplicates volumes by volume number and locale, keeping the most recently updated entry.
     * Volumes with null volume numbers (e.g., specials/unnumbered covers) are preserved as-is.
     * @param volumes List of volumes that may contain duplicates
     * @return Deduplicated list with one volume per volume number, plus all null-volume entries
     */
    private fun deduplicateVolumes(volumes: List<Volume>): List<Volume> {
        val (numbered, unnumbered) = volumes.partition { it.volume != null }

        val deduplicatedNumbered = numbered
            .groupBy { it.volume to it.locale }
            .mapValues { (_, vols) ->
                vols.maxByOrNull { it.updatedAt ?: "" } ?: vols.first()
            }
            .values
            .toList()

        return deduplicatedNumbered + unnumbered.distinctBy { it.id }
    }

    fun markSelectedListAsOwned(isOwned: Boolean) {
        viewModelScope.launch {
            val updatedVolumes = volumeList.value.map { volume ->
                if (selectedIds.value.contains(volume.id)) {
                    volume.copy(owned = isOwned)
                } else {
                    volume
                }
            }
            // Update the local database
            localRepository.updateVolumeList(
                updatedVolumes.filter { selectedIds.value.contains(it.id) }
            )
            // Update the volume list in the current state
            volumeList.value = updatedVolumes
            // Clear selection
            clearSelection()
        }
    }

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

    fun selectAllVolumes(visibleIds: Set<String>) {
        selectedIds.value = visibleIds
    }

    private fun insertMangaOnLocalDatabase() {
        viewModelScope.launch {
            try {
                localRepository.getManga(idManga) ?: run {
                    // If manga is not found in the local database, insert it
                    localRepository.insertManga(mangaState.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addMangaToLibrary(manga: Manga) {
        viewModelScope.launch {
            try {
                val tmpManga = manga.copy(
                    isOnUserLibrary = true
                )
                localRepository.addMangaToLibrary(tmpManga)
                _addResult.value = Result.success(Unit)
                this@MangaDetailViewModel.manga = tmpManga
                isMangaInLibrary.value = true
            } catch (e: Exception) {
                _addResult.value = Result.failure(e)
                localRepository.log(e)
            }
        }
    }

    fun toggleVolumeOwned(volume: Volume) {
        viewModelScope.launch {
            try {
                val updatedVolume = volume.copy(owned = volume.owned.not())
                localRepository.updateVolume(updatedVolume)
                // Update the volume list in the current state
                val updatedList = volumeList.value.toMutableList()
                val index = updatedList.indexOfFirst { it.id == volume.id }
                if (index != -1) {
                    updatedList[index] = updatedVolume
                    volumeList.value = updatedList
                }
            } catch (e: Exception) {
                localRepository.log(e)
            }
        }
    }

    private fun checkMangaInLibrary() {
        viewModelScope.launch {
            try {
                isMangaInLibrary.value = localRepository.isMangaInLibrary(idManga)
            } catch (e: Exception) {
                isMangaInLibrary.value = false
                localRepository.log(e)
            }
        }
    }

    private fun checkCoverInLibrary() {
        loadVolumes(loadCached = true)
    }

    fun refreshManga() {
        loadVolumes(refresh = true)
    }

    fun setCoverLanguage(language: CoverLanguage) {
        if (activeCoverLanguage == language) return
        val previous = activeCoverLanguage
        activeCoverLanguage = language
        if (previous != null) {
            finishMultiSelect()
            refreshManga()
        }
    }

    fun removeMangaFromLibrary() {
        viewModelScope.launch {
            try {
                localRepository.removeMangaFromLibrary(idManga)
                _removeResult.value = Result.success(Unit)
                isMangaInLibrary.value = false
            } catch (e: Exception) {
                _removeResult.value = Result.failure(e)
            }
        }
    }

    fun loadMoreVolumes() {
        loadVolumes()
    }

    fun retryVolumes() {
        if (isVolumeLoading.value || !paginationPaused.value || nextVolumeOffset.value >= 10_000) return
        paginationPaused.value = false
        pagesWithoutProgress = 0
        loadVolumes()
    }

    private fun loadVolumes(refresh: Boolean = false, loadCached: Boolean = false) {
        if (!refresh && (isVolumeLoading.value || noMoreVolume.value || paginationPaused.value)) return
        val previousJob = volumeLoadJob
        if (refresh) previousJob?.cancel()
        // Set before launching so simultaneous scroll events cannot queue duplicate requests.
        isVolumeLoading.value = true
        volumeLoadJob = viewModelScope.launch {
            try {
                if (refresh) {
                    previousJob?.join()
                    nextVolumeOffset.value = 0
                    pagesWithoutProgress = 0
                    noMoreVolume.value = false
                    paginationPaused.value = false
                    val updated = apiRepository.getManga(idManga, refresh = true)
                    currentCoroutineContext().ensureActive()
                    localRepository.updateManga(updated)?.let { mangaState.value = it }
                }
                if (loadCached) {
                    volumeList.value = localRepository.getMangaWithVolume(idManga)?.volumes.orEmpty()
                }
                if (nextVolumeOffset.value >= 10_000) {
                    paginationPaused.value = true
                    return@launch
                }
                val page = apiRepository.getCoverListByManga(
                    manga = mangaState.value,
                    offset = nextVolumeOffset.value,
                    limit = minOf(limit, 10_000 - nextVolumeOffset.value),
                    refresh = refresh,
                )
                currentCoroutineContext().ensureActive()
                if (page.isEmpty()) {
                    noMoreVolume.value = true
                    return@launch
                }

                val before = volumeList.value.associateBy { it.id }
                localRepository.updateOrInsertVolumeList(deduplicateVolumes(page))
                currentCoroutineContext().ensureActive()
                val stored = localRepository.getMangaWithVolume(idManga)?.volumes
                    ?: deduplicateVolumes(volumeList.value + page)
                currentCoroutineContext().ensureActive()
                volumeList.value = stored
                pagesWithoutProgress = if (stored.associateBy { it.id } == before) pagesWithoutProgress + 1 else 0
                // Remote rows consumed, not the number of distinct volumes kept locally.
                nextVolumeOffset.value += page.size
                paginationPaused.value = pagesWithoutProgress >= 2 || nextVolumeOffset.value >= 10_000
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                paginationPaused.value = true
                localRepository.log(e)
            } finally {
                // A cancelled request must not clear the loading flag of its replacement.
                if (currentCoroutineContext().isActive) isVolumeLoading.value = false
            }
        }
    }

    fun clearAddResult() {
        _addResult.value = null
    }

    fun clearRemoveResult() {
        _removeResult.value = null
    }

    init {
        if (autoLoad) {
            insertMangaOnLocalDatabase()
            checkMangaInLibrary()
            checkCoverInLibrary()
        }
    }
}
