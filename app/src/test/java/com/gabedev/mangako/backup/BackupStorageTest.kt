package com.gabedev.mangako.backup

import android.content.ContentResolver
import android.content.Context
import android.content.UriPermission
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class BackupStorageTest {
    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val treeUri = mockk<Uri>()

    @Before
    fun setup() {
        mockkStatic(DocumentFile::class)
        every { context.contentResolver } returns contentResolver
    }

    @After
    fun tearDown() {
        unmockkStatic(DocumentFile::class)
        clearAllMocks()
    }

    @Test
    fun `canWrite accepts a persisted tree even when provider does not advertise write support`() {
        val permission = mockk<UriPermission>()
        val directory = mockk<DocumentFile>()
        every { permission.uri } returns treeUri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns true
        every { contentResolver.persistedUriPermissions } returns listOf(permission)
        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.canWrite() } returns false

        assertTrue(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `canWrite rejects a tree without persisted write permission`() {
        val permission = mockk<UriPermission>()
        every { permission.uri } returns treeUri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns false
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertFalse(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `canWrite rejects a tree without persisted read permission`() {
        val permission = mockk<UriPermission>()
        every { permission.uri } returns treeUri
        every { permission.isReadPermission } returns false
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertFalse(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `canWrite rejects a persisted permission for another tree`() {
        val permission = mockk<UriPermission>()
        every { permission.uri } returns mockk()
        every { contentResolver.persistedUriPermissions } returns listOf(permission)

        assertFalse(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `canWrite rejects a persisted uri that is not a directory`() {
        val permission = mockk<UriPermission>()
        val document = mockk<DocumentFile>()
        every { permission.uri } returns treeUri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns true
        every { contentResolver.persistedUriPermissions } returns listOf(permission)
        every { DocumentFile.fromTreeUri(context, treeUri) } returns document
        every { document.isDirectory } returns false

        assertFalse(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `canWrite rejects a tree whose document is unavailable`() {
        val permission = mockk<UriPermission>()
        every { permission.uri } returns treeUri
        every { permission.isReadPermission } returns true
        every { permission.isWritePermission } returns true
        every { contentResolver.persistedUriPermissions } returns listOf(permission)
        every { DocumentFile.fromTreeUri(context, treeUri) } returns null

        assertFalse(BackupStorage(context).canWrite(treeUri))
    }

    @Test
    fun `read returns all backup bytes`() {
        val bytes = "backup".encodeToByteArray()
        every { contentResolver.openInputStream(treeUri) } returns ByteArrayInputStream(bytes)

        assertArrayEquals(bytes, BackupStorage(context).read(treeUri))
    }

    @Test
    fun `read rejects an unavailable input stream`() {
        every { contentResolver.openInputStream(treeUri) } returns null

        val error = assertFailsWith<IllegalStateException> {
            BackupStorage(context).read(treeUri)
        }

        assertEquals("Unable to open backup", error.message)
    }

    @Test
    fun `read rejects a backup larger than the maximum size`() {
        every { contentResolver.openInputStream(treeUri) } returns ByteArrayInputStream(
            ByteArray(BackupStorage.MAX_BACKUP_BYTES + 1)
        )

        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).read(treeUri)
        }

        assertEquals("Backup is too large", error.message)
    }

    @Test
    fun `write rejects data larger than the maximum size`() {
        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).write(treeUri, "snapshot.json", ByteArray(BackupStorage.MAX_BACKUP_BYTES + 1))
        }

        assertEquals("Backup is too large", error.message)
    }

    @Test
    fun `write verifies backup then removes stale files beyond retention limit`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val existingTemporary = documentFile("snapshot.json.tmp")
        val staleIncomplete = documentFile("mangako-backup-interrupted.tmp")
        val backups = (1..6).map { day -> documentFile("mangako-backup-2026010$day.json") }
        val directoryEntry = mockk<DocumentFile>()
        val unrelatedFile = documentFile("other.json")
        val unnamedFile = mockk<DocumentFile>()
        val outputUri = mockk<Uri>()
        val bytes = "backup".encodeToByteArray()
        val output = ByteArrayOutputStream()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns existingTemporary
        every { existingTemporary.delete() } returns true
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns output
        every { contentResolver.openInputStream(outputUri) } returns ByteArrayInputStream(bytes)
        every { temporary.renameTo("snapshot.json") } returns true
        every { directory.listFiles() } returns (
            listOf(staleIncomplete, directoryEntry, unrelatedFile, unnamedFile) + backups
        ).toTypedArray()
        every { staleIncomplete.delete() } returns true
        every { backups.first().delete() } returns true
        every { directoryEntry.isFile } returns false
        every { unnamedFile.isFile } returns true
        every { unnamedFile.name } returns null

        BackupStorage(context).write(treeUri, "snapshot.json", bytes)

        assertArrayEquals(bytes, output.toByteArray())
        verify(exactly = 1) { temporary.renameTo("snapshot.json") }
        verify(exactly = 1) { existingTemporary.delete() }
        verify(exactly = 1) { staleIncomplete.delete() }
        verify(exactly = 1) { backups.first().delete() }
    }

    @Test
    fun `write removes its temporary file when output cannot be opened`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val outputUri = mockk<Uri>()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns null
        every { temporary.delete() } returns true

        val error = assertFailsWith<IllegalStateException> {
            BackupStorage(context).write(treeUri, "snapshot.json", "backup".encodeToByteArray())
        }

        assertEquals("Unable to write backup", error.message)
        verify(exactly = 1) { temporary.delete() }
    }

    @Test
    fun `write removes its temporary file when verification fails`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val outputUri = mockk<Uri>()
        val output = ByteArrayOutputStream()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns output
        every { contentResolver.openInputStream(outputUri) } returns ByteArrayInputStream("other".encodeToByteArray())
        every { temporary.delete() } returns true

        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).write(treeUri, "snapshot.json", "backup".encodeToByteArray())
        }

        assertEquals("Backup verification failed", error.message)
        verify(exactly = 1) { temporary.delete() }
    }

    @Test
    fun `write rejects an unavailable backup folder`() {
        every { DocumentFile.fromTreeUri(context, treeUri) } returns null

        val error = assertFailsWith<IllegalStateException> {
            BackupStorage(context).write(treeUri, "snapshot.json", "backup".encodeToByteArray())
        }

        assertEquals("Backup folder is unavailable", error.message)
    }

    @Test
    fun `write rejects a document that is not a directory`() {
        val directory = mockk<DocumentFile>()
        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns false

        val error = assertFailsWith<IllegalStateException> {
            BackupStorage(context).write(treeUri, "snapshot.json", "backup".encodeToByteArray())
        }

        assertEquals("Backup folder is unavailable", error.message)
    }

    @Test
    fun `write rejects a provider that cannot create the temporary file`() {
        val directory = mockk<DocumentFile>()
        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns null

        val error = assertFailsWith<IllegalStateException> {
            BackupStorage(context).write(treeUri, "snapshot.json", "backup".encodeToByteArray())
        }

        assertEquals("Unable to create backup", error.message)
    }

    @Test
    fun `write removes the temporary file when it cannot be renamed`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val outputUri = mockk<Uri>()
        val bytes = "backup".encodeToByteArray()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns ByteArrayOutputStream()
        every { contentResolver.openInputStream(outputUri) } returns ByteArrayInputStream(bytes)
        every { temporary.renameTo("snapshot.json") } returns false
        every { temporary.delete() } returns true

        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).write(treeUri, "snapshot.json", bytes)
        }

        assertEquals("Unable to finish backup", error.message)
        verify(exactly = 1) { temporary.delete() }
    }

    @Test
    fun `write preserves the completed backup when pruning fails`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val staleIncomplete = documentFile("mangako-backup-interrupted.tmp")
        val outputUri = mockk<Uri>()
        val bytes = "backup".encodeToByteArray()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns ByteArrayOutputStream()
        every { contentResolver.openInputStream(outputUri) } returns ByteArrayInputStream(bytes)
        every { temporary.renameTo("snapshot.json") } returns true
        every { directory.listFiles() } returns arrayOf(staleIncomplete)
        every { staleIncomplete.delete() } returns false

        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).write(treeUri, "snapshot.json", bytes)
        }

        assertEquals("Unable to remove incomplete backup", error.message)
        verify(exactly = 0) { temporary.delete() }
    }

    @Test
    fun `write reports failure when backup history cannot be pruned`() {
        val directory = mockk<DocumentFile>()
        val temporary = mockk<DocumentFile>()
        val backups = (1..6).map { day -> documentFile("mangako-backup-2026010$day.json") }
        val outputUri = mockk<Uri>()
        val bytes = "backup".encodeToByteArray()

        every { DocumentFile.fromTreeUri(context, treeUri) } returns directory
        every { directory.isDirectory } returns true
        every { directory.findFile("snapshot.json.tmp") } returns null
        every { directory.createFile(BackupStorage.MIME_TYPE, "snapshot.json.tmp") } returns temporary
        every { temporary.uri } returns outputUri
        every { contentResolver.openOutputStream(outputUri, "wt") } returns ByteArrayOutputStream()
        every { contentResolver.openInputStream(outputUri) } returns ByteArrayInputStream(bytes)
        every { temporary.renameTo("snapshot.json") } returns true
        every { directory.listFiles() } returns backups.toTypedArray()
        every { backups.first().delete() } returns false

        val error = assertFailsWith<IllegalArgumentException> {
            BackupStorage(context).write(treeUri, "snapshot.json", bytes)
        }

        assertEquals("Unable to enforce backup history limit", error.message)
        verify(exactly = 0) { temporary.delete() }
    }

    private fun documentFile(name: String): DocumentFile = mockk<DocumentFile>().also { file ->
        every { file.isFile } returns true
        every { file.name } returns name
    }
}
