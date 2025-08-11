package com.gabedev.mangako

import android.app.Application
import com.gabedev.mangako.core.CrashHandler

class MangaKoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
}