package com.gabedev.mangako.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabedev.mangako.data.model.Volume

@Dao
interface VolumeDAO {
    @Query("SELECT * FROM Volume WHERE id = :id")
    suspend fun getVolumeById(id: String): Volume?

    @Query("SELECT * FROM Volume WHERE manga_id = :mangaId")
    suspend fun getVolumesByMangaId(mangaId: String): List<Volume>

    @Insert
    suspend fun insertVolume(
        volume: Volume
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVolumeList(
        volumes: List<Volume>
    )

    @Update
    suspend fun updateVolume(
        volume: Volume
    ): Int

    @Delete
    suspend fun deleteVolumeById(
        volume: Volume
    ): Int
}