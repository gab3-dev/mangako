package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.dao.MangaDAO
import com.gabedev.mangako.data.dao.VolumeDAO
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.model.MangaWithVolume
import com.gabedev.mangako.data.model.Volume
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LibraryRepositoryImplTest {

    private lateinit var db: LocalDatabase
    private lateinit var mangaDao: MangaDAO
    private lateinit var volumeDao: VolumeDAO
    private lateinit var logger: FileLogger
    private lateinit var repository: LibraryRepositoryImpl

    private fun createManga(
        id: String = "manga-1",
        isOnLibrary: Boolean = false
    ) = Manga(
        id = id,
        title = "One Piece",
        coverUrl = "https://example.com/cover.jpg",
        description = "A pirate adventure",
        author = "Oda",
        status = "ongoing",
        isOnUserLibrary = isOnLibrary
    )

    private fun createVolume(
        id: String = "vol-1",
        mangaId: String = "manga-1",
        owned: Boolean = false
    ) = Volume(
        id = id,
        mangaId = mangaId,
        title = "One Piece",
        coverUrl = "https://example.com/$id.jpg",
        volume = 1.0f,
        owned = owned,
        locale = "ja"
    )

    @Before
    fun setup() {
        clearAllMocks()
        db = mockk()
        mangaDao = mockk()
        volumeDao = mockk()
        logger = mockk(relaxed = true)

        every { db.mangaDao() } returns mangaDao
        every { db.volumeDao() } returns volumeDao

        repository = LibraryRepositoryImpl(db, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // --- getManga tests ---

    @Test
    fun `getManga returns manga for valid id`() = runTest {
        val manga = createManga()
        coEvery { mangaDao.getMangaById("manga-1") } returns manga

        val result = repository.getManga("manga-1")

        assertEquals(manga, result)
    }

    @Test
    fun `getManga returns null for blank id`() = runTest {
        val result = repository.getManga("")

        assertNull(result)
        coVerify(exactly = 0) { mangaDao.getMangaById(any()) }
    }

    @Test
    fun `getManga returns null on exception`() = runTest {
        coEvery { mangaDao.getMangaById(any()) } throws RuntimeException("DB error")

        val result = repository.getManga("manga-1")

        assertNull(result)
        verify { logger.logError(any()) }
    }

    // --- getAllManga tests ---

    @Test
    fun `getAllManga returns list`() = runTest {
        val mangaList = listOf(createManga("m1"), createManga("m2"))
        coEvery { mangaDao.getAllManga() } returns mangaList

        val result = repository.getAllManga()

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllManga returns empty list on exception`() = runTest {
        coEvery { mangaDao.getAllManga() } throws RuntimeException("error")

        val result = repository.getAllManga()

        assertTrue(result.isEmpty())
        verify { logger.logError(any()) }
    }

    // --- getMangaOnLibrary tests ---

    @Test
    fun `getMangaOnLibrary filters by isOnUserLibrary`() = runTest {
        val inLib = MangaWithOwned(
            id = "m1", title = "T1", altTitle = null, type = null,
            coverId = null, coverFileName = null, coverUrl = "u",
            authorId = null, author = null, description = "d",
            status = null, volumeCount = 0, isOnUserLibrary = true,
            volumeOwned = 2
        )
        val notInLib = MangaWithOwned(
            id = "m2", title = "T2", altTitle = null, type = null,
            coverId = null, coverFileName = null, coverUrl = "u",
            authorId = null, author = null, description = "d",
            status = null, volumeCount = 0, isOnUserLibrary = false,
            volumeOwned = 0
        )
        coEvery { mangaDao.getAllMangaWithOwned() } returns listOf(inLib, notInLib)

        val result = repository.getMangaOnLibrary()

        assertEquals(1, result.size)
        assertEquals("m1", result[0].id)
    }

    // --- getMangaWithVolume tests ---

    @Test
    fun `getMangaWithVolume returns data for valid id`() = runTest {
        val manga = createManga()
        val volumes = listOf(createVolume())
        val mwv = MangaWithVolume(manga, volumes)
        coEvery { mangaDao.getMangaWithVolumeById("manga-1") } returns mwv

        val result = repository.getMangaWithVolume("manga-1")

        assertNotNull(result)
        assertEquals(1, result!!.volumes.size)
    }

    @Test
    fun `getMangaWithVolume returns null for blank id`() = runTest {
        val result = repository.getMangaWithVolume("")

        assertNull(result)
    }

    @Test
    fun `getMangaWithVolume returns null on exception`() = runTest {
        coEvery { mangaDao.getMangaWithVolumeById(any()) } throws RuntimeException("error")

        val result = repository.getMangaWithVolume("manga-1")

        assertNull(result)
    }

    // --- searchManga tests ---

    @Test
    fun `searchManga returns results for valid title`() = runTest {
        val list = listOf(createManga())
        coEvery { mangaDao.searchMangaByTitle("One Piece") } returns list

        val result = repository.searchManga("One Piece")

        assertEquals(1, result.size)
    }

    @Test
    fun `searchManga returns empty for blank title`() = runTest {
        val result = repository.searchManga("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchManga returns empty on exception`() = runTest {
        coEvery { mangaDao.searchMangaByTitle(any()) } throws RuntimeException("error")

        val result = repository.searchManga("test")

        assertTrue(result.isEmpty())
    }

    // --- insertManga tests ---

    @Test
    fun `insertManga calls dao`() = runTest {
        val manga = createManga()
        coEvery { mangaDao.insertManga(manga) } returns 1L

        repository.insertManga(manga)

        coVerify { mangaDao.insertManga(manga) }
    }

    // --- updateManga tests ---

    @Test
    fun `updateManga merges with existing and updates`() = runTest {
        val existing = createManga(isOnLibrary = true)
        val incoming = createManga().copy(title = "Updated Title", author = "New Author")
        coEvery { mangaDao.getMangaById("manga-1") } returns existing
        coEvery { mangaDao.updateManga(any()) } returns 1

        val result = repository.updateManga(incoming)

        assertNotNull(result)
        assertEquals("Updated Title", result!!.title)
        assertTrue(result.isOnUserLibrary) // Preserved from existing
    }

    @Test
    fun `updateManga returns null when manga not found`() = runTest {
        coEvery { mangaDao.getMangaById("manga-1") } returns null

        val result = repository.updateManga(createManga())

        assertNull(result)
    }

    // --- addMangaToLibrary tests ---

    @Test
    fun `addMangaToLibrary sets isOnUserLibrary to true`() = runTest {
        val manga = createManga()
        coEvery { mangaDao.updateMangaLibraryStatus(any()) } returns 1

        repository.addMangaToLibrary(manga)

        coVerify { mangaDao.updateMangaLibraryStatus(match { it.isOnUserLibrary }) }
    }

    // --- removeMangaFromLibrary(String) tests ---

    @Test
    fun `removeMangaFromLibrary by id updates library status`() = runTest {
        val manga = createManga(isOnLibrary = true)
        coEvery { mangaDao.getMangaById("manga-1") } returns manga
        coEvery { mangaDao.updateMangaLibraryStatus(any()) } returns 1

        repository.removeMangaFromLibrary("manga-1")

        coVerify { mangaDao.updateMangaLibraryStatus(match { !it.isOnUserLibrary }) }
    }

    @Test
    fun `removeMangaFromLibrary by id does nothing when not found`() = runTest {
        coEvery { mangaDao.getMangaById("nonexistent") } returns null

        repository.removeMangaFromLibrary("nonexistent")

        coVerify(exactly = 0) { mangaDao.updateMangaLibraryStatus(any()) }
    }

    // --- removeMangaFromLibrary(Manga) tests ---

    @Test
    fun `removeMangaFromLibrary by manga resets volumes and status`() = runTest {
        val manga = createManga(isOnLibrary = true)
        val vol1 = createVolume("v1", owned = true)
        val vol2 = createVolume("v2", owned = true)
        val mwv = MangaWithVolume(manga, listOf(vol1, vol2))

        coEvery { mangaDao.getMangaWithVolumeById("manga-1") } returns mwv
        coEvery { volumeDao.updateVolume(any()) } returns 1
        coEvery { mangaDao.updateMangaLibraryStatus(any()) } returns 1

        repository.removeMangaFromLibrary(manga)

        coVerify(exactly = 2) { volumeDao.updateVolume(match { !it.owned }) }
        coVerify { mangaDao.updateMangaLibraryStatus(match { !it.isOnUserLibrary }) }
    }

    // --- isMangaInLibrary tests ---

    @Test
    fun `isMangaInLibrary returns true when in library`() = runTest {
        coEvery { mangaDao.getMangaById("manga-1") } returns createManga(isOnLibrary = true)

        assertTrue(repository.isMangaInLibrary("manga-1"))
    }

    @Test
    fun `isMangaInLibrary returns false when not in library`() = runTest {
        coEvery { mangaDao.getMangaById("manga-1") } returns createManga(isOnLibrary = false)

        assertFalse(repository.isMangaInLibrary("manga-1"))
    }

    @Test
    fun `isMangaInLibrary returns false when manga not found`() = runTest {
        coEvery { mangaDao.getMangaById("missing") } returns null

        assertFalse(repository.isMangaInLibrary("missing"))
    }

    // --- insertVolumeList tests ---

    @Test
    fun `insertVolumeList calls dao`() = runTest {
        val volumes = listOf(createVolume("v1"), createVolume("v2"))
        coEvery { volumeDao.insertVolumeList(volumes) } just Runs

        repository.insertVolumeList(volumes)

        coVerify { volumeDao.insertVolumeList(volumes) }
    }

    @Test
    fun `insertVolumeList does nothing for empty list`() = runTest {
        repository.insertVolumeList(emptyList())

        coVerify(exactly = 0) { volumeDao.insertVolumeList(any()) }
    }

    @Test
    fun `insertVolumeList handles exception`() = runTest {
        val volumes = listOf(createVolume())
        coEvery { volumeDao.insertVolumeList(any()) } throws RuntimeException("error")

        repository.insertVolumeList(volumes) // Should not throw

        verify { logger.logError(any()) }
    }

    // --- updateVolume tests ---

    @Test
    fun `updateVolume calls dao`() = runTest {
        val volume = createVolume()
        coEvery { volumeDao.updateVolume(volume) } returns 1

        repository.updateVolume(volume)

        coVerify { volumeDao.updateVolume(volume) }
    }

    // --- updateVolumeList tests ---

    @Test
    fun `updateVolumeList calls dao`() = runTest {
        val volumes = listOf(createVolume())
        coEvery { volumeDao.updateVolumeList(volumes) } returns 1

        repository.updateVolumeList(volumes)

        coVerify { volumeDao.updateVolumeList(volumes) }
    }

    @Test
    fun `updateVolumeList does nothing for empty list`() = runTest {
        repository.updateVolumeList(emptyList())

        coVerify(exactly = 0) { volumeDao.updateVolumeList(any()) }
    }

    // --- updateOrInsertVolumeList tests ---

    @Test
    fun `updateOrInsertVolumeList updates existing and inserts new`() = runTest {
        val existingVol = createVolume("v1", owned = true)
        val newVol = createVolume("v2")

        coEvery { volumeDao.getVolumeById("v1") } returns existingVol
        coEvery { volumeDao.getVolumeById("v2") } returns null
        coEvery { volumeDao.updateVolumeList(any()) } returns 1
        coEvery { volumeDao.insertVolumeList(any()) } just Runs

        val incomingV1 = createVolume("v1").copy(coverUrl = "new-url")
        repository.updateOrInsertVolumeList(listOf(incomingV1, newVol))

        coVerify { volumeDao.updateVolumeList(any()) }
        coVerify { volumeDao.insertVolumeList(any()) }
    }

    @Test
    fun `updateOrInsertVolumeList does nothing for empty list`() = runTest {
        repository.updateOrInsertVolumeList(emptyList())

        coVerify(exactly = 0) { volumeDao.getVolumeById(any()) }
    }

    @Test
    fun `updateOrInsertVolumeList only updates when all exist`() = runTest {
        val existing = createVolume("v1", owned = true)
        coEvery { volumeDao.getVolumeById("v1") } returns existing
        coEvery { volumeDao.updateVolumeList(any()) } returns 1

        repository.updateOrInsertVolumeList(listOf(createVolume("v1")))

        coVerify { volumeDao.updateVolumeList(any()) }
        coVerify(exactly = 0) { volumeDao.insertVolumeList(any()) }
    }

    @Test
    fun `updateOrInsertVolumeList only inserts when all new`() = runTest {
        coEvery { volumeDao.getVolumeById("v1") } returns null
        coEvery { volumeDao.insertVolumeList(any()) } just Runs

        repository.updateOrInsertVolumeList(listOf(createVolume("v1")))

        coVerify(exactly = 0) { volumeDao.updateVolumeList(any()) }
        coVerify { volumeDao.insertVolumeList(any()) }
    }

    // --- log tests ---

    @Test
    fun `log delegates to logger`() {
        val exception = Exception("test error")

        repository.log(exception)

        verify { logger.log(match { it.contains("Library LOG") }) }
    }

    // --- getMangaIdsWithSpecialEditions tests ---

    @Test
    fun `getMangaIdsWithSpecialEditions returns list of manga IDs with special editions`() = runTest {
        val expectedIds = listOf("manga-1", "manga-2", "manga-3")
        coEvery { volumeDao.getMangaIdsWithSpecialEditions() } returns expectedIds

        val result = repository.getMangaIdsWithSpecialEditions()

        assertEquals(expectedIds, result)
        coVerify { volumeDao.getMangaIdsWithSpecialEditions() }
    }

    @Test
    fun `getMangaIdsWithSpecialEditions returns empty list when no special editions exist`() = runTest {
        coEvery { volumeDao.getMangaIdsWithSpecialEditions() } returns emptyList()

        val result = repository.getMangaIdsWithSpecialEditions()

        assertTrue(result.isEmpty())
        coVerify { volumeDao.getMangaIdsWithSpecialEditions() }
    }

    @Test
    fun `getMangaIdsWithSpecialEditions returns empty list on exception`() = runTest {
        coEvery { volumeDao.getMangaIdsWithSpecialEditions() } throws Exception("Database error")

        val result = repository.getMangaIdsWithSpecialEditions()

        assertTrue(result.isEmpty())
        verify { logger.logError(any()) }
    }
}
