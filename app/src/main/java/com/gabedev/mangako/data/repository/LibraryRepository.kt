package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithVolume

interface LibraryRepository {
    suspend fun getAllManga(): List<Manga>
    suspend fun getMangaWithVolume(mangaId: String): MangaWithVolume?
    suspend fun searchManga(title: String): List<Manga>
    suspend fun addMangaToLibrary(manga: Manga)
    suspend fun removeMangaFromLibrary(mangaId: String)
    suspend fun removeMangaFromLibrary(manga: Manga)
    suspend fun isMangaInLibrary(mangaId: String): Boolean
}