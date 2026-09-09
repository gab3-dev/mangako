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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MangaKoRepositoryImplTest {
    private val api = mockk<MangaKoAPI>()
    private val repository = MangaKoRepositoryImpl(
        api, unavailableTitle = { "Title unavailable" }, localeProvider = { Locale.ENGLISH },
    )

    @Test
    fun `search maps localized manga while preserving MangaDex ID`() = runTest {
        coEvery { api.searchMangas("one piece", 6, 0) } returns listOf(mangaDto())

        val manga = repository.searchMangaPage("one piece", 0, 6).single()

        assertEquals("mangadex-manga", manga.id)
        assertEquals("One Piece", manga.title)
        assertEquals("Wan Pisu", manga.altTitle)
        assertEquals("https://example.com/primary.jpg", manga.coverUrl)
        assertEquals("Eiichiro Oda", manga.author)
        assertEquals("Description EN", manga.description)
        assertEquals(12, manga.volumeCount)
    }

    @Test
    fun `search uses placeholder instead of arbitrary primary title or localization`() = runTest {
        val dto = mangaDto().copy(
            primaryTitle = "Berserk",
            localizations = listOf(MangaKoLocalizationDto("fr", "Berserk FR", "Description FR", true)),
            aliases = listOf(MangaKoAliasDto("fr", "Berserk Alias FR")),
        )
        coEvery { api.searchMangas("berserk", 6, 0) } returns listOf(dto)
        coEvery { api.getManga("mangadex-manga", false) } returns dto

        val manga = repository.searchMangaPage("berserk", 0, 6).single()
        val detail = repository.getManga("mangadex-manga", false)

        assertEquals("Title unavailable", manga.title)
        assertTrue(manga.title.isNotBlank())
        assertNull(manga.altTitle)
        assertEquals(manga.title, detail.title)
        assertNull(detail.altTitle)
    }

    @Test
    fun `getManga selects Brazilian Portuguese description`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.forLanguageTag("pt-BR") }
        coEvery { api.getManga("mangadex-manga", false) } returns mangaDto()

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Descricao PT", manga.description)
        assertEquals("One Piece PT", manga.title)
    }

    @Test
    fun `getManga selects exact Portugal locale over Brazilian primary`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.forLanguageTag("pt-PT") }
        val dto = mangaDto()
        coEvery { api.getManga("mangadex-manga", false) } returns dto.copy(
            localizations = dto.localizations + MangaKoLocalizationDto(
                "pt-PT", "One Piece Portugal", "Descricao Portugal", false,
            ),
        )

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Descricao Portugal", manga.description)
        assertEquals("One Piece Portugal", manga.title)
    }

    @Test
    fun `getManga falls back to Brazilian Portuguese for Portugal locale`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.forLanguageTag("pt-PT") }
        coEvery { api.getManga("mangadex-manga", false) } returns mangaDto()

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Descricao PT", manga.description)
        assertEquals("One Piece PT", manga.title)
    }

    @Test
    fun `getManga selects Japanese description over English and primary`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.JAPANESE }
        val dto = mangaDto()
        coEvery { api.getManga("mangadex-manga", false) } returns dto.copy(
            localizations = dto.localizations + MangaKoLocalizationDto(
                "ja", "One Piece JA", "Description JA", false,
            ),
        )

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Description JA", manga.description)
        assertEquals("One Piece JA", manga.title)
        assertEquals("Wan Pisu", manga.altTitle)
    }

    @Test
    fun `getManga falls back to English when requested language is missing`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.GERMAN }
        coEvery { api.getManga("mangadex-manga", false) } returns mangaDto()

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Description EN", manga.description)
    }

    @Test
    fun `getManga falls back to primary when requested language and English are missing`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.GERMAN }
        coEvery { api.getManga("mangadex-manga", false) } returns mangaDto().copy(
            localizations = listOf(
                MangaKoLocalizationDto("fr", "One Piece FR", "Description FR", false),
                mangaDto().localizations.single { it.isPrimary },
            ),
        )

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Descricao PT", manga.description)
    }

    @Test
    fun `getManga ignores blank primary and preserves first nonblank duplicate description`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.forLanguageTag("pt-BR") }
        val dto = mangaDto()
        val primary = dto.localizations.single { it.isPrimary }
        coEvery { api.getManga("mangadex-manga", false) } returns dto.copy(
            localizations = listOf(primary.copy(description = " \n\t")) +
                dto.localizations.map { it.copy(isPrimary = false) } +
                primary.copy(description = "Duplicate PT", isPrimary = false),
        )

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Descricao PT", manga.description)
    }

    @Test
    fun `getManga falls back to English when requested description is blank`() = runTest {
        val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { Locale.forLanguageTag("pt-BR") }
        val dto = mangaDto()
        coEvery { api.getManga("mangadex-manga", false) } returns dto.copy(
            localizations = dto.localizations.map {
                if (it.isPrimary) it.copy(description = " \n\t") else it
            },
        )

        val manga = repository.getManga("mangadex-manga", false)

        assertEquals("Description EN", manga.description)
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

    @Test
    fun `search and detail prioritize title language across localizations and aliases`() = runTest {
        val cases = listOf(
            Triple(Locale.ENGLISH, listOf("fr" to "French", "en" to "English"), "English"),
            Triple(Locale.ENGLISH, listOf("fr" to "French"), "Romanized"),
            Triple(Locale.forLanguageTag("pt-BR"), listOf("en" to "English", "pt-br" to "Brazilian"), "Brazilian"),
            Triple(Locale.forLanguageTag("pt-BR"), listOf("pt-PT" to "Portugal", "en" to "English"), "English"),
            Triple(Locale.forLanguageTag("pt-BR"), listOf("pt-PT" to "Portugal"), "Romanized"),
            Triple(Locale.forLanguageTag("de-AT"), listOf("en" to "English", "de" to "German", "de-AT" to "Austrian"), "Austrian"),
            Triple(Locale.forLanguageTag("de-AT"), listOf("en" to "English", "de-DE" to "Regional", "de" to "German"), "German"),
            Triple(Locale.GERMAN, listOf("en" to "English", "de-DE" to "Regional"), "Regional"),
            Triple(Locale.GERMAN, listOf("fr" to "French", "en" to "English"), "English"),
            Triple(Locale.GERMAN, listOf("fr" to "French"), "Romanized"),
            Triple(Locale.JAPANESE, listOf("en" to "English", "ja" to "Japanese"), "Japanese"),
            Triple(Locale.JAPAN, listOf("en" to "English", "ja-JP" to "Regional Japanese"), "Title unavailable"),
            Triple(Locale.JAPANESE, listOf("en" to "English", "ja" to " \n\t"), "Title unavailable"),
        )
        for ((locale, titles, expected) in cases) {
            val repository = MangaKoRepositoryImpl(api, { "Title unavailable" }) { locale }
            // Put lower-priority languages in localizations to exercise alias priority.
            val dto = mangaDto().copy(
                primaryTitle = "Arbitrary primary",
                localizations = listOf(
                    MangaKoLocalizationDto(titles.first().first, titles.first().second, "Description", true),
                ),
                aliases = titles.drop(1).map { (language, title) -> MangaKoAliasDto(language, title) } +
                    MangaKoAliasDto("ja-ro", "Romanized"),
            )
            coEvery { api.searchMangas("test", 6, 0) } returns listOf(dto)
            coEvery { api.getManga("mangadex-manga", false) } returns dto

            val search = repository.searchMangaPage("test", 0, 6).single()
            val detail = repository.getManga("mangadex-manga", false)

            for (manga in listOf(search, detail)) {
                assertEquals("locale=$locale titles=$titles", expected, manga.title)
                assertEquals("Romanized", manga.altTitle)
                assertTrue(manga.title.isNotBlank())
            }
        }
    }

    @Test
    fun `Japanese title and description are exclusive and placeholder receives locale in search and detail`() = runTest {
        val locale = Locale.JAPAN
        val receivedLocales = mutableListOf<Locale>()
        val placeholder = "\u30bf\u30a4\u30c8\u30eb\u4e0d\u660e"
        val repository = MangaKoRepositoryImpl(api, unavailableTitle = {
            receivedLocales += it
            placeholder
        }, localeProvider = { locale })
        for (japanese in listOf(
            emptyList(),
            listOf(MangaKoLocalizationDto("ja", " ", " \n\t", false)),
            listOf(MangaKoLocalizationDto("ja", "Japanese", "Description JA", false)),
        )) {
            val dto = mangaDto().copy(localizations = mangaDto().localizations + japanese)
            coEvery { api.searchMangas("test", 6, 0) } returns listOf(dto)
            coEvery { api.getManga("mangadex-manga", false) } returns dto

            val search = repository.searchMangaPage("test", 0, 6).single()
            val detail = repository.getManga("mangadex-manga", false)
            val hasJapanese = japanese.singleOrNull()?.title == "Japanese"

            for (manga in listOf(search, detail)) {
                assertEquals(if (hasJapanese) "Japanese" else placeholder, manga.title)
                assertTrue(manga.title.isNotBlank())
                assertEquals("Wan Pisu", manga.altTitle)
                assertEquals(if (hasJapanese) "Description JA" else "", manga.description)
            }
        }
        assertEquals(List(4) { locale }, receivedLocales)
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
