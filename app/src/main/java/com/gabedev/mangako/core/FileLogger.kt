package com.gabedev.mangako.core

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogger(private val context: Context) {

    fun log(message: String) {
        println(message)
        val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logFile = context.getExternalFilesDir(null)?.resolve("app_log_${timestamp}.txt")
        logFile?.appendText("$message\n") ?: run {
            throw IllegalStateException("Unable to access external files directory")
        }
    }

    fun logError(error: Throwable) {
        error.printStackTrace()
        val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logFile = context.getExternalFilesDir(null)?.resolve("error_log_${timestamp}.txt")
        logFile?.appendText("${error.message}\n${error.stackTraceToString()}\n") ?: run {
            throw IllegalStateException("Unable to access external files directory")
        }
    }
}