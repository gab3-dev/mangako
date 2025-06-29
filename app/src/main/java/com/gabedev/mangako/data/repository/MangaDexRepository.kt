package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga

interface MangaDexRepository {
    suspend fun searchManga(title: String): List<Manga>
    suspend fun getManga(id: String): Manga
    suspend fun getMangaCoverFileName(id: String): String
    suspend fun getAuthorNameById(id: String): String
}