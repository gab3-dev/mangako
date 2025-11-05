package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume

interface MangaDexRepository {
    suspend fun searchManga(title: String, offset: Int? = null): List<Manga>
    suspend fun getManga(id: String): Manga
    suspend fun getMangaCoverFileName(id: String): String
    suspend fun getAuthorNameById(id: String): String
    suspend fun getCoverListByManga(manga: Manga, offset: Int? = null, limit: Int = 50): List<Volume>
    fun log(message: Exception)
}