package com.gabedev.mangako.data.dto

import com.google.gson.annotations.SerializedName

data class MangaKoMangaDto(
    val id: String,
    @SerializedName("mangadexId") val mangaDexId: String?,
    @SerializedName("primaryTitle") val primaryTitle: String,
    val status: String?,
    @SerializedName("latestVolumeNumber") val latestVolumeNumber: String?,
    val localizations: List<MangaKoLocalizationDto>,
    val aliases: List<MangaKoAliasDto>,
    val covers: List<MangaKoCoverDto>,
    val authors: List<MangaKoCreatorDto>,
    @SerializedName("originalLanguage") val originalLanguage: String? = null,
)

data class MangaKoLocalizationDto(
    val language: String,
    val title: String?,
    val description: String?,
    @SerializedName("isPrimary") val isPrimary: Boolean,
)

data class MangaKoAliasDto(
    val language: String,
    val title: String,
)

data class MangaKoCoverDto(
    val id: String,
    @SerializedName("mangadexCoverId") val mangaDexCoverId: String?,
    @SerializedName("isPrimary") val isPrimary: Boolean,
    @SerializedName("sourceUrl") val sourceUrl: String?,
)

data class MangaKoCreatorDto(
    val id: String,
    val name: String,
)

data class MangaKoVolumeDto(
    val id: String,
    @SerializedName("mangadexCoverId") val mangaDexCoverId: String?,
    @SerializedName("sourceUrl") val sourceUrl: String,
    val volume: String?,
    val locale: String,
    @SerializedName("isSpecialEdition") val isSpecialEdition: Boolean,
    @SerializedName("sourceCreatedAt") val sourceCreatedAt: String?,
    @SerializedName("sourceUpdatedAt") val sourceUpdatedAt: String?,
    @SerializedName("updatedAt") val updatedAt: String,
)
