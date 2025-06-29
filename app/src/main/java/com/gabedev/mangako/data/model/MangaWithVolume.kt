package com.gabedev.mangako.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MangaWithVolume(
    @Embedded val manga: Manga,
    @Relation(
        parentColumn = "id",
        entityColumn = "manga_id"
    )
    val volumes: List<Volume>
)