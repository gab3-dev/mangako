package com.gabedev.mangako.ui.screens.search_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gabedev.mangako.data.repository.MangaDexRepository

class MangaSearchListViewModelFactory(
    private val apiRepository: MangaDexRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaSearchListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaSearchListViewModel(apiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}