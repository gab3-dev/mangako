package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class MangaSearchListViewModelTest {

    private lateinit var apiRepository: MangaDexRepository
    private val testDispatcher = StandardTestDispatcher()

    private fun createManga(id: String, title: String) = Manga(
        id = id,
        title = title,
        coverUrl = "https://example.com/$id.jpg",
        description = "Description for $title"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `setQueryString updates query and triggers load`() = runTest {
        val mangaList = listOf(createManga("m1", "One Piece"))
        coEvery { apiRepository.searchManga(any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        assertEquals("One Piece", vm.queryString.value)
        assertEquals(1, vm.mangaList.value.size)
    }

    @Test
    fun `setQueryString resets offset and mangaList`() = runTest {
        coEvery { apiRepository.searchManga(any(), any()) } returns emptyList()

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("test")
        advanceUntilIdle()

        // After setting query, noMoreManga should be true since empty result
        assertTrue(vm.noMoreManga.value)
    }

    @Test
    fun `loadMangaList populates manga list`() = runTest {
        val mangaList = listOf(
            createManga("m1", "Naruto"),
            createManga("m2", "Bleach")
        )
        coEvery { apiRepository.searchManga(any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.queryString.value = "test"
        vm.loadMangaList()
        advanceUntilIdle()

        assertEquals(2, vm.mangaList.value.size)
        assertFalse(vm.isMangaLoading.value)
    }

    @Test
    fun `loadMangaList sets noMoreManga when empty result`() = runTest {
        coEvery { apiRepository.searchManga(any(), any()) } returns emptyList()

        val vm = MangaSearchListViewModel(apiRepository)
        vm.queryString.value = "nonexistent"
        vm.loadMangaList()
        advanceUntilIdle()

        assertTrue(vm.noMoreManga.value)
        assertTrue(vm.mangaList.value.isEmpty())
    }

    @Test
    fun `refreshMangaList resets and reloads`() = runTest {
        val mangaList = listOf(createManga("m1", "Test"))
        coEvery { apiRepository.searchManga(any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.queryString.value = "Test"
        vm.refreshMangaList()
        advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        assertFalse(vm.isMangaLoading.value)
    }

    @Test
    fun `initial state has empty manga list`() = runTest {
        val vm = MangaSearchListViewModel(apiRepository)

        assertTrue(vm.mangaList.value.isEmpty())
        assertEquals("", vm.queryString.value)
        assertFalse(vm.isMangaLoading.value)
        assertFalse(vm.isLoadingMoreManga.value)
        assertFalse(vm.noMoreManga.value)
    }
}
