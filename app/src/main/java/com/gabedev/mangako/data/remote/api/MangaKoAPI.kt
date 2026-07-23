package com.gabedev.mangako.data.remote.api

import com.gabedev.mangako.data.dto.MangaKoMangaDto
import com.gabedev.mangako.data.dto.MangaKoVolumeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaKoAPI {
    @GET("mangas")
    suspend fun searchMangas(
        @Query("title") title: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): List<MangaKoMangaDto>

    @GET("mangas/{mangaRef}")
    suspend fun getManga(
        @Path("mangaRef") mangaRef: String,
        @Query("refresh") refresh: Boolean = false,
    ): MangaKoMangaDto

    @GET("mangas/{mangaRef}/volumes")
    suspend fun getVolumes(
        @Path("mangaRef") mangaRef: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("refresh") refresh: Boolean = false,
    ): List<MangaKoVolumeDto>
}
