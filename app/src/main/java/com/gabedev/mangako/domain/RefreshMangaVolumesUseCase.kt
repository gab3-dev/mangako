package com.gabedev.mangako.domain

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import kotlin.coroutines.cancellation.CancellationException

class RefreshMangaVolumesUseCase(
    private val apiRepository: MangaDexRepository,
    private val localRepository: LibraryRepository,
) {
    suspend fun refreshLibrary(forceRefresh: Boolean = false): LibrarySyncResult {
        val libraryManga = localRepository.getMangaOnLibrary().map { it.toManga() }
        var updatedCount = 0
        var failedCount = 0
        val newVolumesByManga = mutableListOf<MangaNewVolumes>()

        libraryManga.forEach { manga ->
            try {
                val result = refreshManga(manga, forceRefresh)
                updatedCount += result.updateCount
                if (result.newVolumes.isNotEmpty()) {
                    newVolumesByManga += MangaNewVolumes(
                        manga = result.manga,
                        volumes = result.newVolumes,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failedCount++
                localRepository.log(e)
            }
        }

        return LibrarySyncResult(
            updatedCount = updatedCount,
            mangaCount = libraryManga.size,
            failedCount = failedCount,
            newVolumesByManga = newVolumesByManga,
        )
    }

    private suspend fun refreshManga(manga: Manga, forceRefresh: Boolean): RefreshMangaResult {
        val updatedManga = apiRepository.getManga(manga.id, forceRefresh)
        val localVolumes = localRepository.getMangaWithVolume(manga.id)?.volumes.orEmpty()
        val remoteVolumes = fetchAllVolumes(updatedManga, forceRefresh).deduplicateVolumes()
        val preferredVolumes = remoteVolumes
            .filter { it.locale.matchesLanguage("ja") }
            .ifEmpty {
                updatedManga.originalLanguage
                    ?.let { language -> remoteVolumes.filter { it.locale.matchesLanguage(language) } }
                    .orEmpty()
            }
            .ifEmpty { remoteVolumes }
        val latestVolumeNumber = preferredVolumes
            .asSequence()
            .mapNotNull { it.volume }
            .maxOrNull()
            ?.toInt()
        val mangaWithLatestVolume = latestVolumeNumber
            ?.takeIf { it > updatedManga.volumeCount }
            ?.let { updatedManga.copy(volumeCount = it) }
            ?: updatedManga
        val finalManga = localRepository.updateManga(mangaWithLatestVolume) ?: manga
        val updateCount = remoteVolumes.countUpdatesComparedTo(localVolumes)
        val newVolumes = remoteVolumes.filterNewComparedTo(localVolumes)

        localRepository.updateOrInsertVolumeList(remoteVolumes)

        return RefreshMangaResult(
            manga = finalManga,
            updateCount = updateCount,
            newVolumes = newVolumes,
        )
    }

    private suspend fun fetchAllVolumes(manga: Manga, forceRefresh: Boolean): List<Volume> {
        val volumes = mutableListOf<Volume>()
        val limit = 50
        var offset = 0

        while (true) {
            val page = apiRepository.getCoverListByManga(
                manga = manga,
                offset = offset,
                limit = limit,
                refresh = forceRefresh,
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

    private fun List<Volume>.filterNewComparedTo(localVolumes: List<Volume>): List<Volume> {
        return filter { remoteVolume ->
            localVolumes.none { localVolume ->
                localVolume.id == remoteVolume.id || localVolume.hasSameNumberedIdentity(remoteVolume)
            }
        }
    }

    private fun Volume.hasSameNumberedIdentity(other: Volume): Boolean {
        return volume != null &&
            other.volume != null &&
            mangaId == other.mangaId &&
            volume == other.volume &&
            locale == other.locale
    }

    private fun String.matchesLanguage(language: String): Boolean {
        return lowercase().replace('_', '-').substringBefore('-') ==
            language.lowercase().replace('_', '-').substringBefore('-')
    }

    private data class RefreshMangaResult(
        val manga: Manga,
        val updateCount: Int,
        val newVolumes: List<Volume>,
    )
}
