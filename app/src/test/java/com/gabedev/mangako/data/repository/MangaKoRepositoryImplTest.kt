package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.dto.MangaKoAliasDto
import com.gabedev.mangako.data.dto.MangaKoCoverDto
import com.gabedev.mangako.data.dto.MangaKoCreatorDto
import com.gabedev.mangako.data.dto.MangaKoLocalizationDto
import com.gabedev.mangako.data.dto.MangaKoMangaDto
import com.gabedev.mangako.data.dto.MangaKoVolumeDto
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaKoRepositoryImplTest {
    private val api = mockk<MangaKoAPI>()
    private val repository = MangaKoRepositoryImpl(api)

    @Test
    fun `search maps localized manga while preserving MangaDex ID`() = runTest {
        coEvery { api.searchMangas("one piece", 6, 0) } returns listOf(mangaDto())

        val manga = repository.searchMangaPage("one piece", 0, 6).single()

        assertEquals("mangadex-manga", manga.id)
        assertEquals("One Piece", manga.title)
        assertEquals("Wan Pisu", manga.altTitle)
        assertEquals("https://example.com/primary.jpg", manga.coverUrl)
        assertEquals("Eiichiro Oda", manga.author)
        assertEquals("Descricao PT", manga.description)
        assertEquals(12, manga.volumeCount)
        assertEquals("pt-BR", manga.originalLanguage)
    }

    @Test
    fun `search falls back to primary title instead of arbitrary localization`() = runTest {
        coEvery { api.searchMangas("berserk", 6, 0) } returns listOf(
            mangaDto().copy(
                primaryTitle = "Berserk",
                localizations = listOf(
                    MangaKoLocalizationDto("fr", "Berserk FR", "Description FR", true),
                ),
                aliases = listOf(MangaKoAliasDto("fr", "Berserk Alias FR")),
            )
        )

        val manga = repository.searchMangaPage("berserk", 0, 6).single()

        assertEquals("Berserk", manga.title)
        assertNull(manga.altTitle)
    }

    @Test
    fun `volumes prefer MangaDex cover ID and source update timestamp`() = runTest {
        coEvery { api.searchMangas("one piece", 6, 0) } returns listOf(mangaDto())
        val manga = repository.searchMangaPage("one piece", 0, 6).single()
        coEvery { api.getVolumes("mangadex-manga", 50, 0, false) } returns listOf(
            MangaKoVolumeDto(
                id = "internal-cover",
                mangaDexCoverId = "mangadex-cover",
                sourceUrl = "https://example.com/volume.jpg",
                volume = "1.5",
                locale = "ja",
                isSpecialEdition = true,
                sourceCreatedAt = "2025-01-01T00:00:00Z",
                sourceUpdatedAt = "2025-01-02T00:00:00Z",
                updatedAt = "2025-01-03T00:00:00Z",
            )
        )

        val volume = repository.getCoverListByManga(manga).single()

        assertEquals("mangadex-cover", volume.id)
        assertEquals("mangadex-manga", volume.mangaId)
        assertEquals(1.5f, volume.volume)
        assertEquals("2025-01-02T00:00:00Z", volume.updatedAt)
    }

    private fun mangaDto() = MangaKoMangaDto(
        id = "internal-manga",
        mangaDexId = "mangadex-manga",
        primaryTitle = "One Piece",
        status = "ongoing",
        latestVolumeNumber = "12",
        localizations = listOf(
            MangaKoLocalizationDto("en", "One Piece", "Description EN", false),
            MangaKoLocalizationDto("pt-BR", "One Piece PT", "Descricao PT", true),
        ),
        aliases = listOf(MangaKoAliasDto("ja-ro", "Wan Pisu")),
        covers = listOf(
            MangaKoCoverDto("internal-cover", "mangadex-cover", true, "https://example.com/primary.jpg")
        ),
        authors = listOf(MangaKoCreatorDto("author", "Eiichiro Oda")),
    )
}
