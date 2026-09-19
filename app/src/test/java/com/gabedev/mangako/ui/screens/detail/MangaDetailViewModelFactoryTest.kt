package com.gabedev.mangako.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.mockk
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class MangaDetailViewModelFactoryTest {
    private var viewModel: MangaDetailViewModel? = null

    @After
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
    }

    @Test
    fun `create returns a manga detail view model`() {
        val factory = MangaDetailViewModelFactory(
            apiRepository = mockk<MangaDexRepository>(relaxed = true),
            localRepository = mockk<LibraryRepository>(relaxed = true),
            manga = Manga("manga", "Manga", coverUrl = "cover", description = "description"),
        )

        viewModel = factory.create(MangaDetailViewModel::class.java)

        assertTrue(viewModel is MangaDetailViewModel)
    }

    @Test
    fun `create rejects an unknown view model type`() {
        val factory = MangaDetailViewModelFactory(
            apiRepository = mockk<MangaDexRepository>(relaxed = true),
            localRepository = mockk<LibraryRepository>(relaxed = true),
            manga = Manga("manga", "Manga", coverUrl = "cover", description = "description"),
        )

        assertFailsWith<IllegalArgumentException> {
            factory.create(UnknownViewModel::class.java)
        }
    }

    private class UnknownViewModel : ViewModel()
}
