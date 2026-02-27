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
        manga?.let {
            val updatedManga = it.copy(isOnUserLibrary = false)
            db.mangaDao().updateMangaLibraryStatus(updatedManga)
        }
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
        val volumesToUpdate = mutableListOf<Volume>()
        val volumesToInsert = mutableListOf<Volume>()
        for (volume in volumeList) {
            val existingVolume = db.volumeDao().getVolumeById(volume.id)
            if (existingVolume != null) {
                val mergeVolume: Volume = existingVolume.copy(
                    title = volume.title,
                    volume = volume.volume,
                    coverUrl = volume.coverUrl,
                )
                volumesToUpdate.add(mergeVolume)
            } else {
                volumesToInsert.add(volume)
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