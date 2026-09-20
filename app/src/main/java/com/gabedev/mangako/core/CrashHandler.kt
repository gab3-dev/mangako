package com.gabedev.mangako.core

import android.content.Context
import android.util.Log

// Crash handler that saves the log to a file.
class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            saveCrashLog(e)
        } catch (ex: Exception) {
            Log.e("CrashHandler", "Error while saving crash log", ex)
        }

        // Forward the exception to the default handler so the app can close.
        defaultExceptionHandler?.uncaughtException(t, e)
    }

    private fun saveCrashLog(e: Throwable) {
        try {
            FileLogger(context).logCrash(e)
        } catch (ex: Exception) {
            Log.e("CrashHandler", "Could not save crash log", ex)
        }
    }
}
