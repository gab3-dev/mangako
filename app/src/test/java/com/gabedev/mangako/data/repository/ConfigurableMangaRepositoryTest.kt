package com.gabedev.mangako.data.repository

import android.content.Context
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.getCatalogIntegration
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertFailsWith

class ConfigurableMangaRepositoryTest {
    private lateinit var context: Context
    private lateinit var mangaDexRepository: MangaDexRepository
    private lateinit var mangaKoRepository: MangaDexRepository
    private lateinit var repository: ConfigurableMangaRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mangaDexRepository = mockk(relaxed = true)
        mangaKoRepository = mockk(relaxed = true)

        mockkStatic("com.gabedev.mangako.data.local.SettingsDataStoreKt")
        every { context.getCatalogIntegration() } returns flowOf(CatalogIntegration.MANGAKO)

        repository = ConfigurableMangaRepository(
            context = context,
            mangaDexRepository = mangaDexRepository,
            mangaKoRepository = mangaKoRepository,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("com.gabedev.mangako.data.local.SettingsDataStoreKt")
        clearAllMocks()
    }

    @Test
    fun `searchMangaPage uses MangaDex directly when MangaKo integration is disabled`() = runTest {
        val mangaDexResult = listOf(
            createManga(
                id = "mangadex-1",
                title = "MangaDex result",
            ),
        )

        every { context.getCatalogIntegration() } returns flowOf(CatalogIntegration.MANGADEX)
        coEvery {
            mangaDexRepository.searchMangaPage("one piece", 0, 6)
        } returns mangaDexResult

        val result = repository.searchMangaPage("one piece", 0, 6)

        assertEquals(mangaDexResult, result)

        coVerify(exactly = 1) {
            mangaDexRepository.searchMangaPage("one piece", 0, 6)
        }
        coVerify(exactly = 0) {
            mangaKoRepository.searchMangaPage(any(), any(), any())
        }
        verify(exactly = 0) {
            mangaDexRepository.log(any())
        }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `searchMangaPage does not call MangaDex when MangaKo succeeds`() = runTest {
        val mangaKoResult = listOf(
            createManga(
                id = "mangako-1",
                title = "MangaKo result",
            ),
        )

        coEvery {
            mangaKoRepository.searchMangaPage("one piece", 0, 6)
        } returns mangaKoResult

        val result = repository.searchMangaPage("one piece", 0, 6)

        assertEquals(mangaKoResult, result)

        coVerify(exactly = 1) {
            mangaKoRepository.searchMangaPage("one piece", 0, 6)
        }
        coVerify(exactly = 0) {
            mangaDexRepository.searchMangaPage(any(), any(), any())
        }
        verify(exactly = 0) {
            mangaDexRepository.log(any())
        }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `searchMangaPage falls back to MangaDex when MangaKo throws exception`() = runTest {
        val exception = RuntimeException("MangaKo API failed")
        val fallbackManga = listOf(createManga(id = "mangadex-1", title = "MangaDex result"))

        coEvery { mangaKoRepository.searchMangaPage("one piece", 0, 6) } throws exception
        coEvery { mangaDexRepository.searchMangaPage("one piece", 0, 6) } returns fallbackManga

        val result = repository.searchMangaPage("one piece", 0, 6)

        assertEquals(fallbackManga, result)
        coVerify(exactly = 1) { mangaKoRepository.searchMangaPage("one piece", 0, 6) }
        coVerify(exactly = 1) { mangaDexRepository.searchMangaPage("one piece", 0, 6) }
        verify(exactly = 1) { mangaDexRepository.log(exception) }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `searchMangaPage falls back to MangaDex when MangaKo throws HttpException`() = runTest {
        val exception = HttpException(
            Response.error<String>(
                503,
                "Service unavailable".toResponseBody(),
            )
        )
        val fallbackManga = listOf(createManga(id = "mangadex-1", title = "MangaDex result"))

        coEvery { mangaKoRepository.searchMangaPage("one piece", 0, 6) } throws exception
        coEvery { mangaDexRepository.searchMangaPage("one piece", 0, 6) } returns fallbackManga

        val result = repository.searchMangaPage("one piece", 0, 6)

        assertEquals(fallbackManga, result)
        coVerify(exactly = 1) { mangaKoRepository.searchMangaPage("one piece", 0, 6) }
        coVerify(exactly = 1) { mangaDexRepository.searchMangaPage("one piece", 0, 6) }
        verify(exactly = 1) { mangaDexRepository.log(exception) }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `searchMangaPage propagates MangaDex exception when fallback also fails`() {
        val mangaKoException = RuntimeException("MangaKo API failed")
        val mangaDexException = RuntimeException("MangaDex API failed")

        coEvery { mangaKoRepository.searchMangaPage("one piece", 0, 6) } throws mangaKoException
        coEvery { mangaDexRepository.searchMangaPage("one piece", 0, 6) } throws mangaDexException

        val exception = assertFailsWith<RuntimeException> {
            runTest {
                repository.searchMangaPage("one piece", 0, 6)
            }
        }

        assertSame(mangaDexException, exception)
        coVerify(exactly = 1) { mangaKoRepository.searchMangaPage("one piece", 0, 6) }
        coVerify(exactly = 1) { mangaDexRepository.searchMangaPage("one piece", 0, 6) }
        verify(exactly = 1) { mangaDexRepository.log(mangaKoException) }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `searchMangaPage does not fall back to MangaDex when MangaKo is cancelled`() {
        val cancellation = CancellationException("Search cancelled")

        coEvery { mangaKoRepository.searchMangaPage("one piece", 0, 6) } throws cancellation

        val exception = assertFailsWith<CancellationException> {
            runTest {
                repository.searchMangaPage("one piece", 0, 6)
            }
        }

        assertSame(cancellation, exception)

        coVerify(exactly = 1) { mangaKoRepository.searchMangaPage("one piece", 0, 6) }
        coVerify(exactly = 0) { mangaDexRepository.searchMangaPage(any(), any(), any()) }
        verify(exactly = 0) { mangaDexRepository.log(any()) }

        confirmVerified(mangaKoRepository, mangaDexRepository)
    }

    @Test
    fun `remaining operations use MangaKo when integration is enabled`() = runTest {
        val manga = createManga("manga", "Manga")
        val enriched = manga.copy(description = "Enriched")
        val volume = Volume("volume", manga.id, manga.title, "cover", 1f, "en")
        val error = IllegalStateException("error")
        coEvery { mangaKoRepository.searchManga("title", 4) } returns listOf(manga)
        coEvery { mangaKoRepository.enrichManga(manga) } returns enriched
        coEvery { mangaKoRepository.getManga("manga", true) } returns manga
        coEvery { mangaKoRepository.getMangaCoverFileName("manga") } returns "cover.jpg"
        coEvery { mangaKoRepository.getAuthorNameById("author") } returns "Author"
        coEvery { mangaKoRepository.getCoverListByManga(manga, 2, 20, true) } returns listOf(volume)

        assertEquals(listOf(manga), repository.searchManga("title", 4))
        assertEquals(enriched, repository.enrichManga(manga))
        assertEquals(manga, repository.getManga("manga", true))
        assertEquals("cover.jpg", repository.getMangaCoverFileName("manga"))
        assertEquals("Author", repository.getAuthorNameById("author"))
        assertEquals(listOf(volume), repository.getCoverListByManga(manga, 2, 20, true))
        repository.log(error)

        coVerify { mangaKoRepository.searchManga("title", 4) }
        coVerify { mangaKoRepository.enrichManga(manga) }
        coVerify { mangaKoRepository.getManga("manga", true) }
        coVerify { mangaKoRepository.getMangaCoverFileName("manga") }
        coVerify { mangaKoRepository.getAuthorNameById("author") }
        coVerify { mangaKoRepository.getCoverListByManga(manga, 2, 20, true) }
        verify { mangaDexRepository.log(error) }
    }

    @Test
    fun `uses MangaDex when MangaKo repository is unavailable`() = runTest {
        val withoutMangaKo = ConfigurableMangaRepository(context, mangaDexRepository, null)
        coEvery { mangaDexRepository.getAuthorNameById("author") } returns "MangaDex author"

        assertEquals("MangaDex author", withoutMangaKo.getAuthorNameById("author"))

        coVerify { mangaDexRepository.getAuthorNameById("author") }
    }

    private fun createManga(id: String, title: String) = Manga(
        id = id,
        title = title,
        coverUrl = "https://example.com/$id.jpg",
        description = "Description for $title",
    )
}
