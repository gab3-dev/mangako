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
    val mangaCollection: State<List<MangaWithOwned>> = _mangaCollection

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(ioDispatcher) {
                repository.getMangaOnLibrary()
            }
            _mangaCollection.value = result
            _isLoading.value = false
        }
    }

    init {
        loadLibrary()
    }
}