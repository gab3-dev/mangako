package com.gabedev.mangako.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A classe CrashHandler que salva o log em um arquivo
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            saveCrashLog(e)
        } catch (ex: Exception) {
            Log.e("CrashHandler", "Error while saving crash log", ex)
        }

        // Repassa a exceção para o handler padrão para que o app possa fechar
        defaultExceptionHandler?.uncaughtException(t, e)
    }

    private fun saveCrashLog(e: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val crashLogFile = File(context.getExternalFilesDir(null), "crash_log_$timestamp.txt")

        try {
            FileWriter(crashLogFile).use { fileWriter ->
                PrintWriter(fileWriter).use { printWriter ->
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    e.printStackTrace(pw)
                    val stackTrace = sw.toString()
                    printWriter.println(stackTrace)
                }
            }
            Log.d("CrashHandler", "Crash log saved to ${crashLogFile.absolutePath}")
        } catch (ex: Exception) {
            Log.e("CrashHandler", "Could not save crash log", ex)
        }
    }
}