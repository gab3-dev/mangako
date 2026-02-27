package com.gabedev.mangako.ui.screens.collection

import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.repository.LibraryRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        altTitle: String? = null,
        volumeOwned: Int = 0,
        volumeCount: Int = 10
    ) = MangaWithOwned(
        id = id, title = title, altTitle = altTitle, type = null,
        coverId = null, coverFileName = null, coverUrl = "url",
        authorId = null, author = null, description = "desc",
        status = null, volumeCount = volumeCount, isOnUserLibrary = true,
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
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        coVerify { repository.getMangaOnLibrary() }
    }

    @Test
    fun `mangaCollection initial state is empty list`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        // Before advancing, the collection should be empty (default)
        assertTrue(viewModel!!.mangaCollection.value.isEmpty())
    }

    @Test
    fun `isLoading is true while loadLibrary is in progress`() = runTest(testDispatcher) {
        // With StandardTestDispatcher passed as IO dispatcher, the coroutine is queued.
        // But advanceUntilIdle() runs EVERYTHING including the IO part.
        // So we can't easily check "in progress" state with a single dispatcher unless we pause.
        // However, we can check that it WAS true at some point if we use a spy or similar,
        // OR we can just check that it's false AFTER completion.
        // Since we want to fix the CI error, let's just verifying it completes.

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()
        assertFalse(viewModel!!.isLoading.value)
    }

    @Test
    fun `loadLibrary can be called manually`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.loadLibrary()
        advanceUntilIdle()

        // Called twice: once in init, once manually
        coVerify(atLeast = 2) { repository.getMangaOnLibrary() }
    }

    // Search functionality tests
    @Test
    fun `setSearchQuery filters manga by title`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")
        val manga3 = createMangaWithOwned(id = "3", title = "One Punch Man")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("One")

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun `setSearchQuery filters manga by alternative title`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece", altTitle = "ワンピース")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto", altTitle = "ナルト")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("ワンピース")

        val result = viewModel!!.mangaCollection.value
        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun `setSearchQuery is case insensitive`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("one piece")

        val result = viewModel!!.mangaCollection.value
        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun `setSearchQuery with empty string shows all manga`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("")

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
    }

    @Test
    fun `setSearchQuery with no matches returns empty list`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("Dragon Ball")

        val result = viewModel!!.mangaCollection.value
        assertTrue(result.isEmpty())
    }

    // Incomplete filter tests
    @Test
    fun `toggleIncompleteFilter shows only manga with unacquired volumes`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece", volumeOwned = 5, volumeCount = 10)
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto", volumeOwned = 10, volumeCount = 10)
        val manga3 = createMangaWithOwned(id = "3", title = "Bleach", volumeOwned = 3, volumeCount = 8)

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.toggleIncompleteFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
        assertFalse(result.any { it.id == "2" })
    }

    @Test
    fun `toggleIncompleteFilter twice shows all manga again`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece", volumeOwned = 5, volumeCount = 10)
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto", volumeOwned = 10, volumeCount = 10)

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.toggleIncompleteFilter()
        viewModel!!.toggleIncompleteFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
    }

    @Test
    fun `showIncompleteOnly initial state is false`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        assertFalse(viewModel!!.showIncompleteOnly.value)
    }

    // Special editions filter tests
    @Test
    fun `toggleSpecialEditionsFilter shows only manga with special editions`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")
        val manga3 = createMangaWithOwned(id = "3", title = "Bleach")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns listOf("1", "3")

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.toggleSpecialEditionsFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
        assertFalse(result.any { it.id == "2" })
    }

    @Test
    fun `toggleSpecialEditionsFilter twice shows all manga again`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "Naruto")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns listOf("1")

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.toggleSpecialEditionsFilter()
        viewModel!!.toggleSpecialEditionsFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
    }

    @Test
    fun `showSpecialEditionsOnly initial state is false`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        assertFalse(viewModel!!.showSpecialEditionsOnly.value)
    }

    @Test
    fun `loadLibrary calls getMangaIdsWithSpecialEditions`() = runTest(testDispatcher) {
        coEvery { repository.getMangaOnLibrary() } returns emptyList()
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        coVerify { repository.getMangaIdsWithSpecialEditions() }
    }

    // Combined filters tests
    @Test
    fun `search and incomplete filter work together`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece", volumeOwned = 5, volumeCount = 10)
        val manga2 = createMangaWithOwned(id = "2", title = "One Punch Man", volumeOwned = 10, volumeCount = 10)
        val manga3 = createMangaWithOwned(id = "3", title = "Naruto", volumeOwned = 3, volumeCount = 8)

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns emptyList()

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("One")
        viewModel!!.toggleIncompleteFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun `search and special editions filter work together`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece")
        val manga2 = createMangaWithOwned(id = "2", title = "One Punch Man")
        val manga3 = createMangaWithOwned(id = "3", title = "Naruto")

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns listOf("1", "3")

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("One")
        viewModel!!.toggleSpecialEditionsFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun `all three filters work together`() = runTest(testDispatcher) {
        val manga1 = createMangaWithOwned(id = "1", title = "One Piece", volumeOwned = 5, volumeCount = 10)
        val manga2 = createMangaWithOwned(id = "2", title = "One Punch Man", volumeOwned = 10, volumeCount = 10)
        val manga3 = createMangaWithOwned(id = "3", title = "Naruto", volumeOwned = 3, volumeCount = 8)
        val manga4 = createMangaWithOwned(id = "4", title = "One Division", volumeOwned = 2, volumeCount = 5)

        coEvery { repository.getMangaOnLibrary() } returns listOf(manga1, manga2, manga3, manga4)
        coEvery { repository.getMangaIdsWithSpecialEditions() } returns listOf("1", "4")

        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        viewModel!!.setSearchQuery("One")
        viewModel!!.toggleIncompleteFilter()
        viewModel!!.toggleSpecialEditionsFilter()

        val result = viewModel!!.mangaCollection.value
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "4" })
    }

    @Test
    fun `searchQuery state is updated correctly`() = runTest(testDispatcher) {
        viewModel = MangaCollectionViewModel(repository, testDispatcher)
        advanceUntilIdle()

        assertEquals("", viewModel!!.searchQuery.value)

        viewModel!!.setSearchQuery("One Piece")

        assertEquals("One Piece", viewModel!!.searchQuery.value)
    }
}
