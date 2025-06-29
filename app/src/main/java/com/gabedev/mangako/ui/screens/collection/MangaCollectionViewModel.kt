package com.gabedev.mangako.ui.screens.collection

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MangaCollectionViewModel (
        private val repository: LibraryRepository
): ViewModel() {
    private val _mangaCollection = mutableStateOf<List<Manga>>(emptyList())
    val mangaCollection: State<List<Manga>> = _mangaCollection

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                repository.getAllManga()
            }
            _mangaCollection.value = result
            _isLoading.value = false
        }
    }

    init {
        loadLibrary()
    }
}