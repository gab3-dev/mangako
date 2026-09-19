package com.gabedev.mangako.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.repository.LibraryRepository
import io.mockk.mockk
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class MangaCollectionViewModelFactoryTest {
    private var viewModel: MangaCollectionViewModel? = null

    @After
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
    }

    @Test
    fun `create returns a manga collection view model`() {
        val factory = MangaCollectionViewModelFactory(mockk<LibraryRepository>(relaxed = true))

        viewModel = factory.create(MangaCollectionViewModel::class.java)

        assertTrue(viewModel is MangaCollectionViewModel)
    }

    @Test
    fun `create rejects an unknown view model type`() {
        val factory = MangaCollectionViewModelFactory(mockk<LibraryRepository>(relaxed = true))

        assertFailsWith<IllegalArgumentException> {
            factory.create(UnknownViewModel::class.java)
        }
    }

    private class UnknownViewModel : ViewModel()
}
