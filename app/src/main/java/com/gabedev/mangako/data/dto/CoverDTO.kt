package com.gabedev.mangako.data.dto

data class CoverArtListResponseDTO(
    val result: String,
    val response: String,
    val data: List<CoverArtDTO>,
    val limit: Int,
    val offset: Int,
    val total: Int
)


data class CoverArtDTO(
    val id: String,
    val type: String,
    val attributes: CoverArtAttributesDTO,
)

data class CoverArtAttributesDTO(
    val description: String,
    val volume: String,
    val fileName: String,
    val locale: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Int
)

data class CoverArtResponseDTO(
    val result: String,
    val response: String,
    val data: CoverArtData
)

data class CoverArtData(
    val id: String,
    val type: String,
    val attributes: CoverArtAttributes,
    val relationships: List<Relationship>
)

data class CoverArtAttributes(
    val description: String,
    val volume: String?,
    val fileName: String,
    val locale: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Int
)

data class Relationship(
    val id: String,
    val type: String
)