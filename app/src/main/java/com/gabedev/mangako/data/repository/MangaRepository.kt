package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga

interface MangaRepository {
    suspend fun searchManga(title: String): List<Manga>
    suspend fun getManga(id: String): Manga
}