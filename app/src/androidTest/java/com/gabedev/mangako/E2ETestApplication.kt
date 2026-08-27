package com.gabedev.mangako

import androidx.room.Room
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaKoRepositoryImpl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class E2ETestApplication : MangaKoApplication() {
    override val enableStartupWork = false
    private lateinit var server: MockWebServer
    private lateinit var database: LocalDatabase

    override fun createAppContainer(): AppContainer {
        server = MockWebServer().apply {
            dispatcher = FixtureDispatcher()
            start()
        }
        database = Room.inMemoryDatabaseBuilder(this, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val logger = FileLogger(this)
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MangaKoAPI::class.java)

        return AppContainer(
            database = database,
            logger = logger,
            mangaRepository = MangaKoRepositoryImpl(api),
            localRepository = LibraryRepositoryImpl(database, logger),
        )
    }

    fun closeE2EResources() {
        database.close()
        server.shutdown()
    }
}

private class FixtureDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.requestUrl?.encodedPath) {
            "/mangas" -> json(mangaListJson)
            "/mangas/manga-1" -> json(mangaJson)
            "/mangas/manga-1/volumes" -> json(volumesJson)
            else -> MockResponse().setResponseCode(404)
        }
    }

    private fun json(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val mangaJson = """{
            "id":"manga-1","mangadexId":"manga-1","primaryTitle":"One Piece",
            "status":"ongoing","latestVolumeNumber":"2",
            "localizations":[{"language":"en","title":"One Piece","description":"Pirate adventure","isPrimary":true}],
            "aliases":[],"covers":[{"id":"cover-1","mangadexCoverId":"cover-1","isPrimary":true,"sourceUrl":"http://example.invalid/cover.jpg"}],
            "authors":[{"id":"author-1","name":"Eiichiro Oda"}]
        }"""
        const val mangaListJson = "[$mangaJson]"
        const val volumesJson = """[
            {"id":"volume-1","mangadexCoverId":"volume-1","sourceUrl":"http://example.invalid/volume-1.jpg","volume":"1","locale":"ja","isSpecialEdition":false,"sourceCreatedAt":null,"sourceUpdatedAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"},
            {"id":"volume-2","mangadexCoverId":"volume-2","sourceUrl":"http://example.invalid/volume-2.jpg","volume":"2","locale":"ja","isSpecialEdition":false,"sourceCreatedAt":null,"sourceUpdatedAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
        ]"""
    }
}
