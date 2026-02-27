package com.gabedev.mangako.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MangaKoDatabase(
    context: Context,
) {
    private val db: LocalDatabase

    fun getDatabase(): LocalDatabase {
        return db
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Volume ADD COLUMN is_special_edition INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE Volume SET is_special_edition = 1 
                    WHERE volume IS NOT NULL 
                    AND CAST(volume AS TEXT) LIKE '%.%' 
                    AND CAST(volume AS TEXT) NOT LIKE '%.0'
                    """.trimIndent()
                )
            }
        }
    }

    init {
        val db = Room.databaseBuilder(
            context.applicationContext,
            LocalDatabase::class.java,
            name = "mangako_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
        this.db = db
    }
}