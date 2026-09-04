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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackupManager(
    private val context: Context,
    private val database: LocalDatabase,
    private val storage: BackupStore = BackupStorage(context),
    private val format: BackupFormat = BackupFormat(),
    private val now: () -> Date = { Date() },
    private val configureFrequency: (Context, BackupFrequency) -> Unit = BackupScheduler::configure,
) {
    private val mutex = Mutex()

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
                val createdAtDate = now()
                val createdAt = timestamp(createdAtDate, DISPLAY_TIMESTAMP_PATTERN)
                val bytes = format.encode(
                    payload = payload,
                    createdAt = createdAt,
                    appVersionCode = BuildConfig.VERSION_CODE,
                )
                storage.write(
                    treeUri = destination,
                    fileName = "${BackupStorage.FILE_PREFIX}${timestamp(createdAtDate, FILE_TIMESTAMP_PATTERN)}${BackupStorage.FILE_SUFFIX}",
                    bytes = bytes,
                )
                context.dataStore.edit { values ->
                    values[SettingsKeys.LAST_BACKUP_AT] = createdAt
                    values.remove(SettingsKeys.LAST_BACKUP_ERROR)
                }
                format.decode(bytes).preview
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
        format.decode(storage.read(uri))
    }

    suspend fun restore(parsed: ParsedBackup, mode: RestoreMode) = withContext(Dispatchers.IO) {
        mutex.withLock {
            format.validate(parsed.document)
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
            configureFrequency(
                context,
                BackupFrequency.valueOf(payload.settings.backupFrequency),
            )
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

    private fun timestamp(date: Date, pattern: String): String {
        return SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(date)
    }

    companion object {
        private const val DISPLAY_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        private const val FILE_TIMESTAMP_PATTERN = "yyyyMMdd'T'HHmmssSSS'Z'"
    }
}
