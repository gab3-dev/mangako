package com.gabedev.mangako.data.api

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
}