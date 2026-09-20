package com.gabedev.mangako.backup

import android.content.ContentResolver
import android.content.Context
import android.content.UriPermission
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
}
