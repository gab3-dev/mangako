package com.gabedev.mangako.domain

data class LibrarySyncResult(
    val updatedCount: Int,
    val mangaCount: Int,
    val failedCount: Int,
)
