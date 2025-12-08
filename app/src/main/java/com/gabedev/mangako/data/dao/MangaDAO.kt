package com.gabedev.mangako.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.MangaWithVolume

@Dao
interface MangaDAO {
    @Query("SELECT * FROM Manga")
    suspend fun getAllManga(): List<Manga>

    @Query("SELECT * FROM Manga WHERE id = :id")
    suspend fun getMangaById(id: String): Manga?

    @Transaction
    @Query("SELECT * FROM Manga WHERE id = :id")
    suspend fun getMangaWithVolumeById(id: String): MangaWithVolume?

    @Query("SELECT * FROM Manga WHERE title LIKE '%' || :title || '%'")
    suspend fun searchMangaByTitle(title: String): List<Manga>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(
        manga: Manga
    ): Long

    @Update
    suspend fun updateManga(
        manga: Manga
    ): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMangaLibraryStatus(
        manga: Manga
    ): Int

    @Query("DELETE FROM Manga WHERE id = :id")
    suspend fun deleteMangaById(id: String): Int

    @Delete
    suspend fun deleteManga(
        manga: Manga
    ): Int
}