package com.gabedev.mangako.backup

import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.NavigationBarStyle
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

class BackupFormat {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(
        payload: BackupPayload,
        createdAt: String,
        appVersionCode: Int,
    ): ByteArray {
        val document = BackupDocument(
            formatVersion = FORMAT_VERSION,
            createdAt = createdAt,
            appVersionCode = appVersionCode,
            payload = payload,
            checksumSha256 = checksum(payload, FORMAT_VERSION),
        )
        return json.encodeToString(document).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): ParsedBackup {
        val document = json.decodeFromString<BackupDocument>(bytes.decodeToString())
        validate(document)
        return ParsedBackup(document)
    }

    internal fun validate(document: BackupDocument) {
        require(document.formatVersion in MIN_FORMAT_VERSION..FORMAT_VERSION) {
            "Unsupported backup version"
        }
        require(document.createdAt.isNotBlank()) { "Backup date is missing" }
        require(document.payload.collection.size <= MAX_MANGA) { "Too many manga entries" }
        require(document.payload.collection.sumOf { it.volumes.size } <= MAX_VOLUMES) {
            "Too many volume entries"
        }
        require(checksum(document.payload, document.formatVersion) == document.checksumSha256) {
            "Backup checksum is invalid"
        }

        val mangaIds = mutableSetOf<String>()
        val volumeIds = mutableSetOf<String>()
        document.payload.collection.forEach { manga ->
            require(manga.id.isNotBlank() && manga.id.length <= MAX_TEXT_LENGTH) { "Invalid manga ID" }
            require(manga.title.isNotBlank() && manga.title.length <= MAX_TEXT_LENGTH) { "Invalid manga title" }
            require(mangaIds.add(manga.id)) { "Duplicate manga ID" }
            val volumeKeys = mutableSetOf<String>()
            manga.volumes.forEach { volume ->
                require(volume.id.isNotBlank() && volume.id.length <= MAX_TEXT_LENGTH) { "Invalid volume ID" }
                require(volumeIds.add(volume.id)) { "Duplicate volume ID" }
                require(volume.mangaId == manga.id) { "Volume references another manga" }
                require(volume.locale.isNotBlank() && volume.locale.length <= 32) { "Invalid volume locale" }
                require(volume.number?.isFinite() != false) { "Invalid volume number" }
                val key = volume.number?.let { "number:$it:${volume.locale}" } ?: "id:${volume.id}"
                require(volumeKeys.add(key)) { "Duplicate volume" }
            }
        }
        require(runCatching { CatalogIntegration.valueOf(document.payload.settings.catalogIntegration) }.isSuccess) {
            "Invalid catalog setting"
        }
        require(runCatching { NavigationBarStyle.valueOf(document.payload.settings.navigationBarStyle) }.isSuccess) {
            "Invalid navigation setting"
        }
        require(runCatching { BackupFrequency.valueOf(document.payload.settings.backupFrequency) }.isSuccess) {
            "Invalid backup frequency"
        }
    }

    private fun checksum(payload: BackupPayload, formatVersion: Int): String {
        val payloadJson = json.encodeToJsonElement(payload).let { element ->
            if (formatVersion == 1) element.toLegacyFormat() else element
        }.toString()
        return MessageDigest.getInstance("SHA-256")
            .digest(payloadJson.encodeToByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun JsonElement.toLegacyFormat(): JsonElement {
        return when (this) {
            is JsonObject -> JsonObject(
                buildMap {
                    for ((key, value) in this@toLegacyFormat) {
                        if (key != "owned") {
                            put(
                                if (key == "volumes") "ownedVolumes" else key,
                                value.toLegacyFormat(),
                            )
                        }
                    }
                }
            )
            is JsonArray -> JsonArray(map { it.toLegacyFormat() })
            else -> this
        }
    }

    companion object {
        const val FORMAT_VERSION = 2
        private const val MIN_FORMAT_VERSION = 1
        private const val MAX_MANGA = 10_000
        private const val MAX_VOLUMES = 100_000
        private const val MAX_TEXT_LENGTH = 2_000
    }
}
