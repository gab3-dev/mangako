package com.gabedev.mangako.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository

class MangaDetailViewModelFactory(
    private val apiRepository: MangaDexRepository,
    private val localRepository: LibraryRepository,
    private val manga: Manga
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaDetailViewModel(apiRepository, localRepository, manga) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}