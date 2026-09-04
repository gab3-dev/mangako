package com.gabedev.mangako.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabedev.mangako.MangaKoApplication
import com.gabedev.mangako.data.local.BackupFrequency
import com.gabedev.mangako.data.local.getBackupPreferences
import kotlinx.coroutines.flow.first

class BackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MangaKoApplication
        val backupManager = app.appContainer.backupManager ?: return Result.failure()
        val preferences = applicationContext.getBackupPreferences().first()
        val uri = preferences.treeUri?.let(Uri::parse) ?: return Result.success()
        if (!backupManager.canUseFolder(uri)) {
            backupManager.recordFailure("Backup folder permission is unavailable")
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }

        if (
            inputData.getBoolean(KEY_CHANGE_TRIGGER, false) &&
            preferences.frequency != BackupFrequency.ON_CHANGE
        ) {
            return Result.success()
        }

        return runCatching {
            backupManager.createBackup(uri)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val KEY_CHANGE_TRIGGER = "change_trigger"
        private const val MAX_ATTEMPTS = 3
    }
}
