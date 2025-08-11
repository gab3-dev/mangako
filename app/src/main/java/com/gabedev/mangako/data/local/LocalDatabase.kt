package com.gabedev.mangako.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gabedev.mangako.data.dao.MangaDAO
import com.gabedev.mangako.data.dao.VolumeDAO
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume

@Database(entities = [Manga::class, Volume::class], version = 2)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDAO
    abstract fun volumeDao(): VolumeDAO
}