package com.gabedev.mangako.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject

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
    val coverLanguage: String? = null,
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

@Serializable
internal data class BackupV3Document(
    val formatVersion: Int = 3,
    val createdAt: String,
    val appVersionCode: Int,
    val payload: BackupV3Payload,
    val checksumSha256: String,
)

@Serializable
internal data class BackupV3Payload(
    val collection: List<BackupV3Manga>,
    val settings: BackupV3Settings,
)

@Serializable
internal data class BackupV3Manga(
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
    val coverLanguage: String? = null,
    val volumes: List<BackupV3Volume>,
)

@Serializable
internal data class BackupV3Volume(
    val id: String,
    val mangaId: String,
    val title: String,
    val coverUrl: String = "",
    val number: String? = null,
    val locale: String,
    val isSpecialEdition: Boolean,
    val owned: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
internal data class BackupV3Settings(
    val shared: BackupSharedSettings,
    val android: BackupAndroidSettings,
    val desktop: JsonObject = JsonObject(emptyMap()),
)

@Serializable
internal data class BackupSharedSettings(
    val catalogIntegration: String,
    val backupFrequency: String,
)

@Serializable
internal data class BackupAndroidSettings(
    val viewMode: String,
    val collectionDensity: Int,
    val navigationBarStyle: String,
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
