package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
        MangaSearchCache.clear()
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

    // ---- Cache tests ----

    @Test
    fun `setQueryString with same query skips API call`() = runTest {
        val mangaList = listOf(createManga("m1", "Naruto"))
        coEvery { apiRepository.searchManga(any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("Naruto")
        advanceUntilIdle()

        // First call should hit the API
        coVerify(exactly = 1) { apiRepository.searchManga("Naruto", any()) }
        assertEquals(1, vm.mangaList.value.size)

        // Setting the same query again should be a no-op
        vm.setQueryString("Naruto")
        advanceUntilIdle()

        // Still only 1 API call total
        coVerify(exactly = 1) { apiRepository.searchManga("Naruto", any()) }
    }

    @Test
    fun `loadMangaList serves cached results on second call`() = runTest {
        val mangaList = listOf(createManga("m1", "One Piece"))
        coEvery { apiRepository.searchManga(any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        // First load: API called
        coVerify(exactly = 1) { apiRepository.searchManga("One Piece", 0) }
        assertEquals(1, vm.mangaList.value.size)

        // Reset offset to simulate re-entering the same search
        vm.mangaList.value = emptyList()
        vm.queryString.value = ""
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        // Second load should serve from cache — no new API call for offset 0
        coVerify(exactly = 1) { apiRepository.searchManga("One Piece", 0) }
        assertEquals(1, vm.mangaList.value.size)
    }

    @Test
    fun `refreshMangaList invalidates cache and re-fetches`() = runTest {
        val initialList = listOf(createManga("m1", "Bleach"))
        val refreshedList = listOf(
            createManga("m1", "Bleach"),
            createManga("m2", "Bleach 2")
        )
        coEvery { apiRepository.searchManga(any(), any()) } returns initialList

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("Bleach")
        advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchManga("Bleach", 0) }

        // Now refresh — cache should be cleared, API called again
        coEvery { apiRepository.searchManga(any(), any()) } returns refreshedList
        vm.refreshMangaList()
        advanceUntilIdle()

        coVerify(exactly = 2) { apiRepository.searchManga("Bleach", 0) }
        assertEquals(2, vm.mangaList.value.size)
    }

    @Test
    fun `different queries do not share cache`() = runTest {
        val narutoList = listOf(createManga("m1", "Naruto"))
        val bleachList = listOf(createManga("m2", "Bleach"))

        coEvery { apiRepository.searchManga("Naruto", any()) } returns narutoList
        coEvery { apiRepository.searchManga("Bleach", any()) } returns bleachList

        val vm = MangaSearchListViewModel(apiRepository)

        vm.setQueryString("Naruto")
        advanceUntilIdle()
        assertEquals("Naruto", vm.mangaList.value[0].title)

        vm.setQueryString("Bleach")
        advanceUntilIdle()
        assertEquals("Bleach", vm.mangaList.value[0].title)

        // Both queries should have triggered their own API call
        coVerify(exactly = 1) { apiRepository.searchManga("Naruto", 0) }
        coVerify(exactly = 1) { apiRepository.searchManga("Bleach", 0) }
    }

    @Test
    fun `load more caches each offset independently`() = runTest {
        val page1 = listOf(createManga("m1", "A"), createManga("m2", "B"))
        val page2 = listOf(createManga("m3", "C"))

        coEvery { apiRepository.searchManga("test", 0) } returns page1
        coEvery { apiRepository.searchManga("test", 6) } returns page2

        val vm = MangaSearchListViewModel(apiRepository)
        vm.setQueryString("test")
        advanceUntilIdle()

        // First page loaded
        assertEquals(2, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchManga("test", 0) }

        // Load more (offset 6)
        vm.loadMangaList()
        advanceUntilIdle()

        assertEquals(3, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchManga("test", 6) }

        // Reset and search same query — both pages should come from cache
        vm.mangaList.value = emptyList()
        vm.queryString.value = ""
        vm.setQueryString("test")
        advanceUntilIdle()

        // offset 0 served from cache — still only 1 API call for offset 0
        coVerify(exactly = 1) { apiRepository.searchManga("test", 0) }

        vm.loadMangaList()
        advanceUntilIdle()

        // offset 6 served from cache — still only 1 API call for offset 6
        coVerify(exactly = 1) { apiRepository.searchManga("test", 6) }
        assertEquals(3, vm.mangaList.value.size)
    }
}
