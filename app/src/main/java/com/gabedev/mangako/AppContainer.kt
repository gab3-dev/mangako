package com.gabedev.mangako

import android.content.Context
import androidx.room.withTransaction
import com.gabedev.mangako.backup.BackupManager
import com.gabedev.mangako.backup.BackupScheduler
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.MangaKoDatabase
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import com.gabedev.mangako.data.repository.ConfigurableMangaRepository
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaDexRepository
import com.gabedev.mangako.data.repository.MangaDexRepositoryImpl
import com.gabedev.mangako.data.repository.MangaKoRepositoryImpl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class AppContainer(
    val database: LocalDatabase,
    val logger: FileLogger,
    val mangaRepository: MangaDexRepository,
    val localRepository: LibraryRepository,
    val backupManager: BackupManager? = null,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val database = MangaKoDatabase(context).getDatabase()
            val logger = FileLogger(context)
            val backupManager = BackupManager(context.applicationContext, database)
            val mangaDexApi = Retrofit.Builder()
                .baseUrl("https://api.mangadex.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MangaDexAPI::class.java)
            val mangaDexRepository = MangaDexRepositoryImpl(mangaDexApi, logger)
            val mangaKoRepository = BuildConfig.MANGAKO_API_TOKEN
                .takeIf { it.isNotBlank() }
                ?.let { token ->
                    val client = OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("Authorization", "Bearer $token")
                                    .build(),
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

            return AppContainer(
                database = database,
                logger = logger,
                mangaRepository = ConfigurableMangaRepository(
                    context = context.applicationContext,
                    mangaDexRepository = mangaDexRepository,
                    mangaKoRepository = mangaKoRepository,
                ),
                localRepository = LibraryRepositoryImpl(
                    db = database,
                    logger = logger,
                    onBackupRelevantChange = {
                        BackupScheduler.enqueueAfterChange(context.applicationContext)
                    },
                    runInTransaction = { operation ->
                        database.withTransaction { operation() }
                    },
                ),
                backupManager = backupManager,
            )
        }
    }
}
