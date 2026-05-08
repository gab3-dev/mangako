package com.gabedev.mangako.ui.screens.search_list

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private class FakeMangaDexRepository(
        private val searchResults: Map<String, List<Manga>>,
        private val enrichResult: suspend (Manga) -> Manga = { it }
    ) : MangaDexRepository {
        override suspend fun searchManga(title: String, offset: Int?): List<Manga> {
            return searchMangaPage(title, offset)
        }

        override suspend fun searchMangaPage(title: String, offset: Int?, limit: Int): List<Manga> {
            return searchResults[title].orEmpty()
        }

        override suspend fun enrichManga(manga: Manga): Manga {
            return enrichResult(manga)
        }

        override suspend fun getManga(id: String): Manga {
            error("Not used")
        }

        override suspend fun getMangaCoverFileName(id: String): String {
            error("Not used")
        }

        override suspend fun getAuthorNameById(id: String): String {
            error("Not used")
        }

        override suspend fun getCoverListByManga(
            manga: Manga,
            offset: Int?,
            limit: Int
        ) = emptyList<com.gabedev.mangako.data.model.Volume>()

        override fun log(message: Exception) = Unit
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiRepository = mockk(relaxed = true)
        coEvery { apiRepository.enrichManga(any()) } answers { firstArg<Manga>() }
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
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        assertEquals("One Piece", vm.queryString.value)
        assertEquals(1, vm.mangaList.value.size)
    }

    @Test
    fun `setQueryString with blank query loads discovery results`() = runTest {
        val mangaList = listOf(createManga("m1", "Discovery Manga"))
        coEvery { apiRepository.searchMangaPage("", 0, any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("")
        advanceUntilIdle()

        assertEquals("", vm.queryString.value)
        assertEquals(1, vm.mangaList.value.size)
        assertEquals("Discovery Manga", vm.mangaList.value[0].title)
        coVerify(exactly = 1) { apiRepository.searchMangaPage("", 0, any()) }
    }

    @Test
    fun `setQueryString resets offset and mangaList`() = runTest {
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns emptyList()

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
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
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.queryString.value = "test"
        vm.loadMangaList()
        advanceUntilIdle()

        assertEquals(2, vm.mangaList.value.size)
        assertFalse(vm.isMangaLoading.value)
    }

    @Test
    fun `loadMangaList sets noMoreManga when empty result`() = runTest {
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns emptyList()

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.queryString.value = "nonexistent"
        vm.loadMangaList()
        advanceUntilIdle()

        assertTrue(vm.noMoreManga.value)
        assertTrue(vm.mangaList.value.isEmpty())
    }

    @Test
    fun `refreshMangaList resets and reloads`() = runTest {
        val mangaList = listOf(createManga("m1", "Test"))
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.queryString.value = "Test"
        vm.refreshMangaList()
        advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        assertFalse(vm.isMangaLoading.value)
    }

    @Test
    fun `loadMangaList emits base list before enrichment updates item`() = runTest {
        val baseManga = createManga("m1", "One Piece")
        val enrichedManga = baseManga.copy(author = "Eiichiro Oda")
        val repository = FakeMangaDexRepository(mapOf("One Piece" to listOf(baseManga))) {
            delay(1_000)
            enrichedManga
        }

        val vm = MangaSearchListViewModel(repository, testDispatcher)
        vm.setQueryString("One Piece")
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, vm.mangaList.value.size)
        assertEquals(null, vm.mangaList.value[0].author)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Eiichiro Oda", vm.mangaList.value[0].author)
    }

    @Test
    fun `enrichment failure keeps base item in list`() = runTest {
        val baseManga = createManga("m1", "One Piece")
        val repository = FakeMangaDexRepository(mapOf("One Piece" to listOf(baseManga))) {
            throw RuntimeException("metadata failed")
        }

        val vm = MangaSearchListViewModel(repository, testDispatcher)
        vm.setQueryString("One Piece")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        assertEquals("One Piece", vm.mangaList.value[0].title)
    }

    @Test
    fun `new query cancels old enrichment results`() = runTest {
        val naruto = createManga("m1", "Naruto")
        val bleach = createManga("m2", "Bleach")
        val repository = FakeMangaDexRepository(
            searchResults = mapOf(
                "Naruto" to listOf(naruto),
                "Bleach" to listOf(bleach)
            )
        ) { manga ->
            if (manga.id == naruto.id) {
                delay(1_000)
                manga.copy(author = "Old author")
            } else {
                manga.copy(author = "New author")
            }
        }

        val vm = MangaSearchListViewModel(repository, testDispatcher)
        vm.setQueryString("Naruto")
        testDispatcher.scheduler.runCurrent()

        vm.setQueryString("Bleach")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        assertEquals("Bleach", vm.mangaList.value[0].title)
        assertEquals("New author", vm.mangaList.value[0].author)
    }

    @Test
    fun `initial state has empty manga list`() = runTest {
        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)

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
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("Naruto")
        advanceUntilIdle()

        // First call should hit the API
        coVerify(exactly = 1) { apiRepository.searchMangaPage("Naruto", any(), any()) }
        assertEquals(1, vm.mangaList.value.size)

        // Setting the same query again should be a no-op
        vm.setQueryString("Naruto")
        advanceUntilIdle()

        // Still only 1 API call total
        coVerify(exactly = 1) { apiRepository.searchMangaPage("Naruto", any(), any()) }
    }

    @Test
    fun `loadMangaList serves cached results on second call`() = runTest {
        val mangaList = listOf(createManga("m1", "One Piece"))
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns mangaList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        // First load: API called
        coVerify(exactly = 1) { apiRepository.searchMangaPage("One Piece", 0, any()) }
        assertEquals(1, vm.mangaList.value.size)

        // Reset offset to simulate re-entering the same search
        vm.mangaList.value = emptyList()
        vm.queryString.value = ""
        vm.setQueryString("One Piece")
        advanceUntilIdle()

        // Second load should serve from cache — no new API call for offset 0
        coVerify(exactly = 1) { apiRepository.searchMangaPage("One Piece", 0, any()) }
        assertEquals(1, vm.mangaList.value.size)
    }

    @Test
    fun `refreshMangaList invalidates cache and re-fetches`() = runTest {
        val initialList = listOf(createManga("m1", "Bleach"))
        val refreshedList = listOf(
            createManga("m1", "Bleach"),
            createManga("m2", "Bleach 2")
        )
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns initialList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("Bleach")
        advanceUntilIdle()

        assertEquals(1, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchMangaPage("Bleach", 0, any()) }

        // Now refresh — cache should be cleared, API called again
        coEvery { apiRepository.searchMangaPage(any(), any(), any()) } returns refreshedList
        vm.refreshMangaList()
        advanceUntilIdle()

        coVerify(exactly = 2) { apiRepository.searchMangaPage("Bleach", 0, any()) }
        assertEquals(2, vm.mangaList.value.size)
    }

    @Test
    fun `different queries do not share cache`() = runTest {
        val narutoList = listOf(createManga("m1", "Naruto"))
        val bleachList = listOf(createManga("m2", "Bleach"))

        coEvery { apiRepository.searchMangaPage("Naruto", any(), any()) } returns narutoList
        coEvery { apiRepository.searchMangaPage("Bleach", any(), any()) } returns bleachList

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)

        vm.setQueryString("Naruto")
        advanceUntilIdle()
        assertEquals("Naruto", vm.mangaList.value[0].title)

        vm.setQueryString("Bleach")
        advanceUntilIdle()
        assertEquals("Bleach", vm.mangaList.value[0].title)

        // Both queries should have triggered their own API call
        coVerify(exactly = 1) { apiRepository.searchMangaPage("Naruto", 0, any()) }
        coVerify(exactly = 1) { apiRepository.searchMangaPage("Bleach", 0, any()) }
    }

    @Test
    fun `load more caches each offset independently`() = runTest {
        val page1 = listOf(
            createManga("m1", "A"),
            createManga("m2", "B"),
            createManga("m3", "C"),
            createManga("m4", "D"),
            createManga("m5", "E"),
            createManga("m6", "F")
        )
        val page2 = listOf(createManga("m7", "G"))

        coEvery { apiRepository.searchMangaPage("test", 0, any()) } returns page1
        coEvery { apiRepository.searchMangaPage("test", 6, any()) } returns page2

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("test")
        advanceUntilIdle()

        // First page loaded
        assertEquals(6, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchMangaPage("test", 0, any()) }

        // Load more (offset 6)
        vm.loadMangaList()
        advanceUntilIdle()

        assertEquals(7, vm.mangaList.value.size)
        coVerify(exactly = 1) { apiRepository.searchMangaPage("test", 6, any()) }

        // Reset and search same query — both pages should come from cache
        vm.mangaList.value = emptyList()
        vm.queryString.value = ""
        vm.setQueryString("test")
        advanceUntilIdle()

        // offset 0 served from cache — still only 1 API call for offset 0
        coVerify(exactly = 1) { apiRepository.searchMangaPage("test", 0, any()) }

        vm.loadMangaList()
        advanceUntilIdle()

        // offset 6 served from cache — still only 1 API call for offset 6
        coVerify(exactly = 1) { apiRepository.searchMangaPage("test", 6, any()) }
        assertEquals(7, vm.mangaList.value.size)
    }

    @Test
    fun `load more ignores manga ids already present in previous pages`() = runTest {
        val page1 = listOf(
            createManga("m1", "A"),
            createManga("m2", "B"),
            createManga("m3", "C"),
            createManga("m4", "D"),
            createManga("m5", "E"),
            createManga("m6", "F")
        )
        val page2 = listOf(
            createManga("m6", "F"),
            createManga("m7", "G")
        )

        coEvery { apiRepository.searchMangaPage("test", 0, any()) } returns page1
        coEvery { apiRepository.searchMangaPage("test", 6, any()) } returns page2

        val vm = MangaSearchListViewModel(apiRepository, testDispatcher)
        vm.setQueryString("test")
        advanceUntilIdle()

        vm.loadMangaList()
        advanceUntilIdle()

        assertEquals(listOf("m1", "m2", "m3", "m4", "m5", "m6", "m7"), vm.mangaList.value.map { it.id })
    }
}
