package com.gabedev.mangako.ui.screens.collection

import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.repository.LibraryRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangaCollectionViewModelTest {

    private lateinit var repository: LibraryRepository
    private val testDispatcher = StandardTestDispatcher()
    private var viewModel: MangaCollectionViewModel? = null

    private fun createMangaWithOwned(
        id: String,
        title: String,
        volumeOwned: Int = 0
    ) = MangaWithOwned(
        id = id, title = title, altTitle = null, type = null,
        coverId = null, coverFileName = null, coverUrl = "url",
        authorId = null, author = null, description = "desc",
        status = null, volumeCount = 10, isOnUserLibrary = true,
        volumeOwned = volumeOwned
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        // Cancel the viewModel's coroutine scope before resetting Main
        // to prevent IO continuations from crashing on a reset Main dispatcher
        viewModel?.viewModelScope?.cancel()
        viewModel = null
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadLibrary calls getMangaOnLibrary from repository`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository)
        advanceUntilIdle()

        coVerify { repository.getMangaOnLibrary() }
    }

    @Test
    fun `mangaCollection initial state is empty list`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository)
        // Before advancing, the collection should be empty (default)
        assertTrue(viewModel!!.mangaCollection.value.isEmpty())
    }

    @Test
    fun `isLoading is true while loadLibrary is in progress`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository)
        // After advancing, loadLibrary has started and set isLoading=true,
        // but withContext(Dispatchers.IO) runs on a real thread so isLoading
        // hasn't been reset to false yet
        advanceUntilIdle()
        assertTrue(viewModel!!.isLoading.value)
    }

    @Test
    fun `loadLibrary can be called manually`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository)
        advanceUntilIdle()

        viewModel!!.loadLibrary()
        advanceUntilIdle()

        // Called twice: once in init, once manually
        coVerify(atLeast = 2) { repository.getMangaOnLibrary() }
    }
}
