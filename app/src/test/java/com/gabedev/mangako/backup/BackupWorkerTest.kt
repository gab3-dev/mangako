package com.gabedev.mangako.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.gabedev.mangako.AppContainer
import com.gabedev.mangako.MangaKoApplication
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.SettingsKeys
import com.gabedev.mangako.data.local.dataStore
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = BackupWorkerTestApplication::class, sdk = [35])
class BackupWorkerTest {
    private lateinit var context: Context
    private lateinit var application: MangaKoApplication
    private lateinit var database: LocalDatabase
    private lateinit var storage: FakeBackupStore
    private lateinit var backupManager: BackupManager
    private val uri = Uri.parse("content://backup/tree")

    @Before
    fun setUp() {
        runBlocking {
            context = RuntimeEnvironment.getApplication()
            application = context as MangaKoApplication
            context.dataStore.edit { it.clear() }
            database = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            storage = FakeBackupStore()
            backupManager = BackupManager(
                context = context,
                database = database,
                storage = storage,
                configureFrequency = { _, _ -> },
            )
            replaceContainer(backupManager)
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.close()
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun `returns failure when backup manager is unavailable`() = runBlocking {
        replaceContainer(null)

        assertEquals(Result.failure(), worker().doWork())
    }

    @Test
    fun `returns success when no backup folder is configured`() = runBlocking {
        assertEquals(Result.success(), worker().doWork())
        assertNull(storage.bytes)
    }

    @Test
    fun `retries unavailable backup folders before failing permanently`() = runBlocking {
        configureDestination(BackupFrequency.ON_CHANGE)
        storage.writable = false

        assertEquals(Result.retry(), worker().doWork())
        assertEquals(
            "Backup folder permission is unavailable",
            context.dataStore.data.first()[SettingsKeys.LAST_BACKUP_ERROR],
        )
        assertEquals(Result.failure(), worker(runAttemptCount = 3).doWork())
    }

    @Test
    fun `skips change triggered work when backups are scheduled periodically`() = runBlocking {
        configureDestination(BackupFrequency.DAILY)

        assertEquals(Result.success(), worker(changeTrigger = true).doWork())
        assertNull(storage.bytes)
    }

    @Test
    fun `creates a backup for on change work`() = runBlocking {
        configureDestination(BackupFrequency.ON_CHANGE)

        assertEquals(Result.success(), worker(changeTrigger = true).doWork())
        assertNotNull(storage.bytes)
    }

    @Test
    fun `retries failed backup creation before failing permanently`() = runBlocking {
        configureDestination(BackupFrequency.ON_CHANGE)
        storage.writeError = IllegalStateException("disk full")

        assertEquals(Result.retry(), worker().doWork())
        assertEquals(Result.failure(), worker(runAttemptCount = 3).doWork())
    }

    private suspend fun configureDestination(frequency: BackupFrequency) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.BACKUP_TREE_URI] = uri.toString()
            preferences[SettingsKeys.BACKUP_FREQUENCY] = frequency.name
        }
    }

    private fun worker(changeTrigger: Boolean = false, runAttemptCount: Int = 0): BackupWorker {
        return TestListenableWorkerBuilder<BackupWorker>(context)
            .setInputData(workDataOf(BackupWorker.KEY_CHANGE_TRIGGER to changeTrigger))
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    private fun replaceContainer(manager: BackupManager?) {
        application.replaceAppContainer(
            AppContainer(
                database = database,
                logger = FileLogger(context),
                mangaRepository = mockk<MangaDexRepository>(),
                localRepository = mockk<LibraryRepository>(),
                backupManager = manager,
            ),
        )
    }

    private class FakeBackupStore : BackupStore {
        var bytes: ByteArray? = null
        var writable = true
        var writeError: Exception? = null

        override fun read(uri: Uri): ByteArray = requireNotNull(bytes)

        override fun write(treeUri: Uri, fileName: String, bytes: ByteArray) {
            writeError?.let { throw it }
            this.bytes = bytes
        }

        override fun canWrite(treeUri: Uri): Boolean = writable
    }
}

class BackupWorkerTestApplication : MangaKoApplication() {
    override val enableStartupWork = false
}
