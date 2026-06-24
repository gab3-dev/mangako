package com.gabedev.mangako.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object LibraryVolumeRefreshScheduler {
    const val UNIQUE_WORK_NAME = "library_volume_refresh"

    fun enqueue(context: Context): OneTimeWorkRequest {
        val request = OneTimeWorkRequestBuilder<RefreshLibraryVolumesWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )

        return request
    }
}
