package com.gabedev.mangako.domain

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.model.MangaWithVolume
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshMangaVolumesUseCaseTest {
    @Test
    fun `refreshLibrary reports remote volume as new when no local volume matches`() = runTest {
        val manga = createManga("manga-1", "One Piece")
        val remoteVolume = createVolume("cover-1", manga.id, 1f)
        val apiRepository = FakeMangaRepository(
            mangaResults = mapOf(manga.id to manga),
            volumeResults = mapOf(manga.id to listOf(remoteVolume)),
        )
        val localRepository = FakeLibraryRepository(
            library = listOf(manga),
            localVolumes = mapOf(manga.id to emptyList()),
        )

        val result = RefreshMangaVolumesUseCase(apiRepository, localRepository)
            .refreshLibrary(forceRefresh = true)

        assertEquals(1, result.updatedCount)
        assertEquals(1, result.newVolumesByManga.size)
        assertEquals(manga.copy(volumeCount = 1), result.newVolumesByManga.single().manga)
        assertEquals(listOf(remoteVolume), result.newVolumesByManga.single().volumes)
        assertEquals(listOf(remoteVolume), localRepository.persistedVolumes.single())
    }

    @Test
    fun `refreshLibrary does not report existing volume as new when only cover changes`() = runTest {
        val manga = createManga("manga-1", "One Piece")
        val localVolume = createVolume("cover-1", manga.id, 1f, coverUrl = "old.jpg")
        val remoteVolume = createVolume("cover-1", manga.id, 1f, coverUrl = "new.jpg")
        val apiRepository = FakeMangaRepository(
            mangaResults = mapOf(manga.id to manga),
            volumeResults = mapOf(manga.id to listOf(remoteVolume)),
        )
        val localRepository = FakeLibraryRepository(
            library = listOf(manga),
            localVolumes = mapOf(manga.id to listOf(localVolume)),
        )

        val result = RefreshMangaVolumesUseCase(apiRepository, localRepository)
            .refreshLibrary(forceRefresh = true)

        assertEquals(1, result.updatedCount)
        assertTrue(result.newVolumesByManga.isEmpty())
        assertEquals(listOf(remoteVolume), localRepository.persistedVolumes.single())
    }

    @Test
    fun `refreshLibrary continues with remaining manga when one manga fails`() = runTest {
        val failedManga = createManga("manga-1", "Failed Manga")
        val successfulManga = createManga("manga-2", "Successful Manga")
        val remoteVolume = createVolume("cover-2", successfulManga.id, 2f)
        val apiRepository = FakeMangaRepository(
            mangaResults = mapOf(successfulManga.id to successfulManga),
            volumeResults = mapOf(successfulManga.id to listOf(remoteVolume)),
            failedMangaIds = setOf(failedManga.id),
        )
        val localRepository = FakeLibraryRepository(
            library = listOf(failedManga, successfulManga),
            localVolumes = mapOf(successfulManga.id to emptyList()),
        )

        val result = RefreshMangaVolumesUseCase(apiRepository, localRepository)
            .refreshLibrary(forceRefresh = true)

        assertEquals(2, result.mangaCount)
        assertEquals(1, result.failedCount)
        assertEquals(1, result.updatedCount)
        assertEquals(
            successfulManga.copy(volumeCount = 2),
            result.newVolumesByManga.single().manga,
        )
        assertEquals(listOf(remoteVolume), result.newVolumesByManga.single().volumes)
        assertEquals(1, localRepository.loggedExceptions.size)
    }

    @Test
    fun `refreshLibrary updates manga volume count from newly discovered volume`() = runTest {
        val manga = createManga("manga-1", "One Piece", volumeCount = 15)
        val existingVolume = createVolume("cover-15", manga.id, 15f)
        val newVolume = createVolume("cover-16", manga.id, 16f)
        val apiRepository = FakeMangaRepository(
            mangaResults = mapOf(manga.id to manga),
            volumeResults = mapOf(manga.id to listOf(existingVolume, newVolume)),
        )
        val localRepository = FakeLibraryRepository(
            library = listOf(manga),
            localVolumes = mapOf(manga.id to listOf(existingVolume)),
        )

        val result = RefreshMangaVolumesUseCase(apiRepository, localRepository)
            .refreshLibrary(forceRefresh = true)

        assertEquals(16, localRepository.persistedManga.single().volumeCount)
        assertEquals(16, result.newVolumesByManga.single().manga.volumeCount)
        assertEquals(listOf(newVolume), result.newVolumesByManga.single().volumes)
    }

    private class FakeMangaRepository(
        private val mangaResults: Map<String, Manga>,
        private val volumeResults: Map<String, List<Volume>>,
        private val failedMangaIds: Set<String> = emptySet(),
    ) : MangaDexRepository {
        override suspend fun searchManga(title: String, offset: Int?): List<Manga> = emptyList()

        override suspend fun searchMangaPage(title: String, offset: Int?, limit: Int): List<Manga> = emptyList()

        override suspend fun enrichManga(manga: Manga): Manga = manga

        override suspend fun getManga(id: String, refresh: Boolean): Manga {
            if (id in failedMangaIds) error("Could not refresh manga $id")
            return mangaResults.getValue(id)
        }

        override suspend fun getMangaCoverFileName(id: String): String = ""

        override suspend fun getAuthorNameById(id: String): String = ""

        override suspend fun getCoverListByManga(
            manga: Manga,
            offset: Int?,
            limit: Int,
            refresh: Boolean,
        ): List<Volume> {
            return if ((offset ?: 0) == 0) volumeResults[manga.id].orEmpty() else emptyList()
        }

        override fun log(message: Exception) = Unit
    }

    private class FakeLibraryRepository(
        private val library: List<Manga>,
        private val localVolumes: Map<String, List<Volume>>,
    ) : LibraryRepository {
        val persistedVolumes = mutableListOf<List<Volume>>()
        val persistedManga = mutableListOf<Manga>()
        val loggedExceptions = mutableListOf<Exception>()

        override suspend fun getManga(mangaId: String): Manga? = library.firstOrNull { it.id == mangaId }

        override suspend fun getAllManga(): List<Manga> = library

        override suspend fun getMangaOnLibrary(): List<MangaWithOwned> = library.map { it.toMangaWithOwned() }

        override suspend fun getMangaWithVolume(mangaId: String): MangaWithVolume? {
            val manga = library.firstOrNull { it.id == mangaId } ?: return null
            return MangaWithVolume(manga, localVolumes[mangaId].orEmpty())
        }

        override suspend fun searchManga(title: String): List<Manga> = emptyList()

        override suspend fun insertManga(manga: Manga) = Unit

        override suspend fun updateManga(manga: Manga): Manga {
            persistedManga += manga
            return manga
        }

        override suspend fun addMangaToLibrary(manga: Manga) = Unit

        override suspend fun removeMangaFromLibrary(mangaId: String) = Unit

        override suspend fun removeMangaFromLibrary(manga: Manga) = Unit

        override suspend fun isMangaInLibrary(mangaId: String): Boolean = true

        override suspend fun insertVolumeList(volumeList: List<Volume>) = Unit

        override suspend fun updateVolume(volume: Volume) = Unit

        override suspend fun updateVolumeList(volumeList: List<Volume>) = Unit

        override suspend fun updateOrInsertVolumeList(volumeList: List<Volume>) {
            persistedVolumes += volumeList
        }

        override suspend fun getMangaIdsWithSpecialEditions(): List<String> = emptyList()

        override fun log(message: Exception) {
            loggedExceptions += message
        }

        private fun Manga.toMangaWithOwned() = MangaWithOwned(
            id = id,
            title = title,
            altTitle = altTitle,
            type = type,
            coverId = coverId,
            coverFileName = coverFileName,
            coverUrl = coverUrl,
            authorId = authorId,
            author = author,
            description = description,
            status = status,
            volumeCount = volumeCount,
            isOnUserLibrary = isOnUserLibrary,
            volumeOwned = 0,
        )
    }

    private fun createManga(id: String, title: String, volumeCount: Int = 0) = Manga(
        id = id,
        title = title,
        coverUrl = "https://example.com/$id.jpg",
        description = "Description for $title",
        volumeCount = volumeCount,
        isOnUserLibrary = true,
    )

    private fun createVolume(
        id: String,
        mangaId: String,
        volume: Float,
        coverUrl: String = "https://example.com/$id.jpg",
    ) = Volume(
        id = id,
        mangaId = mangaId,
        title = "Volume $volume",
        coverUrl = coverUrl,
        volume = volume,
        locale = "ja",
        updatedAt = "2026-01-01T00:00:00Z",
    )

}
