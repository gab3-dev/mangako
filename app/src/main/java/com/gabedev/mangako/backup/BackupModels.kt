package com.gabedev.mangako.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

@Serializable
data class BackupDocument(
    val formatVersion: Int,
    val createdAt: String,
    val appVersionCode: Int,
    val payload: BackupPayload,
    val checksumSha256: String,
)

@Serializable
data class BackupPayload(
    val collection: List<BackupManga>,
    val settings: BackupSettings,
)

@Serializable
data class BackupManga(
    val id: String,
    val title: String,
    val altTitle: String? = null,
    val type: String? = null,
    val coverId: String? = null,
    val coverFileName: String? = null,
    val coverUrl: String = "",
    val authorId: String? = null,
    val author: String? = null,
    val status: String? = null,
    val volumeCount: Int = 0,
    val originalLanguage: String? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("ownedVolumes")
    val volumes: List<BackupVolume>,
)

@Serializable
data class BackupVolume(
    val id: String,
    val mangaId: String,
    val title: String,
    val coverUrl: String = "",
    val number: Float? = null,
    val locale: String,
    val isSpecialEdition: Boolean,
    val owned: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class BackupSettings(
    val viewMode: String,
    val collectionDensity: Int,
    val catalogIntegration: String,
    val navigationBarStyle: String,
    val backupFrequency: String,
)

enum class RestoreMode {
    MERGE,
    REPLACE,
}

data class BackupPreview(
    val createdAt: String,
    val mangaCount: Int,
    val volumeCount: Int,
    val ownedVolumeCount: Int,
)

class ParsedBackup internal constructor(
    internal val document: BackupDocument,
) {
    val preview = BackupPreview(
        createdAt = document.createdAt,
        mangaCount = document.payload.collection.size,
        volumeCount = document.payload.collection.sumOf { it.volumes.size },
        ownedVolumeCount = document.payload.collection.sumOf { manga ->
            manga.volumes.count { it.owned }
        },
    )
}
