package com.gabedev.mangako.ui.screens.detail

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
    val isVolumeLoading = MutableStateFlow(false)
    val addResult: StateFlow<Result<Unit>?> = _addResult
    val removeResult: StateFlow<Result<Unit>?> = _removeResult
    val isMangaInLibrary = MutableStateFlow(false)
    val volumeList = MutableStateFlow<List<Volume>>(emptyList())
    private var currentOffset = 0
    private val limit = 50

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
            }
        }
    }

    fun markVolumeAsOwned(volume: Volume) {
        viewModelScope.launch {
            try {
                val updatedVolume = volume.copy(owned = true)
                localRepository.updateVolume(updatedVolume)
                // Update the volume list in the current state
                val updatedList = volumeList.value.toMutableList()
                val index = updatedList.indexOfFirst { it.id == volume.id }
                if (index != -1) {
                    updatedList[index] = updatedVolume
                    volumeList.value = updatedList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkMangaInLibrary() {
        viewModelScope.launch {
            try {
                isMangaInLibrary.value = localRepository.isMangaInLibrary(manga.id)
            } catch (e: Exception) {
                isMangaInLibrary.value = false
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
                    volumeList.value = tmpData.volumes
                    isVolumeLoading.value = false
                }
            } catch (e: Exception) {
                volumeList.value = emptyList()
                isVolumeLoading.value = false
            }
            isVolumeLoading.value = false
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
                volumeList.value = coverList
                isVolumeLoading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                isVolumeLoading.value = false
            }
        }
    }

    fun loadMoreVolumes() {
        if (isVolumeLoading.value) return
        viewModelScope.launch {
            isVolumeLoading.value = true
            currentOffset += limit
            val moreVolumes = apiRepository.getCoverListByManga(
                manga = manga,
                offset = currentOffset,
            )
            volumeList.value += moreVolumes
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