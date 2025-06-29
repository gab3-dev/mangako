package com.gabedev.mangako.data.remote.api

import com.gabedev.mangako.data.dto.AuthorResponseDTO
import com.gabedev.mangako.data.dto.CoverArtListResponseDTO
import com.gabedev.mangako.data.dto.CoverArtResponseDTO
import com.gabedev.mangako.data.dto.MangaDto
import com.gabedev.mangako.data.dto.MangaListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexAPI {

    @GET("manga/{id}")
    suspend fun getManga(@Path("id") id: String): MangaDto

    @GET("manga")
    suspend fun searchMangas(
        @Query("limit") limit: Int = 10,
        @Query("title") title: String
    ): MangaListResponse

    @GET("cover")
    suspend fun getCover(
        @Query("limit") limit: Int = 50,
        @Query("manga[]") manga: List<String>,
        @Query("locales[]") locales: List<String> = listOf("ja"),
        @Query("order[createdAt]") orderCreatedAt: String? = null,
        @Query("order[updatedAt]") orderUpdatedAt: String? = null,
        @Query("order[volume]") orderVolume: String = "asc",
    ): CoverArtListResponseDTO

    @GET("cover/{id}")
    suspend fun getCoverById(@Path("id") id: String): CoverArtResponseDTO

    @GET("author/{id}")
    suspend fun getAuthorById(@Path("id") id: String): AuthorResponseDTO
}