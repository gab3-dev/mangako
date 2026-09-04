package com.gabedev.mangako.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.room.withTransaction
import com.gabedev.mangako.BuildConfig
import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.NavigationBarStyle
import com.gabedev.mangako.data.local.SettingsKeys
import com.gabedev.mangako.data.local.dataStore
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

class BackupManager(
    private val context: Context,
    private val database: LocalDatabase,
    private val storage: BackupStorage = BackupStorage(context),
) {
    private val mutex = Mutex()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun createBackup(treeUri: Uri? = null): BackupPreview = withContext(Dispatchers.IO) {
        mutex.withLock {
        try {
            val preferences = context.dataStore.data.first()
            val destination = treeUri
                ?: preferences[SettingsKeys.BACKUP_TREE_URI]?.let(Uri::parse)
                ?: error("No backup folder configured")
            require(storage.canWrite(destination)) { "Backup folder permission is unavailable" }

            val payload = database.withTransaction {
                val collection = mutableListOf<BackupManga>()
                database.mangaDao().getAllMangaWithOwned()
                    .filter { it.isOnUserLibrary }
                    .forEach { manga ->
                        val entity = database.mangaDao().getMangaById(manga.id)
                            ?: error("Library manga ${manga.id} is missing")
                        val volumes = database.volumeDao().getVolumesByMangaId(manga.id)
                            .map { it.toBackup() }
                        collection += entity.toBackup(volumes)
                    }
                BackupPayload(
                    collection = collection,
                    settings = BackupSettings(
                        viewMode = preferences[SettingsKeys.VIEW_MODE].orEmpty(),
                        collectionDensity = preferences[SettingsKeys.COLLECTION_DENSITY]
                            ?.coerceIn(1, 5) ?: 2,
                        catalogIntegration = preferences[SettingsKeys.CATALOG_INTEGRATION]
                            ?: CatalogIntegration.MANGAKO.name,
                        navigationBarStyle = preferences[SettingsKeys.NAVIGATION_BAR_STYLE]
                            ?: NavigationBarStyle.CLASSIC.name,
                        backupFrequency = preferences[SettingsKeys.BACKUP_FREQUENCY]
                            ?: BackupFrequency.ON_CHANGE.name,
                    ),
                )
            }
            val createdAt = timestamp(DISPLAY_TIMESTAMP_PATTERN)
            val document = BackupDocument(
                formatVersion = FORMAT_VERSION,
                createdAt = createdAt,
                appVersionCode = BuildConfig.VERSION_CODE,
                payload = payload,
                checksumSha256 = checksum(payload, FORMAT_VERSION),
            )
            val bytes = json.encodeToString(document).encodeToByteArray()
            storage.write(
                treeUri = destination,
                fileName = "${BackupStorage.FILE_PREFIX}${timestamp(FILE_TIMESTAMP_PATTERN)}${BackupStorage.FILE_SUFFIX}",
                bytes = bytes,
            )
            context.dataStore.edit { values ->
                values[SettingsKeys.LAST_BACKUP_AT] = createdAt
                values.remove(SettingsKeys.LAST_BACKUP_ERROR)
            }
            ParsedBackup(document).preview
        } catch (error: Exception) {
            context.dataStore.edit { values ->
                values[SettingsKeys.LAST_BACKUP_ERROR] = error.message.orEmpty().take(300)
            }
            throw error
        }
        }
    }

    fun canUseFolder(uri: Uri): Boolean = storage.canWrite(uri)

    suspend fun recordFailure(message: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { values ->
            values[SettingsKeys.LAST_BACKUP_ERROR] = message.take(300)
        }
    }

    suspend fun readBackup(uri: Uri): ParsedBackup = withContext(Dispatchers.IO) {
        val document = json.decodeFromString<BackupDocument>(storage.read(uri).decodeToString())
        validate(document)
        ParsedBackup(document)
    }

    suspend fun restore(parsed: ParsedBackup, mode: RestoreMode) = withContext(Dispatchers.IO) {
        mutex.withLock {
            validate(parsed.document)
            val payload = parsed.document.payload

            database.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    database.volumeDao().clearOwnedStatus()
                    database.mangaDao().clearLibraryStatus()
                }

                payload.collection.forEach { backupManga ->
                    val existing = database.mangaDao().getMangaById(backupManga.id)
                    database.mangaDao().insertManga(backupManga.toEntity(existing))
                    backupManga.volumes.forEach { backupVolume ->
                        val existingById = database.volumeDao().getVolumeById(backupVolume.id)
                        require(existingById == null || existingById.mangaId == backupVolume.mangaId) {
                            "Volume ID belongs to another manga"
                        }
                        val existingVolume = existingById ?: backupVolume.number?.let { number ->
                                database.volumeDao().getNumberedVolume(
                                    mangaId = backupVolume.mangaId,
                                    volume = number,
                                    locale = backupVolume.locale,
                                )
                            }
                        database.volumeDao().insertVolumeList(
                            listOf(backupVolume.toEntity(existingVolume, mode))
                        )
                    }
                }
            }

            context.dataStore.edit { preferences ->
                preferences[SettingsKeys.VIEW_MODE] = payload.settings.viewMode
                preferences[SettingsKeys.COLLECTION_DENSITY] = payload.settings.collectionDensity.coerceIn(1, 5)
                preferences[SettingsKeys.CATALOG_INTEGRATION] = payload.settings.catalogIntegration
                preferences[SettingsKeys.CATALOG_INTEGRATION_MIGRATED_TO_MANGAKO] = true
                preferences[SettingsKeys.NAVIGATION_BAR_STYLE] = payload.settings.navigationBarStyle
                preferences[SettingsKeys.BACKUP_FREQUENCY] = payload.settings.backupFrequency
            }
            BackupScheduler.configure(
                context,
                BackupFrequency.valueOf(payload.settings.backupFrequency),
            )
        }
    }

    private fun validate(document: BackupDocument) {
        require(document.formatVersion in MIN_FORMAT_VERSION..FORMAT_VERSION) {
            "Unsupported backup version"
        }
        require(document.createdAt.isNotBlank()) { "Backup date is missing" }
        require(document.payload.collection.size <= MAX_MANGA) { "Too many manga entries" }
        require(
            document.payload.collection.sumOf { it.volumes.size } <= MAX_VOLUMES
        ) { "Too many volume entries" }
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

    private fun Manga.toBackup(volumes: List<BackupVolume>) = BackupManga(
        id = id,
        title = title,
        altTitle = altTitle,
        type = type,
        coverId = coverId,
        coverFileName = coverFileName,
        coverUrl = coverUrl,
        authorId = authorId,
        author = author,
        status = status,
        volumeCount = volumeCount,
        originalLanguage = originalLanguage,
        volumes = volumes,
    )

    private fun Volume.toBackup() = BackupVolume(
        id = id,
        mangaId = mangaId,
        title = title,
        coverUrl = coverUrl,
        number = volume,
        locale = locale,
        isSpecialEdition = isSpecialEdition,
        owned = owned,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupManga.toEntity(existing: Manga?): Manga {
        return existing?.copy(isOnUserLibrary = true) ?: Manga(
            id = id,
            title = title,
            altTitle = altTitle,
            type = type,
            coverId = coverId,
            coverFileName = coverFileName,
            coverUrl = coverUrl,
            authorId = authorId,
            author = author,
            description = "",
            status = status,
            volumeCount = volumeCount,
            originalLanguage = originalLanguage,
            isOnUserLibrary = true,
        )
    }

    private fun BackupVolume.toEntity(existing: Volume?, mode: RestoreMode): Volume {
        val restoredOwned = if (mode == RestoreMode.MERGE) {
            existing?.owned == true || owned
        } else {
            owned
        }
        return existing?.copy(owned = restoredOwned) ?: Volume(
            id = id,
            mangaId = mangaId,
            title = title,
            coverUrl = coverUrl,
            volume = number,
            locale = locale,
            owned = restoredOwned,
            isSpecialEdition = isSpecialEdition,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun timestamp(pattern: String): String {
        return SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    companion object {
        const val FORMAT_VERSION = 2
        private const val MIN_FORMAT_VERSION = 1
        private const val MAX_MANGA = 10_000
        private const val MAX_VOLUMES = 100_000
        private const val MAX_TEXT_LENGTH = 2_000
        private const val DISPLAY_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        private const val FILE_TIMESTAMP_PATTERN = "yyyyMMdd'T'HHmmssSSS'Z'"
    }
}
