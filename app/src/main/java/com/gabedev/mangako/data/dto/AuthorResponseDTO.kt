package com.gabedev.mangako.data.dto

data class AuthorResponseDTO(
    val result: String,
    val response: String,
    val data: AuthorData
)

data class AuthorData(
    val id: String,
    val type: String,
    val attributes: AuthorAttributes,
    val relationships: List<Relationship>
)

data class AuthorAttributes(
    val name: String,
    val imageUrl: String? = null,
    val biography: Map<String, String>? = null,
    val twitter: String? = null,
    val pixiv: String? = null,
    val melonBook: String? = null,
    val fanBox: String? = null,
    val booth: String? = null,
    val namicomi: String? = null,
    val nicoVideo: String? = null,
    val skeb: String? = null,
    val fantia: String? = null,
    val tumblr: String? = null,
    val youtube: String? = null,
    val weibo: String? = null,
    val naver: String? = null,
    val website: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val version: Int
)