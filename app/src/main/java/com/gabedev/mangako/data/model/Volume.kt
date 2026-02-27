package com.gabedev.mangako.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    indices = [
        androidx.room.Index(value = ["manga_id", "volume", "cover_url"], unique = true)
    ]
)
data class Volume(
    @PrimaryKey val                         id: String,
    @ColumnInfo(name = "manga_id")          val mangaId: String, // chave estrangeira
    @ColumnInfo(name = "title")             val title: String,
    @ColumnInfo(name = "cover_url")         val coverUrl: String,
    @ColumnInfo(name = "volume")            val volume: Float?,
    @ColumnInfo(name = "owned")             val owned: Boolean = false,
    @ColumnInfo(name = "updated_at")        val updatedAt: String? = null
)
