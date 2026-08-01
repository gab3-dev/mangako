package com.gabedev.mangako.domain

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume

data class LibrarySyncResult(
    val updatedCount: Int,
    val mangaCount: Int,
    val failedCount: Int,
    val newVolumesByManga: List<MangaNewVolumes> = emptyList(),
)

data class MangaNewVolumes(
    val manga: Manga,
    val volumes: List<Volume>,
)
