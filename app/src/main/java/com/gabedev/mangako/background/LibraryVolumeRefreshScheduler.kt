package com.gabedev.mangako.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object LibraryVolumeRefreshScheduler {
    const val STARTUP_WORK_NAME = "library_volume_refresh_startup"
    const val PERIODIC_WORK_NAME = "library_volume_refresh_periodic"

    fun enqueue(context: Context): OneTimeWorkRequest {
        val request = OneTimeWorkRequestBuilder<RefreshLibraryVolumesWorker>()
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )

        return request
    }

    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshLibraryVolumesWorker>(3, TimeUnit.DAYS)
            .setInitialDelay(3, TimeUnit.DAYS)
            .setInputData(
                workDataOf(RefreshLibraryVolumesWorker.KEY_SHOULD_NOTIFY to true)
            )
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
