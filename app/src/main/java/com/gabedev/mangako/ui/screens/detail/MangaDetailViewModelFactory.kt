package com.gabedev.mangako.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gabedev.mangako.data.repository.LibraryRepository

class MangaDetailViewModelFactory(
    private val repository: LibraryRepository,
    private val mangaId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaDetailViewModel(repository, mangaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}