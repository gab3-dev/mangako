package com.gabedev.mangako.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Volume(
    @PrimaryKey val                         id: String,
    @ColumnInfo(name = "manga_id")          val mangaId: String, // chave estrangeira
    @ColumnInfo(name = "title")             val title: String,
    @ColumnInfo(name = "cover_url")         val coverUrl: String,
    @ColumnInfo(name = "volume")            val volume: Int?,
    @ColumnInfo(name = "owned")             val owned: Boolean = false
)
