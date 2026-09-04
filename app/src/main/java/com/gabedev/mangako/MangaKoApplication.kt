package com.gabedev.mangako

import android.app.Application
import com.gabedev.mangako.background.LibraryVolumeRefreshScheduler
import com.gabedev.mangako.backup.BackupScheduler
import com.gabedev.mangako.data.local.getBackupPreferences
import com.gabedev.mangako.core.CrashHandler
import com.gabedev.mangako.data.local.migrateCatalogIntegrationDefaultToMangaKo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

open class MangaKoApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    open val enableStartupWork = true
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        appContainer = createAppContainer()
        if (!enableStartupWork) return
        applicationScope.launch {
            migrateCatalogIntegrationDefaultToMangaKo()
            LibraryVolumeRefreshScheduler.enqueuePeriodic(this@MangaKoApplication)
            BackupScheduler.configure(
                this@MangaKoApplication,
                getBackupPreferences().first().frequency,
            )
        }
    }

    open fun createAppContainer(): AppContainer = AppContainer.create(this)

    fun replaceAppContainer(container: AppContainer) {
        appContainer = container
    }
}
