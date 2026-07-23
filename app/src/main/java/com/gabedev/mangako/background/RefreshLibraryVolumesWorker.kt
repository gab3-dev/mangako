package com.gabedev.mangako.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gabedev.mangako.BuildConfig
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.MangaKoDatabase
import com.gabedev.mangako.data.repository.ConfigurableMangaRepository
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaDexRepositoryImpl
import com.gabedev.mangako.data.repository.MangaKoRepositoryImpl
import com.gabedev.mangako.domain.RefreshMangaVolumesUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RefreshLibraryVolumesWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val logger = FileLogger(applicationContext)
            val database = MangaKoDatabase(applicationContext).getDatabase()
            val api = Retrofit.Builder()
                .baseUrl("https://api.mangadex.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MangaDexAPI::class.java)
            val mangaDexRepository = MangaDexRepositoryImpl(api, logger)
            val mangaKoRepository = BuildConfig.MANGAKO_API_TOKEN
                .takeIf { it.isNotBlank() }
                ?.let { token ->
                    val client = OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("Authorization", "Bearer $token")
                                    .build()
                            )
                        }
                        .build()
                    val mangaKoApi = Retrofit.Builder()
                        .baseUrl(BuildConfig.MANGAKO_API_BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(MangaKoAPI::class.java)
                    MangaKoRepositoryImpl(mangaKoApi)
                }
            val syncResult = RefreshMangaVolumesUseCase(
                apiRepository = ConfigurableMangaRepository(
                    context = applicationContext,
                    mangaDexRepository = mangaDexRepository,
                    mangaKoRepository = mangaKoRepository,
                ),
                localRepository = LibraryRepositoryImpl(database, logger),
            ).refreshLibrary(forceRefresh = true)

            Result.success(
                workDataOf(
                    KEY_UPDATED_COUNT to syncResult.updatedCount,
                    KEY_MANGA_COUNT to syncResult.mangaCount,
                    KEY_FAILED_COUNT to syncResult.failedCount,
                )
            )
        } catch (e: Exception) {
            FileLogger(applicationContext).logError(e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_UPDATED_COUNT = "updated_count"
        const val KEY_MANGA_COUNT = "manga_count"
        const val KEY_FAILED_COUNT = "failed_count"
    }
}
