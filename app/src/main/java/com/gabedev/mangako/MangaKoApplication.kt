package com.gabedev.mangako

import android.app.Application
import com.gabedev.mangako.background.LibraryVolumeRefreshScheduler
import com.gabedev.mangako.core.CrashHandler
import com.gabedev.mangako.data.local.migrateCatalogIntegrationDefaultToMangaKo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MangaKoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        applicationScope.launch {
            migrateCatalogIntegrationDefaultToMangaKo()
            LibraryVolumeRefreshScheduler.enqueuePeriodic(this@MangaKoApplication)
        }
    }
}
