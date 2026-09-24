package com.gabedev.mangako.backup

import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.NavigationBarStyle
import java.math.BigDecimal
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class BackupFormat {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: BackupPayload, createdAt: String, appVersionCode: Int): ByteArray {
        val v3Payload = payload.toV3()
        val document = BackupV3Document(
            createdAt = createdAt,
            appVersionCode = appVersionCode,
            payload = v3Payload,
            checksumSha256 = checksumV3(v3Payload),
        )
        return json.encodeToString(document).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): ParsedBackup {
        val root = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val version = root["formatVersion"]?.let { json.decodeFromJsonElement<Int>(it) }
            ?: throw IllegalArgumentException("Backup version is missing")
        require(version in MIN_FORMAT_VERSION..FORMAT_VERSION) { "Unsupported backup version" }
        val document = if (version == FORMAT_VERSION) {
            val v3 = json.decodeFromJsonElement<BackupV3Document>(root)
            validateV3(v3)
            v3.toDocument()
        } else {
            val legacy = json.decodeFromJsonElement<BackupDocument>(root)
            validate(legacy)
            legacy
        }
        return ParsedBackup(document)
    }

    internal fun validate(document: BackupDocument) {
        require(document.formatVersion in MIN_FORMAT_VERSION..LEGACY_FORMAT_VERSION) {
            "Unsupported backup version"
        }
        validatePayload(document.payload)
        require(checksumLegacy(document.payload, document.formatVersion) == document.checksumSha256) {
            "Backup checksum is invalid"
        }
    }

    private fun validateV3(document: BackupV3Document) {
        require(document.formatVersion == FORMAT_VERSION) { "Unsupported backup version" }
        require(document.createdAt.isNotBlank()) { "Backup date is missing" }
        document.payload.collection.flatMap { it.volumes }.forEach { volume ->
            volume.number?.let { number ->
                val parsed = runCatching { BigDecimal(number) }.getOrNull()
                require(parsed != null && parsed.toFloat().isFinite()) { "Invalid volume number" }
                require(parsed.stripTrailingZeros().toPlainString() == number) { "Invalid volume number" }
            }
        }
        val payload = document.payload.toPayload()
        validatePayload(payload)
        require(checksumV3(document.payload) == document.checksumSha256) {
            "Backup checksum is invalid"
        }
    }

    private fun validatePayload(payload: BackupPayload) {
        require(payload.collection.size <= MAX_MANGA) { "Too many manga entries" }
        require(payload.collection.sumOf { it.volumes.size } <= MAX_VOLUMES) { "Too many volume entries" }

        val mangaIds = mutableSetOf<String>()
        val volumeIds = mutableSetOf<String>()
        payload.collection.forEach { manga ->
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
        require(runCatching { CatalogIntegration.valueOf(payload.settings.catalogIntegration) }.isSuccess) {
            "Invalid catalog setting"
        }
        require(runCatching { NavigationBarStyle.valueOf(payload.settings.navigationBarStyle) }.isSuccess) {
            "Invalid navigation setting"
        }
        require(runCatching { BackupFrequency.valueOf(payload.settings.backupFrequency) }.isSuccess) {
            "Invalid backup frequency"
        }
    }

    private fun checksumLegacy(payload: BackupPayload, formatVersion: Int): String {
        val element = json.encodeToJsonElement(payload).let {
            if (formatVersion == 1) it.toLegacyFormat() else it
        }
        return sha256(element.toString())
    }

    private fun checksumV3(payload: BackupV3Payload): String =
        sha256(json.encodeToJsonElement(payload).canonicalJson())

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun JsonElement.canonicalJson(): String = when (this) {
        is JsonObject -> entries.sortedBy { it.key }.joinToString(prefix = "{", postfix = "}") {
            "${JsonPrimitive(it.key)}:${it.value.canonicalJson()}"
        }
        is JsonArray -> joinToString(prefix = "[", postfix = "]") { it.canonicalJson() }
        else -> toString()
    }

    private fun JsonElement.toLegacyFormat(): JsonElement = when (this) {
        is JsonObject -> JsonObject(buildMap {
            for ((key, value) in this@toLegacyFormat) {
                if (key != "owned" && key != "coverLanguage") {
                    put(if (key == "volumes") "ownedVolumes" else key, value.toLegacyFormat())
                }
            }
        })
        is JsonArray -> JsonArray(map { it.toLegacyFormat() })
        else -> this
    }

    private fun BackupPayload.toV3() = BackupV3Payload(
        collection = collection.sortedBy { it.id }.map { manga ->
            BackupV3Manga(
                id = manga.id, title = manga.title, altTitle = manga.altTitle, type = manga.type,
                coverId = manga.coverId, coverFileName = manga.coverFileName, coverUrl = manga.coverUrl,
                authorId = manga.authorId, author = manga.author, status = manga.status,
                volumeCount = manga.volumeCount, originalLanguage = manga.originalLanguage,
                coverLanguage = manga.coverLanguage,
                volumes = manga.volumes.sortedBy { it.id }.map { volume ->
                    BackupV3Volume(
                        id = volume.id, mangaId = volume.mangaId, title = volume.title,
                        coverUrl = volume.coverUrl, number = volume.number?.normalizedNumber(),
                        locale = volume.locale, isSpecialEdition = volume.isSpecialEdition, owned = volume.owned,
                        createdAt = volume.createdAt, updatedAt = volume.updatedAt,
                    )
                },
            )
        },
        settings = BackupV3Settings(
            shared = BackupSharedSettings(settings.catalogIntegration, settings.backupFrequency),
            android = BackupAndroidSettings(settings.viewMode, settings.collectionDensity, settings.navigationBarStyle),
        ),
    )

    private fun BackupV3Payload.toPayload() = BackupPayload(
        collection = collection.map { manga ->
            BackupManga(
                id = manga.id, title = manga.title, altTitle = manga.altTitle, type = manga.type,
                coverId = manga.coverId, coverFileName = manga.coverFileName, coverUrl = manga.coverUrl,
                authorId = manga.authorId, author = manga.author, status = manga.status,
                volumeCount = manga.volumeCount, originalLanguage = manga.originalLanguage,
                coverLanguage = manga.coverLanguage,
                volumes = manga.volumes.map { volume ->
                    BackupVolume(
                        id = volume.id, mangaId = volume.mangaId, title = volume.title,
                        coverUrl = volume.coverUrl, number = volume.number?.toFloatOrNull(), locale = volume.locale,
                        isSpecialEdition = volume.isSpecialEdition, owned = volume.owned,
                        createdAt = volume.createdAt, updatedAt = volume.updatedAt,
                    )
                },
            )
        },
        settings = BackupSettings(
            viewMode = settings.android.viewMode, collectionDensity = settings.android.collectionDensity,
            catalogIntegration = settings.shared.catalogIntegration,
            navigationBarStyle = settings.android.navigationBarStyle,
            backupFrequency = settings.shared.backupFrequency,
        ),
    )

    private fun BackupV3Document.toDocument() = BackupDocument(
        formatVersion = formatVersion, createdAt = createdAt, appVersionCode = appVersionCode,
        payload = payload.toPayload(), checksumSha256 = checksumSha256,
    )

    private fun Float.normalizedNumber(): String = BigDecimal(toString()).stripTrailingZeros().toPlainString()

    companion object {
        const val FORMAT_VERSION = 3
        private const val LEGACY_FORMAT_VERSION = 2
        private const val MIN_FORMAT_VERSION = 1
        private const val MAX_MANGA = 10_000
        private const val MAX_VOLUMES = 100_000
        private const val MAX_TEXT_LENGTH = 2_000
    }
}
