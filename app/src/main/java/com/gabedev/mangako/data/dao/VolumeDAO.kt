package com.gabedev.mangako.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gabedev.mangako.data.model.Volume

@Dao
interface VolumeDAO {
    // Methods for managing manga volumes
    @Query("SELECT * FROM Volume WHERE id = :id")
    suspend fun getVolumeById(id: String): Volume?

    @Insert
    suspend fun insertVolume(
        volume: Volume
    ): Long

    @Update
    suspend fun updateVolumeOwnership(
        volume: Volume
    ): Int

    @Delete
    suspend fun deleteVolumeById(
        volume: Volume
    ): Int
}