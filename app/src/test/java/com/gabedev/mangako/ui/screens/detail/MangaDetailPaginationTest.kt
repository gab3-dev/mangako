package com.gabedev.mangako.ui.screens.detail

import androidx.lifecycle.viewModelScope
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.local.CoverLanguage
import com.gabedev.mangako.data.model.MangaWithVolume
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MangaDetailPaginationTest {
    private val dispatcher = StandardTestDispatcher()
    private val manga = Manga(id = "manga", title = "Frieren", coverUrl = "", description = "Description")
    private lateinit var api: MangaDexRepository
    private lateinit var local: LibraryRepository
    private lateinit var vm: MangaDetailViewModel

    private fun volume(id: String = "v1", number: Float? = 1f) = Volume(
        id = id, mangaId = manga.id, title = manga.title, coverUrl = "url/$id",
        volume = number, locale = "ja",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = mockk(relaxed = true)
        local = mockk(relaxed = true)
        coEvery { local.getMangaWithVolume(any()) } returns null
        coEvery { api.getCoverListByManga(any(), any(), any(), any()) } returns emptyList()
        coEvery { api.getManga(any(), any()) } returns manga
        coEvery { local.updateManga(any()) } returns manga
        vm = MangaDetailViewModel(api, local, manga, autoLoad = false)
    }

    @After
    fun tearDown() {
        vm.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `remote offset uses raw page size and later special is loaded`() = runTest {
        val repeated = List(50) { volume() }
        val special = volume("special", 1.5f).copy(locale = "en", isSpecialEdition = true)
        coEvery { api.getCoverListByManga(any(), 0, 50, false) } returns repeated
        coEvery { api.getCoverListByManga(any(), 50, 50, false) } returns listOf(special)
        vm.loadMoreVolumes()
        advanceUntilIdle()
        assertEquals(1, vm.volumeList.value.size)
        assertEquals(50, vm.nextVolumeOffset.value)
        vm.loadMoreVolumes()
        advanceUntilIdle()
        assertEquals(listOf(volume(), special), vm.volumeList.value)
        assertEquals(51, vm.nextVolumeOffset.value)
        coVerify(exactly = 0) { local.insertVolumeList(any()) }
    }

    @Test
    fun `two pages without progress pause until explicit retry`() = runTest {
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } returns listOf(volume("special", null))
        repeat(3) {
            vm.loadMoreVolumes()
            advanceUntilIdle()
        }
        assertTrue(vm.paginationPaused.value)
        assertFalse(vm.noMoreVolume.value)
        assertEquals(1, vm.volumeList.value.size)
        assertEquals(3, vm.nextVolumeOffset.value)
        repeat(5) { vm.loadMoreVolumes() }
        advanceUntilIdle()
        coVerify(exactly = 3) { api.getCoverListByManga(any(), any(), any(), false) }
        coEvery { api.getCoverListByManga(any(), 3, 50, false) } returns listOf(volume("new", 2f))
        vm.retryVolumes()
        advanceUntilIdle()
        assertFalse(vm.paginationPaused.value)
        assertEquals(4, vm.nextVolumeOffset.value)
    }

    @Test
    fun `error pauses without changing offset and retry uses the same offset`() = runTest {
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } throws IOException("offline")
        vm.loadMoreVolumes()
        advanceUntilIdle()
        repeat(5) { vm.loadMoreVolumes() }
        advanceUntilIdle()
        coVerify(exactly = 1) { api.getCoverListByManga(any(), 0, 50, false) }
        assertTrue(vm.paginationPaused.value)
        assertFalse(vm.noMoreVolume.value)
        assertFalse(vm.isVolumeLoading.value)
        assertEquals(0, vm.nextVolumeOffset.value)
        coEvery { api.getCoverListByManga(any(), 0, 50, false) } returns listOf(volume())
        vm.retryVolumes()
        advanceUntilIdle()
        coVerify(exactly = 2) { api.getCoverListByManga(any(), 0, 50, false) }
        assertFalse(vm.paginationPaused.value)
    }

    @Test
    fun `simultaneous load events cannot queue multiple requests and empty page ends`() = runTest {
        repeat(10) { vm.loadMoreVolumes() }
        advanceUntilIdle()
        assertTrue(vm.noMoreVolume.value)
        repeat(10) { vm.loadMoreVolumes() }
        advanceUntilIdle()
        coVerify(exactly = 1) { api.getCoverListByManga(any(), any(), any(), any()) }
    }

    @Test
    fun `cached volumes stay visible offline but are never used as remote offset`() = runTest {
        val cached = listOf(volume().copy(owned = true))
        coEvery { local.getMangaWithVolume(any()) } returns MangaWithVolume(manga, cached)
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } throws IOException("offline")
        vm.viewModelScope.cancel()
        vm = MangaDetailViewModel(api, local, manga)
        advanceUntilIdle()
        assertEquals(cached, vm.volumeList.value)
        assertTrue(vm.paginationPaused.value)
        coVerify(exactly = 1) { api.getCoverListByManga(any(), 0, 50, false) }
    }

    @Test
    fun `loaded page uses ownership preserving merge and reads stored state`() = runTest {
        val owned = volume().copy(owned = true)
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } returns listOf(volume())
        coEvery { local.getMangaWithVolume(any()) } returns MangaWithVolume(manga, listOf(owned))
        vm.loadMoreVolumes()
        advanceUntilIdle()
        coVerify(exactly = 1) { local.updateOrInsertVolumeList(listOf(volume())) }
        coVerify(exactly = 0) { local.insertVolumeList(any()) }
        assertEquals(listOf(owned), vm.volumeList.value)
    }

    @Test
    fun `reaching the remote offset bound pauses without another request`() = runTest {
        vm.nextVolumeOffset.value = 9_999
        coEvery { api.getCoverListByManga(any(), 9_999, 1, false) } returns listOf(volume())
        vm.loadMoreVolumes()
        advanceUntilIdle()
        assertTrue(vm.paginationPaused.value)
        assertFalse(vm.noMoreVolume.value)
        vm.retryVolumes()
        vm.loadMoreVolumes()
        advanceUntilIdle()
        coVerify(exactly = 1) { api.getCoverListByManga(any(), any(), any(), any()) }
    }

    @Test
    fun `refresh cancels old request and ignores a late response`() = runTest {
        val oldResponse = CompletableDeferred<List<Volume>>()
        val newResponse = CompletableDeferred<List<Volume>>()
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } coAnswers {
            withContext(NonCancellable) { oldResponse.await() }
        }
        coEvery { api.getCoverListByManga(any(), 0, 50, true) } coAnswers { newResponse.await() }
        vm.loadMoreVolumes()
        runCurrent()
        vm.refreshManga()
        runCurrent()
        oldResponse.complete(listOf(volume("old")))
        runCurrent()
        assertTrue(vm.isVolumeLoading.value)
        assertTrue(vm.volumeList.value.isEmpty())
        newResponse.complete(listOf(volume("new")))
        advanceUntilIdle()
        assertEquals(listOf(volume("new")), vm.volumeList.value)
        assertFalse(vm.isVolumeLoading.value)
        assertEquals(1, vm.nextVolumeOffset.value)
        coVerify(exactly = 0) { local.updateOrInsertVolumeList(match { it.any { v -> v.id == "old" } }) }
    }

    @Test
    fun `clearing view model cancels pending volume request`() = runTest {
        val response = CompletableDeferred<List<Volume>>()
        coEvery { api.getCoverListByManga(any(), any(), any(), false) } coAnswers { response.await() }
        vm.loadMoreVolumes()
        runCurrent()
        vm.viewModelScope.cancel()
        response.complete(listOf(volume()))
        advanceUntilIdle()
        assertTrue(vm.volumeList.value.isEmpty())
        coVerify(exactly = 0) { local.updateOrInsertVolumeList(any()) }
    }

    @Test
    fun `changing cover language restarts paging once and keeps cached ownership`() = runTest {
        val cached = listOf(volume().copy(owned = true))
        vm.volumeList.value = cached
        vm.setCoverLanguage(CoverLanguage.JAPANESE)
        vm.nextVolumeOffset.value = 150
        vm.noMoreVolume.value = true
        vm.paginationPaused.value = true
        vm.setCoverLanguage(CoverLanguage.FRENCH)
        vm.setCoverLanguage(CoverLanguage.FRENCH)
        advanceUntilIdle()
        coVerify(exactly = 1) { api.getCoverListByManga(any(), 0, 50, true) }
        assertEquals(0, vm.nextVolumeOffset.value)
        assertFalse(vm.paginationPaused.value)
        assertEquals(cached, vm.volumeList.value)
    }
}
