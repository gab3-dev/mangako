package com.gabedev.mangako.domain

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository

class RefreshMangaVolumesUseCase(
    private val apiRepository: MangaDexRepository,
    private val localRepository: LibraryRepository,
) {
    suspend fun refreshLibrary(): LibrarySyncResult {
        val libraryManga = localRepository.getMangaOnLibrary().map { it.toManga() }
        var updatedCount = 0
        var failedCount = 0

        libraryManga.forEach { manga ->
            try {
                updatedCount += refreshManga(manga)
            } catch (e: Exception) {
                failedCount++
                localRepository.log(e)
            }
        }

        return LibrarySyncResult(
            updatedCount = updatedCount,
            mangaCount = libraryManga.size,
            failedCount = failedCount,
        )
    }

    private suspend fun refreshManga(manga: Manga): Int {
        val updatedManga = apiRepository.getManga(manga.id)
        val finalManga = localRepository.updateManga(updatedManga) ?: manga
        val localVolumes = localRepository.getMangaWithVolume(manga.id)?.volumes.orEmpty()
        val remoteVolumes = fetchAllVolumes(finalManga).deduplicateVolumes()
        val updateCount = remoteVolumes.countUpdatesComparedTo(localVolumes)

        localRepository.updateOrInsertVolumeList(remoteVolumes)

        return updateCount
    }

    private suspend fun fetchAllVolumes(manga: Manga): List<Volume> {
        val volumes = mutableListOf<Volume>()
        val limit = 50
        var offset = 0

        while (true) {
            val page = apiRepository.getCoverListByManga(
                manga = manga,
                offset = offset,
                limit = limit,
            )
            if (page.isEmpty()) break

            volumes += page
            if (page.size < limit) break

            offset += limit
        }

        return volumes
    }

    private fun List<Volume>.deduplicateVolumes(): List<Volume> {
        val (numbered, unnumbered) = partition { it.volume != null }
        val deduplicatedNumbered = numbered
            .groupBy { Triple(it.mangaId, it.volume, it.locale) }
            .map { (_, volumes) ->
                volumes.maxByOrNull { it.updatedAt.orEmpty() } ?: volumes.first()
            }

        return deduplicatedNumbered + unnumbered.distinctBy { it.id }
    }

    private fun List<Volume>.countUpdatesComparedTo(localVolumes: List<Volume>): Int {
        return count { remoteVolume ->
            val localMatch = localVolumes.firstOrNull { localVolume ->
                localVolume.id == remoteVolume.id || localVolume.hasSameNumberedIdentity(remoteVolume)
            }

            localMatch == null ||
                localMatch.id != remoteVolume.id ||
                localMatch.coverUrl != remoteVolume.coverUrl ||
                localMatch.updatedAt != remoteVolume.updatedAt
        }
    }

    private fun Volume.hasSameNumberedIdentity(other: Volume): Boolean {
        return volume != null &&
            other.volume != null &&
            mangaId == other.mangaId &&
            volume == other.volume &&
            locale == other.locale
    }
}
