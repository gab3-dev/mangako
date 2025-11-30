package com.gabedev.mangako.ui.screens.detail

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MangaDetailViewModel(
    private val apiRepository: MangaDexRepository,
    private val localRepository: LibraryRepository,
    private var manga: Manga,
) : ViewModel() {
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
    private var currentOffset = 0
    private val limit = 50

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

    fun selectAllVolumes() {
        val allIds = volumeList.value.map { it.id }.toSet()
        selectedIds.value = allIds
    }

    private fun insertMangaOnLocalDatabase() {
        viewModelScope.launch {
            try {
                localRepository.getManga(manga.id) ?: run {
                    // If manga is not found in the local database, insert it
                    localRepository.insertManga(manga)
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
                isMangaInLibrary.value = localRepository.isMangaInLibrary(manga.id)
            } catch (e: Exception) {
                isMangaInLibrary.value = false
                localRepository.log(e)
            }
        }
    }

    private fun checkCoverInLibrary() {
        viewModelScope.launch {
            isVolumeLoading.value = true
            try {
                // Check if the manga and volumes is already in the local database
                val tmpData = localRepository.getMangaWithVolume(manga.id)
                if (tmpData == null || tmpData.volumes.isEmpty()) {
                    loadCoverList()
                } else {
                    volumeList.value = tmpData.volumes.map { it.copy() }
                    isVolumeLoading.value = false
                }
            } catch (_: Exception) {
                volumeList.value = emptyList()
                isVolumeLoading.value = false
            }
            isVolumeLoading.value = false
        }
    }

    fun refreshCoverList() {
        isVolumeLoading.value = true
        viewModelScope.launch {
            try {
                // Fetch cover list from the API
                val coverList: List<Volume> = apiRepository.getCoverListByManga(
                    manga = manga
                )
                // Insert the cover list into the local database
                localRepository.insertVolumeList(coverList)
                volumeList.value = coverList.map { it.copy() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isVolumeLoading.value = false
            }
        }
    }

    fun removeMangaFromLibrary() {
        viewModelScope.launch {
            try {
                localRepository.removeMangaFromLibrary(manga.id)
                _removeResult.value = Result.success(Unit)
                isMangaInLibrary.value = false
            } catch (e: Exception) {
                _removeResult.value = Result.failure(e)
            }
        }
    }

    private fun loadCoverList() {
        isVolumeLoading.value = true
        viewModelScope.launch {
            try {
                // Fetch cover list from the API
                val coverList: List<Volume> = apiRepository.getCoverListByManga(
                    manga = manga
                )
                // Insert the cover list into the local database
                localRepository.insertVolumeList(coverList)
                volumeList.value = coverList.map { it.copy() }
                isVolumeLoading.value = false
            } catch (e: Exception) {
                localRepository.log(e)
                isVolumeLoading.value = false
            }
        }
    }

    fun loadMoreVolumes() {
        if (isVolumeLoading.value) return
        viewModelScope.launch {
            currentOffset += limit
            isVolumeLoading.value = true

            val tmpMangaWithVolumes = localRepository.getMangaWithVolume(manga.id)
            if (tmpMangaWithVolumes != null && tmpMangaWithVolumes.volumes.isNotEmpty()) {
                // If local volumes are greater than the current offset, return
                if (tmpMangaWithVolumes.volumes.size > currentOffset) {
                    isVolumeLoading.value = false
                    try {
                        volumeList.value = tmpMangaWithVolumes.volumes.map {
                            it.copy()
                        }
                    } catch (e: Exception) {
                        localRepository.log(e)
                    }
                    currentOffset = tmpMangaWithVolumes.volumes.size
                    return@launch
                }
            }

            val moreVolumes = apiRepository.getCoverListByManga(
                manga = manga,
                offset = currentOffset,
            )
            // Insert the new volumes into the local database
            if (moreVolumes.isEmpty()) {
                isVolumeLoading.value = false
                noMoreVolume.value = true
                return@launch
            }
            localRepository.insertVolumeList(moreVolumes)
            try {
                volumeList.value += moreVolumes.map { it.copy() }
            } catch (e: Exception) {
                localRepository.log(e)
            }
            isVolumeLoading.value = false
        }
    }

    fun clearAddResult() {
        _addResult.value = null
    }

    fun clearRemoveResult() {
        _removeResult.value = null
    }

    init {
        insertMangaOnLocalDatabase()
        checkMangaInLibrary()
        checkCoverInLibrary()
    }
}
