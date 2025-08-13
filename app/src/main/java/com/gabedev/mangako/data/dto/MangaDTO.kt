package com.gabedev.mangako.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MangaDto(
    val id: String,
    val type: String?,
    val attributes: AttributesDto,
    val relationships: List<RelationshipDto>
)

@Serializable
data class AttributesDto(
    val title: Map<String, String?>?,
    val altTitles: List<Map<String, String?>>?,
    val description: Map<String, String?>?,
    val isLocked: Boolean,
    val links: LinksDto,
    val originalLanguage: String?,
    val lastVolume: String?,
    val lastChapter: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val contentRating: String?,
    val tags: List<TagDto>,
    val state: String?,
    val chapterNumbersResetOnNewVolume: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val version: Int?,
    val availableTranslatedLanguages: List<String?>,
    val latestUploadedChapter: String?
)

@Serializable
data class LinksDto(
    val al: String?,
    val ap: String?,
    val bw: String?,
    val kt: String?,
    val mu: String?,
    val mal: String?,
    val raw: String?
)

@Serializable
data class TagDto(
    val id: String?,
    val type: String?,
    val attributes: TagAttributesDto
)

@Serializable
data class TagAttributesDto(
    val name: Map<String, String?>,
    val description: Map<String, String?> = emptyMap(),
    val group: String?,
    val version: Int?
)

@Serializable
data class RelationshipDto(
    val id: String?,
    val type: String?
)