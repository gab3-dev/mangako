package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithVolume
import com.gabedev.mangako.data.model.Volume

interface LibraryRepository {
    suspend fun getManga(mangaId: String): Manga?
    suspend fun getAllManga(): List<Manga>
    suspend fun getMangaOnLibrary(): List<Manga>
    suspend fun getMangaWithVolume(mangaId: String): MangaWithVolume?
    suspend fun searchManga(title: String): List<Manga>
    suspend fun insertManga(manga: Manga)
    suspend fun addMangaToLibrary(manga: Manga)
    suspend fun removeMangaFromLibrary(mangaId: String)
    suspend fun removeMangaFromLibrary(manga: Manga)
    suspend fun isMangaInLibrary(mangaId: String): Boolean
    suspend fun insertVolumeList(volumeList: List<Volume>)
    suspend fun updateVolume(volume: Volume)
}