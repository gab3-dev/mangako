package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithVolume

class LibraryRepositoryImpl (
    private val db: LocalDatabase
) : LibraryRepository {
    override suspend fun getAllManga(): List<Manga> {
        return db.mangaDao().getAllManga()
    }

    override suspend fun getMangaWithVolume(mangaId: String): MangaWithVolume? {
        return db.mangaDao().getMangaWithVolumeById(mangaId)
    }

    override suspend fun searchManga(title: String): List<Manga> {
        return db.mangaDao().searchMangaByTitle(title)
    }

    override suspend fun addMangaToLibrary(manga: Manga) {
        db.mangaDao().insertManga(manga)
    }

    override suspend fun removeMangaFromLibrary(mangaId: String) {
        db.mangaDao().deleteMangaById(mangaId)
    }

    override suspend fun removeMangaFromLibrary(manga: Manga) {
        db.mangaDao().deleteManga(manga)
    }

    override suspend fun isMangaInLibrary(mangaId: String): Boolean {
        return db.mangaDao().getMangaById(mangaId) != null
    }
}