package com.gabedev.mangako.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gabedev.mangako.MangaKoApplication
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.domain.RefreshMangaVolumesUseCase
import kotlin.coroutines.cancellation.CancellationException

class RefreshLibraryVolumesWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as MangaKoApplication
            val container = app.appContainer
            val syncResult = RefreshMangaVolumesUseCase(
                apiRepository = container.mangaRepository,
                localRepository = container.localRepository,
            ).refreshLibrary(forceRefresh = true)
            if (inputData.getBoolean(KEY_SHOULD_NOTIFY, false)) {
                NewVolumeNotificationHelper.notifyNewVolumes(
                    context = applicationContext,
                    newVolumesByManga = syncResult.newVolumesByManga,
                )
            }

            Result.success(
                workDataOf(
                    KEY_UPDATED_COUNT to syncResult.updatedCount,
                    KEY_NEW_VOLUME_COUNT to syncResult.newVolumesByManga.sumOf { it.volumes.size },
                    KEY_MANGA_COUNT to syncResult.mangaCount,
                    KEY_FAILED_COUNT to syncResult.failedCount,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileLogger(applicationContext).logError(e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_UPDATED_COUNT = "updated_count"
        const val KEY_NEW_VOLUME_COUNT = "new_volume_count"
        const val KEY_MANGA_COUNT = "manga_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_SHOULD_NOTIFY = "should_notify"
    }
}
