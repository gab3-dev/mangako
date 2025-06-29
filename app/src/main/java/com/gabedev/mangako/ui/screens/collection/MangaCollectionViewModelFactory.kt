package com.gabedev.mangako.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gabedev.mangako.data.repository.LibraryRepository

class MangaCollectionViewModelFactory(
    private val repository: LibraryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaCollectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaCollectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}