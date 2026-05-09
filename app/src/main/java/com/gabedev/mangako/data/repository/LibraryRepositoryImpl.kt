package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithOwned
import com.gabedev.mangako.data.model.MangaWithVolume
import com.gabedev.mangako.data.model.Volume

class LibraryRepositoryImpl(
    private val db: LocalDatabase,
    private val logger: FileLogger
) : LibraryRepository {
    override suspend fun getManga(mangaId: String): Manga? {
        try {
            if (mangaId.isBlank()) {
                return null
            }
            return db.mangaDao().getMangaById(mangaId)
        } catch (e: Exception) {
            logger.logError(e)
            return null
        }
    }

    override suspend fun getAllManga(): List<Manga> {
        try {
            return db.mangaDao().getAllManga()
        } catch (e: Exception) {
            logger.logError(e)
            return emptyList()
        }
    }

    override suspend fun getMangaOnLibrary(): List<MangaWithOwned> {
        return db.mangaDao().getAllMangaWithOwned().filter { it.isOnUserLibrary }
    }

    override suspend fun getMangaWithVolume(mangaId: String): MangaWithVolume? {
        try {
            if (mangaId.isBlank()) {
                return null
            }
            return db.mangaDao().getMangaWithVolumeById(mangaId)
        } catch (e: Exception) {
            logger.logError(e)
            return null
        }
    }

    override suspend fun searchManga(title: String): List<Manga> {
        try {
            if (title.isBlank()) {
                return emptyList()
            }
            return db.mangaDao().searchMangaByTitle(title)
        } catch (e: Exception) {
            logger.logError(e)
            return emptyList()
        }
    }

    override suspend fun insertManga(manga: Manga) {
        db.mangaDao().insertManga(manga)
    }

    override suspend fun updateManga(manga: Manga) : Manga? {
        // Fetch existing manga to preserve isOnUserLibrary status
        val existingManga = db.mangaDao().getMangaById(manga.id)
        val updatedManga: Manga? = existingManga?.copy(
            title = manga.title,
            author = manga.author,
            coverUrl = manga.coverUrl,
            description = manga.description,
            status = manga.status,
            volumeCount = manga.volumeCount,
        )
        if (updatedManga != null)
            db.mangaDao().updateManga(updatedManga)
        return updatedManga
    }

    override suspend fun addMangaToLibrary(manga: Manga) {
        val updatedManga = manga.copy(isOnUserLibrary = true)
        db.mangaDao().updateMangaLibraryStatus(updatedManga)
    }

    override suspend fun removeMangaFromLibrary(mangaId: String) {
        val manga = db.mangaDao().getMangaById(mangaId)
        if (manga == null) {
            logger.log("Manga with ID $mangaId not found in database.")
            return
        }
        val updatedManga = manga.copy(isOnUserLibrary = false)
        val volumeList = db.mangaDao().getMangaWithVolumeById(mangaId)
        volumeList?.volumes?.forEach { volume ->
            db.volumeDao().updateVolume(volume.copy(owned = false))
        }
        db.mangaDao().updateMangaLibraryStatus(updatedManga)
    }

    override suspend fun removeMangaFromLibrary(manga: Manga) {
        val updatedManga = manga.copy(isOnUserLibrary = false)
        val volumeList = db.mangaDao().getMangaWithVolumeById(manga.id)
        volumeList?.volumes?.forEach { volume ->
            db.volumeDao().updateVolume(volume.copy(owned = false))
        }
        db.mangaDao().updateMangaLibraryStatus(updatedManga)
    }

    override suspend fun isMangaInLibrary(mangaId: String): Boolean {
        return db.mangaDao().getMangaById(mangaId)?.isOnUserLibrary ?: false
    }

    override suspend fun insertVolumeList(volumeList: List<Volume>) {
        if (volumeList.isEmpty()) return
        try {
            db.volumeDao().insertVolumeList(volumeList)
        } catch (e: Exception) {
            logger.logError(e)
        }
    }

    override suspend fun updateVolume(volume: Volume) {
        db.volumeDao().updateVolume(volume)
    }

    override suspend fun updateVolumeList(volumeList: List<Volume>) {
        if (volumeList.isEmpty()) return
        try {
            db.volumeDao().updateVolumeList(volumeList)
        } catch (e: Exception) {
            logger.logError(e)
        }
    }

    override suspend fun updateOrInsertVolumeList(volumeList: List<Volume>) {
        if (volumeList.isEmpty()) return
        val incomingVolumes = volumeList.deduplicateForPersistence()
        val volumesToUpdate = mutableListOf<Volume>()
        val volumesToInsert = mutableListOf<Volume>()
        val volumeIdsToDelete = mutableSetOf<String>()
        val localVolumesByManga = incomingVolumes
            .map { it.mangaId }
            .distinct()
            .associateWith { mangaId ->
                db.volumeDao().getVolumesByMangaId(mangaId).toMutableList()
            }

        for (volume in incomingVolumes) {
            val localVolumes = localVolumesByManga[volume.mangaId].orEmpty()
            val matchingVolumes = localVolumes.filter { existing ->
                existing.id == volume.id || existing.hasSameNumberedIdentity(volume)
            }

            if (matchingVolumes.isNotEmpty()) {
                val owned = matchingVolumes.any { it.owned }
                val existingSameId = matchingVolumes.firstOrNull { it.id == volume.id }
                val mergedVolume = (existingSameId ?: volume).copy(
                    id = existingSameId?.id ?: volume.id,
                    mangaId = volume.mangaId,
                    title = volume.title,
                    volume = volume.volume,
                    coverUrl = volume.coverUrl,
                    locale = volume.locale,
                    owned = owned,
                    isSpecialEdition = volume.isSpecialEdition,
                    createdAt = volume.createdAt,
                    updatedAt = volume.updatedAt,
                )

                if (existingSameId != null) {
                    volumesToUpdate.add(mergedVolume)
                    volumeIdsToDelete += matchingVolumes
                        .filter { it.id != existingSameId.id }
                        .map { it.id }
                } else {
                    volumesToInsert.add(mergedVolume)
                    volumeIdsToDelete += matchingVolumes.map { it.id }
                }

                localVolumesByManga[volume.mangaId]?.removeAll { local ->
                    matchingVolumes.any { it.id == local.id }
                }
                localVolumesByManga[volume.mangaId]?.add(mergedVolume)
            } else {
                volumesToInsert.add(volume)
                localVolumesByManga[volume.mangaId]?.add(volume)
            }
        }
        volumeIdsToDelete.forEach { volumeId ->
            try {
                db.volumeDao().deleteVolumeById(volumeId)
            } catch (e: Exception) {
                logger.logError(e)
            }
        }
        if (volumesToUpdate.isNotEmpty()) {
            try {
                db.volumeDao().updateVolumeList(volumesToUpdate)
            } catch (e: Exception) {
                logger.logError(e)
            }
        }
        if (volumesToInsert.isNotEmpty()) {
            try {
                db.volumeDao().insertVolumeList(volumesToInsert)
            } catch (e: Exception) {
                logger.logError(e)
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

    private fun List<Volume>.deduplicateForPersistence(): List<Volume> {
        val (numbered, unnumbered) = partition { it.volume != null }
        val deduplicatedNumbered = numbered
            .groupBy { Triple(it.mangaId, it.volume, it.locale) }
            .map { (_, volumes) -> volumes.preferredVolume() }

        return deduplicatedNumbered + unnumbered.distinctBy { it.id }
    }

    private fun List<Volume>.preferredVolume(): Volume {
        return maxWithOrNull(
            compareBy<Volume> { it.updatedAt.orEmpty() }
                .thenBy { it.owned }
                .thenByDescending { it.id }
        ) ?: first()
    }

    override suspend fun getMangaIdsWithSpecialEditions(): List<String> {
        return try {
            db.volumeDao().getMangaIdsWithSpecialEditions()
        } catch (e: Exception) {
            logger.logError(e)
            emptyList()
        }
    }

    override fun log(message: Exception) {
        message.printStackTrace()
        logger.log("Library LOG: $message")
    }
}
