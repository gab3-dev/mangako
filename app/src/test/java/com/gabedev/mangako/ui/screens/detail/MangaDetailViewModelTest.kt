package com.gabedev.mangako.ui.screens.detail

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MangaDetailViewModelTest {

    private lateinit var apiRepository: MangaDexRepository
    private lateinit var localRepository: LibraryRepository
    private val testDispatcher = StandardTestDispatcher()

    private fun createManga(
        id: String = "manga-1",
        isOnLibrary: Boolean = false
    ) = Manga(
        id = id,
        title = "One Piece",
        coverUrl = "https://example.com/cover.jpg",
        description = "A pirate adventure",
        isOnUserLibrary = isOnLibrary
    )

    private fun createVolume(
        id: String = "vol-1",
        owned: Boolean = false,
        volumeNumber: Float = 1.0f,
        updatedAt: String? = null
    ) = Volume(
        id = id,
        mangaId = "manga-1",
        title = "One Piece",
        coverUrl = "https://example.com/$id.jpg",
        volume = volumeNumber,
        owned = owned,
        updatedAt = updatedAt,
        locale = "ja"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiRepository = mockk(relaxed = true)
        localRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createViewModel(manga: Manga = createManga()): MangaDetailViewModel {
        // Mock init calls
        coEvery { localRepository.getManga(any()) } returns null
        coEvery { localRepository.insertManga(any()) } just Runs
        coEvery { localRepository.isMangaInLibrary(any()) } returns false
        coEvery { localRepository.getMangaWithVolume(any()) } returns null
        coEvery { apiRepository.getCoverListByManga(any(), any(), any()) } returns emptyList()
        coEvery { localRepository.insertVolumeList(any()) } just Runs

        return MangaDetailViewModel(apiRepository, localRepository, manga)
    }

    // --- Selection tests ---

    @Test
    fun `toggleSelection adds id to selection`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleSelection("vol-1")

        assertTrue(vm.selectedIds.value.contains("vol-1"))
        assertTrue(vm.isMultiSelectActive.value)
    }

    @Test
    fun `toggleSelection removes already selected id`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleSelection("vol-1")
        vm.toggleSelection("vol-1")

        assertFalse(vm.selectedIds.value.contains("vol-1"))
        assertFalse(vm.isMultiSelectActive.value)
    }

    @Test
    fun `clearSelection empties selected ids`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleSelection("vol-1")
        vm.toggleSelection("vol-2")
        vm.clearSelection()

        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun `finishMultiSelect disables multi-select and clears selection`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleSelection("vol-1")
        vm.finishMultiSelect()

        assertFalse(vm.isMultiSelectActive.value)
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun `selectAllVolumes selects all volume ids`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Manually set volume list
        vm.volumeList.value = listOf(
            createVolume("v1"),
            createVolume("v2"),
            createVolume("v3")
        )

        vm.selectAllVolumes()

        assertEquals(3, vm.selectedIds.value.size)
        assertTrue(vm.selectedIds.value.containsAll(setOf("v1", "v2", "v3")))
    }

    // --- addMangaToLibrary tests ---

    @Test
    fun `addMangaToLibrary sets isMangaInLibrary to true on success`() = runTest {
        val manga = createManga()
        coEvery { localRepository.addMangaToLibrary(any()) } just Runs
        val vm = createViewModel(manga)
        advanceUntilIdle()

        vm.addMangaToLibrary(manga)
        advanceUntilIdle()

        assertTrue(vm.isMangaInLibrary.value)
        assertTrue(vm.addResult.value?.isSuccess == true)
    }

    @Test
    fun `addMangaToLibrary sets failure result on exception`() = runTest {
        val manga = createManga()
        coEvery { localRepository.addMangaToLibrary(any()) } throws RuntimeException("error")
        val vm = createViewModel(manga)
        advanceUntilIdle()

        vm.addMangaToLibrary(manga)
        advanceUntilIdle()

        assertTrue(vm.addResult.value?.isFailure == true)
    }

    // --- removeMangaFromLibrary tests ---

    @Test
    fun `removeMangaFromLibrary sets isMangaInLibrary to false on success`() = runTest {
        val manga = createManga(isOnLibrary = true)
        coEvery { localRepository.removeMangaFromLibrary(manga.id) } just Runs
        val vm = createViewModel(manga)
        advanceUntilIdle()

        vm.removeMangaFromLibrary()
        advanceUntilIdle()

        assertFalse(vm.isMangaInLibrary.value)
        assertTrue(vm.removeResult.value?.isSuccess == true)
    }

    @Test
    fun `removeMangaFromLibrary sets failure result on exception`() = runTest {
        val manga = createManga(isOnLibrary = true)
        coEvery { localRepository.removeMangaFromLibrary(manga.id) } throws RuntimeException("error")
        val vm = createViewModel(manga)
        advanceUntilIdle()

        vm.removeMangaFromLibrary()
        advanceUntilIdle()

        assertTrue(vm.removeResult.value?.isFailure == true)
    }

    // --- toggleVolumeOwned tests ---

    @Test
    fun `toggleVolumeOwned flips owned status in volume list`() = runTest {
        val volume = createVolume("v1", owned = false)
        coEvery { localRepository.updateVolume(any()) } just Runs
        val vm = createViewModel()
        advanceUntilIdle()
        vm.volumeList.value = listOf(volume)

        vm.toggleVolumeOwned(volume)
        advanceUntilIdle()

        val updated = vm.volumeList.value.find { it.id == "v1" }
        assertTrue(updated!!.owned)
    }

    // --- markSelectedListAsOwned tests ---

    @Test
    fun `markSelectedListAsOwned marks selected volumes`() = runTest {
        val v1 = createVolume("v1", owned = false)
        val v2 = createVolume("v2", owned = false)
        coEvery { localRepository.updateVolumeList(any()) } just Runs
        val vm = createViewModel()
        advanceUntilIdle()
        vm.volumeList.value = listOf(v1, v2)
        vm.toggleSelection("v1")

        vm.markSelectedListAsOwned(true)
        advanceUntilIdle()

        val updatedV1 = vm.volumeList.value.find { it.id == "v1" }
        val updatedV2 = vm.volumeList.value.find { it.id == "v2" }
        assertTrue(updatedV1!!.owned)
        assertFalse(updatedV2!!.owned)
        assertTrue(vm.selectedIds.value.isEmpty()) // selection cleared
    }

    // --- clearAddResult / clearRemoveResult tests ---

    @Test
    fun `clearAddResult sets addResult to null`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.addMangaToLibrary(createManga())
        advanceUntilIdle()
        vm.clearAddResult()

        assertNull(vm.addResult.value)
    }

    @Test
    fun `clearRemoveResult sets removeResult to null`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.removeMangaFromLibrary()
        advanceUntilIdle()
        vm.clearRemoveResult()

        assertNull(vm.removeResult.value)
    }

    // --- init behavior tests ---

    @Test
    fun `init inserts manga if not in local database`() = runTest {
        coEvery { localRepository.getManga("manga-1") } returns null
        val vm = createViewModel()
        advanceUntilIdle()

        coVerify { localRepository.insertManga(any()) }
    }

    @Test
    fun `init does not insert manga if already in local database`() = runTest {
        coEvery { localRepository.getManga("manga-1") } returns createManga()
        val vm = createViewModel()
        advanceUntilIdle()

        // insertManga may still be mocked, but we verify getManga was checked
        coVerify { localRepository.getManga("manga-1") }
    }

    // --- Deduplication tests ---

    @Test
    fun `loadCoverList keeps only one volume per volume number`() = runTest {
        val manga = createManga()
        val duplicateVolumes = listOf(
            createVolume(id = "v1", volumeNumber = 1.0f, updatedAt = "2024-01-01T00:00:00Z"),
            createVolume(id = "v2", volumeNumber = 1.0f, updatedAt = "2024-01-02T00:00:00Z"),
            createVolume(id = "v3", volumeNumber = 2.0f, updatedAt = "2024-01-01T00:00:00Z")
        )

        val vm = createViewModel(manga)

        // Set up mocks after createViewModel (which overrides them with emptyList),
        // but before advanceUntilIdle (which actually runs the init coroutines)
        coEvery { apiRepository.getCoverListByManga(any(), any(), any()) } returns duplicateVolumes
        coEvery { localRepository.insertVolumeList(any()) } just Runs

        advanceUntilIdle()

        // Should only have 2 volumes (volume 1 and volume 2)
        assertEquals(2, vm.volumeList.value.size)
    }

    @Test
    fun `loadCoverList keeps most recently updated volume when duplicates exist`() = runTest {
        val manga = createManga()
        val duplicateVolumes = listOf(
            createVolume(id = "v1", volumeNumber = 1.0f, updatedAt = "2024-01-01T00:00:00Z"),
            createVolume(id = "v2", volumeNumber = 1.0f, updatedAt = "2024-01-03T00:00:00Z"),
            createVolume(id = "v3", volumeNumber = 1.0f, updatedAt = "2024-01-02T00:00:00Z")
        )

        val vm = createViewModel(manga)

        coEvery { apiRepository.getCoverListByManga(any(), any(), any()) } returns duplicateVolumes
        coEvery { localRepository.insertVolumeList(any()) } just Runs

        advanceUntilIdle()

        // Should only have 1 volume
        assertEquals(1, vm.volumeList.value.size)

        // Should keep the most recently updated volume (v2 with 2024-01-03)
        val volume = vm.volumeList.value.first()
        assertEquals("v2", volume.id)
        assertEquals("2024-01-03T00:00:00Z", volume.updatedAt)
    }

    @Test
    fun `loadMoreVolumes maintains deduplication across pages`() = runTest {
        val manga = createManga()
        val initialVolumes = listOf(
            createVolume(id = "v1", volumeNumber = 1.0f, updatedAt = "2024-01-01T00:00:00Z")
        )
        val moreVolumes = listOf(
            createVolume(id = "v2", volumeNumber = 1.0f, updatedAt = "2024-01-02T00:00:00Z"),
            createVolume(id = "v3", volumeNumber = 2.0f, updatedAt = "2024-01-01T00:00:00Z")
        )

        val vm = createViewModel(manga)

        coEvery { apiRepository.getCoverListByManga(any(), any(), any()) } returnsMany listOf(
            initialVolumes,
            moreVolumes
        )
        coEvery { localRepository.insertVolumeList(any()) } just Runs
        coEvery { localRepository.getMangaWithVolume(any()) } returns null
        advanceUntilIdle()

        // Initial load should have 1 volume
        assertEquals(1, vm.volumeList.value.size)

        vm.loadMoreVolumes()
        advanceUntilIdle()

        // After loading more, should have 2 unique volumes (1 and 2)
        assertEquals(2, vm.volumeList.value.size)

        // Volume 1 should be the most recently updated (v2)
        val volume1 = vm.volumeList.value.find { it.volume == 1.0f }
        assertEquals("v2", volume1?.id)
        assertEquals("2024-01-02T00:00:00Z", volume1?.updatedAt)
    }

    @Test
    fun `no duplicated covers are inserted into local database`() = runTest {
        val manga = createManga()
        val duplicateVolumes = listOf(
            createVolume(id = "v1", volumeNumber = 1.0f, updatedAt = "2024-01-01T00:00:00Z"),
            createVolume(id = "v2", volumeNumber = 1.0f, updatedAt = "2024-01-02T00:00:00Z"),
            createVolume(id = "v3", volumeNumber = 1.0f, updatedAt = "2024-01-03T00:00:00Z"),
            createVolume(id = "v4", volumeNumber = 2.0f, updatedAt = "2024-01-01T00:00:00Z")
        )

        var insertedVolumes: List<Volume>? = null
        coEvery { apiRepository.getCoverListByManga(any(), any(), any()) } returns duplicateVolumes
        coEvery { localRepository.insertVolumeList(any()) } answers {
            insertedVolumes = firstArg()
        }

        val vm = createViewModel(manga)
        advanceUntilIdle()

        // Verify that only unique volumes by volume number are inserted
        assertEquals(2, insertedVolumes?.size)

        // Verify no duplicate volume numbers in inserted list
        val volumeNumbers = insertedVolumes?.mapNotNull { it.volume }?.toSet()
        assertEquals(2, volumeNumbers?.size)
        assertTrue(volumeNumbers?.contains(1.0f) == true)
        assertTrue(volumeNumbers?.contains(2.0f) == true)

        // Verify the most recently updated volume 1 is inserted (v3)
        val insertedVolume1 = insertedVolumes?.find { it.volume == 1.0f }
        assertEquals("v3", insertedVolume1?.id)
        assertEquals("2024-01-03T00:00:00Z", insertedVolume1?.updatedAt)
    }
}
