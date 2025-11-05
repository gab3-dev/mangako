package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.model.Manga
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

    override suspend fun getMangaOnLibrary(): List<Manga> {
        return db.mangaDao().getAllManga().filter { it.isOnUserLibrary }
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

    override fun log(message: Exception) {
        message.printStackTrace()
        logger.log("Library LOG: $message")
    }
}