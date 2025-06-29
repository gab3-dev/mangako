package com.gabedev.mangako.data.dto

data class MangaListResponse(
    val result: String,
    val response: String,
    val data: List<MangaDto>,
    val limit: Int,
    val offset: Int,
    val total: Int
)