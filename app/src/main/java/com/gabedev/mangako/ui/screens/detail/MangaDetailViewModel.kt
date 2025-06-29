package com.gabedev.mangako.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MangaDetailViewModel(
    private val repository: LibraryRepository,
    private val mangaId: String,
) : ViewModel() {
    private val _addResult = MutableStateFlow<Result<Unit>?>(null)
    private val _removeResult = MutableStateFlow<Result<Unit>?>(null)
    val addResult: StateFlow<Result<Unit>?> = _addResult
    val removeResult: StateFlow<Result<Unit>?> = _removeResult
    val isMangaInLibrary = MutableStateFlow<Boolean>(false)

    fun addManga(manga: Manga) {
        viewModelScope.launch {
            try {
                repository.addMangaToLibrary(manga)
                _addResult.value = Result.success(Unit)
                isMangaInLibrary.value = true
            } catch (e: Exception) {
                _addResult.value = Result.failure(e)
            }
        }
    }

    private fun checkMangaInLibrary() {
        viewModelScope.launch {
            try {
                isMangaInLibrary.value = repository.isMangaInLibrary(mangaId)
            } catch (e: Exception) {
                isMangaInLibrary.value = false
            }
        }
    }

    fun removeManga() {
        viewModelScope.launch {
            try {
                repository.removeMangaFromLibrary(mangaId)
                _removeResult.value = Result.success(Unit)
                isMangaInLibrary.value = false
            } catch (e: Exception) {
                _removeResult.value = Result.failure(e)
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
        checkMangaInLibrary()
    }
}