package com.gabedev.mangako.data.model

import androidx.room.DatabaseView

@DatabaseView(
    viewName = "MangaWithOwned",
    value = """
        SELECT 
            M.id AS id,
            M.title AS title,
            M.alt_title AS altTitle,
            M.type AS type,
            M.cover_id AS coverId,
            M.cover_file_name AS coverFileName,
            IFNULL(M.cover_url, '') AS coverUrl,
            M.author_id AS authorId,
            M.author AS author,
            IFNULL(M.description, '') AS description,
            M.status AS status,
            IFNULL(M.volume_count, 0) AS volumeCount,
            IFNULL(M.on_user_library, 0) AS isOnUserLibrary,
            COUNT(V.id) AS volumeOwned
        FROM Manga M
        LEFT JOIN Volume V 
            ON V.manga_id = M.id AND V.owned = 1
        GROUP BY M.id
    """
)

data class MangaWithOwned(
    val id: String,
    val title: String,
    val altTitle: String?,
    val type: String?,
    val coverId: String?,
    val coverFileName: String?,
    val coverUrl: String,
    val authorId: String?,
    val author: String?,
    val description: String,
    val status: String?,
    val volumeCount: Int,
    val isOnUserLibrary: Boolean,
    val volumeOwned: Int
)

fun MangaWithOwned.toManga(): Manga {
    return Manga(
        id = this.id,
        title = this.title,
        altTitle = this.altTitle,
        type = this.type,
        coverId = this.coverId,
        coverFileName = this.coverFileName,
        coverUrl = this.coverUrl,
        authorId = this.authorId,
        author = this.author,
        description = this.description,
        status = this.status,
        volumeCount = this.volumeCount,
        isOnUserLibrary = this.isOnUserLibrary
    )
}