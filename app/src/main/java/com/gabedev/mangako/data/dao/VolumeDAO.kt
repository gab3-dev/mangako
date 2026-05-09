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

    @Query(
        """
        SELECT DISTINCT manga_id FROM Volume
        WHERE is_special_edition = 1
        OR locale != 'ja'
        OR volume IS NULL
        OR (CAST(volume AS TEXT) LIKE '%.%' AND CAST(volume AS TEXT) NOT LIKE '%.0')
        """
    )
    suspend fun getMangaIdsWithSpecialEditions(): List<String>

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

    @Update
    suspend fun updateVolumeList(
        volumes: List<Volume>
    ): Int

    @Delete
    suspend fun deleteVolume(volume: Volume): Int

    @Query("DELETE FROM Volume WHERE id = :id")
    suspend fun deleteVolumeById(id: String): Int
}
