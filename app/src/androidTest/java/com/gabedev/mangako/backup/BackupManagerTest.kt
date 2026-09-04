package com.gabedev.mangako.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.SettingsKeys
import com.gabedev.mangako.data.local.dataStore
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: LocalDatabase
    private lateinit var storage: FakeBackupStore
    private lateinit var manager: BackupManager
    private val uri = Uri.parse("content://backup/tree")

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }
        database = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = FakeBackupStore()
        manager = BackupManager(
            context = context,
            database = database,
            storage = storage,
            now = { Date(0) },
            configureFrequency = { _, _ -> },
        )
    }

    @After
    fun tearDown() {
        runBlocking {
            database.close()
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun createBackup_exportsAllVolumesForLibraryManga_andRelevantSettings() = runBlocking {
        database.mangaDao().insertManga(manga("library", onLibrary = true))
        database.mangaDao().insertManga(manga("cached", onLibrary = false))
        database.volumeDao().insertVolumeList(
            listOf(
                volume("owned", "library", number = 1f, owned = true),
                volume("not-owned", "library", number = 2f, owned = false),
                volume("cached-volume", "cached", number = 1f, owned = true),
            )
        )
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.VIEW_MODE] = "list"
            preferences[SettingsKeys.COLLECTION_DENSITY] = 4
            preferences[SettingsKeys.CATALOG_INTEGRATION] = "MANGADEX"
            preferences[SettingsKeys.NAVIGATION_BAR_STYLE] = "FLOATING"
            preferences[SettingsKeys.BACKUP_FREQUENCY] = "WEEKLY"
        }

        val preview = manager.createBackup(uri)
        val document = BackupFormat().decode(requireNotNull(storage.bytes)).document

        assertEquals(1, preview.mangaCount)
        assertEquals(2, preview.volumeCount)
        assertEquals(1, preview.ownedVolumeCount)
        assertEquals("library", document.payload.collection.single().id)
        assertEquals(listOf(true, false), document.payload.collection.single().volumes.map { it.owned })
        assertEquals("list", document.payload.settings.viewMode)
        assertEquals(4, document.payload.settings.collectionDensity)
        assertEquals("MANGADEX", document.payload.settings.catalogIntegration)
        assertEquals("FLOATING", document.payload.settings.navigationBarStyle)
        assertEquals("WEEKLY", document.payload.settings.backupFrequency)
        assertEquals(
            "mangako-backup-19700101T000000000Z.json",
            storage.fileName,
        )
    }

    @Test
    fun replaceRestore_replacesCollectionOwnershipAndSettings() = runBlocking {
        database.mangaDao().insertManga(manga("stale", onLibrary = true))
        database.volumeDao().insertVolumeList(
            listOf(volume("stale-volume", "stale", number = 1f, owned = true))
        )
        storage.bytes = BackupFormat().encode(
            payload = payload(
                mangaId = "restored",
                volumes = listOf(
                    backupVolume("v1", "restored", 1f, owned = true),
                    backupVolume("v2", "restored", 2f, owned = false),
                ),
                frequency = BackupFrequency.DAILY,
            ),
            createdAt = "2026-09-04T12:00:00Z",
            appVersionCode = 1,
        )

        manager.restore(manager.readBackup(uri), RestoreMode.REPLACE)

        assertFalse(requireNotNull(database.mangaDao().getMangaById("stale")).isOnUserLibrary)
        assertFalse(requireNotNull(database.volumeDao().getVolumeById("stale-volume")).owned)
        assertTrue(requireNotNull(database.mangaDao().getMangaById("restored")).isOnUserLibrary)
        assertTrue(requireNotNull(database.volumeDao().getVolumeById("v1")).owned)
        assertFalse(requireNotNull(database.volumeDao().getVolumeById("v2")).owned)
        val preferences = context.dataStore.data.first()
        assertEquals("list", preferences[SettingsKeys.VIEW_MODE])
        assertEquals(3, preferences[SettingsKeys.COLLECTION_DENSITY])
        assertEquals("DAILY", preferences[SettingsKeys.BACKUP_FREQUENCY])
    }

    @Test
    fun mergeRestore_preservesLocalOwnershipAndImportsBackupOwnership() = runBlocking {
        database.mangaDao().insertManga(manga("m1", onLibrary = true))
        database.volumeDao().insertVolumeList(
            listOf(volume("v1", "m1", number = 1f, owned = true))
        )
        storage.bytes = BackupFormat().encode(
            payload = payload(
                mangaId = "m1",
                volumes = listOf(
                    backupVolume("v1", "m1", 1f, owned = false),
                    backupVolume("v2", "m1", 2f, owned = true),
                ),
            ),
            createdAt = "2026-09-04T12:00:00Z",
            appVersionCode = 1,
        )

        manager.restore(manager.readBackup(uri), RestoreMode.MERGE)

        assertTrue(requireNotNull(database.volumeDao().getVolumeById("v1")).owned)
        assertTrue(requireNotNull(database.volumeDao().getVolumeById("v2")).owned)
    }

    @Test
    fun conflictingVolumeId_rollsBackReplaceTransaction() = runBlocking {
        database.mangaDao().insertManga(manga("local", onLibrary = true))
        database.volumeDao().insertVolumeList(
            listOf(volume("shared-id", "local", number = 1f, owned = true))
        )
        storage.bytes = BackupFormat().encode(
            payload = payload(
                mangaId = "other",
                volumes = listOf(backupVolume("shared-id", "other", 1f, owned = false)),
            ),
            createdAt = "2026-09-04T12:00:00Z",
            appVersionCode = 1,
        )

        val result = runCatching {
            manager.restore(manager.readBackup(uri), RestoreMode.REPLACE)
        }

        assertTrue(result.isFailure)
        assertTrue(requireNotNull(database.mangaDao().getMangaById("local")).isOnUserLibrary)
        assertTrue(requireNotNull(database.volumeDao().getVolumeById("shared-id")).owned)
        assertNull(database.mangaDao().getMangaById("other"))
    }

    @Test
    fun failedCreate_recordsError_andSuccessClearsIt() = runBlocking {
        database.mangaDao().insertManga(manga("m1", onLibrary = true))
        storage.writable = false

        assertTrue(runCatching { manager.createBackup(uri) }.isFailure)
        assertNotNull(context.dataStore.data.first()[SettingsKeys.LAST_BACKUP_ERROR])

        storage.writable = true
        manager.createBackup(uri)
        val preferences = context.dataStore.data.first()
        assertNull(preferences[SettingsKeys.LAST_BACKUP_ERROR])
        assertEquals("1970-01-01T00:00:00Z", preferences[SettingsKeys.LAST_BACKUP_AT])
    }

    private fun payload(
        mangaId: String,
        volumes: List<BackupVolume>,
        frequency: BackupFrequency = BackupFrequency.ON_CHANGE,
    ) = BackupPayload(
        collection = listOf(
            BackupManga(
                id = mangaId,
                title = "Manga $mangaId",
                volumes = volumes,
            )
        ),
        settings = BackupSettings(
            viewMode = "list",
            collectionDensity = 3,
            catalogIntegration = "MANGAKO",
            navigationBarStyle = "CLASSIC",
            backupFrequency = frequency.name,
        ),
    )

    private fun manga(id: String, onLibrary: Boolean) = Manga(
        id = id,
        title = "Manga $id",
        coverUrl = "",
        description = "Description",
        isOnUserLibrary = onLibrary,
    )

    private fun volume(
        id: String,
        mangaId: String,
        number: Float,
        owned: Boolean,
    ) = Volume(
        id = id,
        mangaId = mangaId,
        title = "Volume $number",
        coverUrl = "",
        volume = number,
        locale = "ja",
        owned = owned,
    )

    private fun backupVolume(
        id: String,
        mangaId: String,
        number: Float,
        owned: Boolean,
    ) = BackupVolume(
        id = id,
        mangaId = mangaId,
        title = "Volume $number",
        number = number,
        locale = "ja",
        isSpecialEdition = false,
        owned = owned,
    )
}

private class FakeBackupStore : BackupStore {
    var bytes: ByteArray? = null
    var fileName: String? = null
    var writable = true

    override fun read(uri: Uri): ByteArray = requireNotNull(bytes)

    override fun write(treeUri: Uri, fileName: String, bytes: ByteArray) {
        this.fileName = fileName
        this.bytes = bytes
    }

    override fun canWrite(treeUri: Uri): Boolean = writable
}
