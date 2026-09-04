package com.gabedev.mangako.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gabedev.mangako.data.local.BackupFrequency
import java.util.concurrent.TimeUnit

object BackupScheduler {
    internal const val CHANGE_WORK_NAME = "collection_backup_after_change"
    internal const val PERIODIC_WORK_NAME = "collection_backup_periodic"
    internal const val CHANGE_TRIGGER_TAG = "collection_backup_change_trigger"

    fun enqueueAfterChange(context: Context) {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setInputData(workDataOf(BackupWorker.KEY_CHANGE_TRIGGER to true))
            .addTag(CHANGE_TRIGGER_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CHANGE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun configure(context: Context, frequency: BackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        if (frequency == BackupFrequency.ON_CHANGE) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val intervalDays = if (frequency == BackupFrequency.DAILY) 1L else 7L
        val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalDays, TimeUnit.DAYS)
            .setInitialDelay(intervalDays, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
