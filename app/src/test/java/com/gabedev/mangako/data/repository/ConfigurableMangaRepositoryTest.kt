package com.gabedev.mangako.data.repository

import android.content.Context
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.getCatalogIntegration
import com.gabedev.mangako.data.model.Manga
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
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

    private fun createManga(id: String, title: String) = Manga(
        id = id,
        title = title,
        coverUrl = "https://example.com/$id.jpg",
        description = "Description for $title",
    )
}
