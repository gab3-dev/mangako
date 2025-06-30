package com.gabedev.mangako.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class Manga (
    @PrimaryKey val                         id: String,
    @ColumnInfo(name = "title")             val title: String,
    @ColumnInfo(name = "alt_title")         val altTitle: String? = null,
    @ColumnInfo(name = "type")              val type: String? = null,
    @ColumnInfo(name = "cover_id")          val coverId: String? = null,
    @ColumnInfo(name = "cover_file_name")   val coverFileName: String? = null,
    @ColumnInfo(name = "cover_url")         val coverUrl: String,
    @ColumnInfo(name = "author_id")         val authorId: String? = null,
    @ColumnInfo(name = "author")            val author: String? = null,
    @ColumnInfo(name = "description")       val description: String,
    @ColumnInfo(name = "status")            val status: String? = null,
    @ColumnInfo(name = "volume_count")      val volumeCount: Int = 0,
    @ColumnInfo(name = "on_user_library")   val isOnUserLibrary: Boolean = false
)