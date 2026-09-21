package com.gabedev.mangako.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MangaKoDatabaseTest {
    @Test
    fun `migration 4 to 5 recreates MangaWithOwned with Room expected SQL`() {
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        MangaKoDatabase.MIGRATION_4_5.migrate(database)

        verify {
            database.execSQL(
                """
                |CREATE VIEW `MangaWithOwned` AS SELECT${" "}
                |            M.id AS id,
                |            M.title AS title,
                |            M.alt_title AS altTitle,
                |            M.type AS type,
                |            M.cover_id AS coverId,
                |            M.cover_file_name AS coverFileName,
                |            IFNULL(M.cover_url, '') AS coverUrl,
                |            M.author_id AS authorId,
                |            M.author AS author,
                |            IFNULL(M.description, '') AS description,
                |            M.status AS status,
                |            IFNULL(M.volume_count, 0) AS volumeCount,
                |            M.original_language AS originalLanguage,
                |            IFNULL(M.on_user_library, 0) AS isOnUserLibrary,
                |            COUNT(V.id) AS volumeOwned
                |        FROM Manga M
                |        LEFT JOIN Volume V${" "}
                |            ON V.manga_id = M.id AND V.owned = 1
                |        GROUP BY M.id
                """.trimMargin()
            )
        }
    }
}
