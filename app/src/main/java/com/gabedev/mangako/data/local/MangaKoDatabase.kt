package com.gabedev.mangako.data.local

import android.content.Context
import androidx.room.Room

class MangaKoDatabase(
    context: Context,
) {
    private val db: LocalDatabase

    fun getDatabase(): LocalDatabase {
        return db
    }

    init {
        val db = Room.databaseBuilder(
            context.applicationContext,
            LocalDatabase::class.java,
            name = "mangako_database"
        ).build()
        this.db = db
    }
}