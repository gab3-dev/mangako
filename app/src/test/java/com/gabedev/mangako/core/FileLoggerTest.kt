package com.gabedev.mangako.core

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.mockk
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileLoggerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `exportTo combines application error and crash logs`() {
        val destination = mockk<Uri>()
        val output = ByteArrayOutputStream()
        every { context.getExternalFilesDir(null) } returns temporaryFolder.root
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(destination, "wt") } returns output
        val logger = FileLogger(context)

        logger.log("Search completed")
        logger.logError(IllegalStateException("Request failed"))
        logger.logCrash(IllegalArgumentException("Unexpected state"))
        logger.exportTo(destination)

        val exported = output.toString()
        assertTrue(exported.contains("app_log_"))
        assertTrue(exported.contains("Search completed"))
        assertTrue(exported.contains("error_log_"))
        assertTrue(exported.contains("Request failed"))
        assertTrue(exported.contains("crash_log_"))
        assertTrue(exported.contains("Unexpected state"))
    }
}
