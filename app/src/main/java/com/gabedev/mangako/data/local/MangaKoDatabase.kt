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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Volume ADD COLUMN created_at TEXT")
                db.execSQL("DROP INDEX IF EXISTS index_Volume_manga_id_volume_cover_url")

                db.execSQL(
                    """
                    CREATE TEMP TABLE volume_owned_groups AS
                    SELECT manga_id, volume, locale
                    FROM Volume
                    WHERE volume IS NOT NULL
                    GROUP BY manga_id, volume, locale
                    HAVING MAX(owned) = 1
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    DELETE FROM Volume
                    WHERE volume IS NOT NULL
                    AND EXISTS (
                        SELECT 1
                        FROM Volume better
                        WHERE better.manga_id = Volume.manga_id
                        AND better.volume = Volume.volume
                        AND better.locale = Volume.locale
                        AND better.id != Volume.id
                        AND (
                            (
                                better.updated_at IS NOT NULL
                                AND better.updated_at != ''
                                AND (
                                    Volume.updated_at IS NULL
                                    OR Volume.updated_at = ''
                                    OR better.updated_at > Volume.updated_at
                                )
                            )
                            OR (
                                COALESCE(better.updated_at, '') = COALESCE(Volume.updated_at, '')
                                AND better.owned > Volume.owned
                            )
                            OR (
                                COALESCE(better.updated_at, '') = COALESCE(Volume.updated_at, '')
                                AND better.owned = Volume.owned
                                AND better.id < Volume.id
                            )
                        )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE Volume
                    SET owned = 1
                    WHERE volume IS NOT NULL
                    AND EXISTS (
                        SELECT 1
                        FROM volume_owned_groups owned_group
                        WHERE owned_group.manga_id = Volume.manga_id
                        AND owned_group.volume = Volume.volume
                        AND owned_group.locale = Volume.locale
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE volume_owned_groups")
                db.execSQL(
                    """
                    UPDATE Volume
                    SET is_special_edition = CASE
                        WHEN locale != 'ja'
                        OR volume IS NULL
                        OR (
                            CAST(volume AS TEXT) LIKE '%.%'
                            AND CAST(volume AS TEXT) NOT LIKE '%.0'
                        )
                        THEN 1
                        ELSE 0
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_Volume_manga_id_volume_locale
                    ON Volume(manga_id, volume, locale)
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        this.db = db
    }
}
