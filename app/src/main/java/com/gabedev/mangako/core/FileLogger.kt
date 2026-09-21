package com.gabedev.mangako.core

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogger(private val context: Context) {

    fun log(message: String) {
        Log.d(TAG, message)
        append("app_log_${dateStamp()}.txt", "$message\n")
    }

    fun logError(error: Throwable) {
        Log.e(TAG, "Application error", error)
        append("error_log_${dateStamp()}.txt", "${error.message}\n${error.stackTraceToString()}\n")
    }

    fun logCrash(error: Throwable) {
        Log.e(TAG, "Uncaught application crash", error)
        append("crash_log_${timeStamp()}.txt", error.stackTraceToString())
    }

    fun exportTo(destination: Uri) {
        val logs = logDirectory().listFiles()
            ?.filter { it.isFile && LOG_FILE_PREFIXES.any(it.name::startsWith) }
            ?.sortedBy { it.name }
            .orEmpty()
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Unable to create diagnostics file")
        output.bufferedWriter().use { writer ->
            if (logs.isEmpty()) {
                writer.appendLine("No diagnostic logs have been recorded.")
            } else {
                logs.forEach { file ->
                    writer.appendLine("===== ${file.name} =====")
                    file.bufferedReader().use { reader -> reader.copyTo(writer) }
                    writer.appendLine()
                }
            }
        }
    }

    private fun append(fileName: String, content: String) {
        runCatching {
            logDirectory().apply(File::mkdirs).resolve(fileName).appendText(content)
        }.onFailure { Log.e(TAG, "Unable to save diagnostic log", it) }
    }

    private fun logDirectory(): File = context.externalMediaDirs.firstOrNull()
        ?.resolve(LOG_DIRECTORY)
        ?: context.getExternalFilesDir(null)?.resolve(LOG_DIRECTORY)
        ?: context.filesDir.resolve(LOG_DIRECTORY)

    private fun dateStamp(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun timeStamp(): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())

    private companion object {
        const val TAG = "MangaKo"
        const val LOG_DIRECTORY = "logs"
        val LOG_FILE_PREFIXES = listOf("app_log_", "error_log_", "crash_log_")
    }
}
