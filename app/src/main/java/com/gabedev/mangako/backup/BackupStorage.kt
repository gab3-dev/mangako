package com.gabedev.mangako.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream

class BackupStorage(private val context: Context) {
    fun read(uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Unable to open backup")
        return stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_BYTES) { "Backup is too large" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    fun write(treeUri: Uri, fileName: String, bytes: ByteArray) {
        require(bytes.size <= MAX_BACKUP_BYTES) { "Backup is too large" }
        val directory = DocumentFile.fromTreeUri(context, treeUri)
            ?.takeIf { it.isDirectory && it.canWrite() }
            ?: error("Backup folder is unavailable")
        val temporaryName = "$fileName.tmp"
        directory.findFile(temporaryName)?.delete()
        val temporary = directory.createFile(MIME_TYPE, temporaryName)
            ?: error("Unable to create backup")
        var completed = false

        try {
            context.contentResolver.openOutputStream(temporary.uri, "wt")
                ?.use { output ->
                    output.write(bytes)
                    output.flush()
                }
                ?: error("Unable to write backup")
            require(read(temporary.uri).contentEquals(bytes)) { "Backup verification failed" }
            require(temporary.renameTo(fileName)) { "Unable to finish backup" }
            completed = true
            prune(directory)
        } catch (error: Exception) {
            if (!completed) temporary.delete()
            throw error
        }
    }

    fun canWrite(treeUri: Uri): Boolean {
        val hasPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission && it.isWritePermission
        }
        return hasPermission && DocumentFile.fromTreeUri(context, treeUri)
            ?.let { it.isDirectory && it.canWrite() } == true
    }

    private fun prune(directory: DocumentFile) {
        directory.listFiles()
            .filter { file ->
                file.isFile &&
                    file.name?.startsWith(FILE_PREFIX) == true &&
                    file.name?.contains(".tmp") == true
            }
            .forEach { require(it.delete()) { "Unable to remove incomplete backup" } }
        directory.listFiles()
            .filter { file ->
                file.isFile &&
                    file.name?.startsWith(FILE_PREFIX) == true &&
                    file.name?.endsWith(FILE_SUFFIX) == true
            }
            .sortedByDescending { it.name }
            .drop(MAX_HISTORY)
            .forEach { require(it.delete()) { "Unable to enforce backup history limit" } }
    }

    companion object {
        const val FILE_PREFIX = "mangako-backup-"
        const val FILE_SUFFIX = ".json"
        const val MIME_TYPE = "application/json"
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
        const val MAX_HISTORY = 5
    }
}
