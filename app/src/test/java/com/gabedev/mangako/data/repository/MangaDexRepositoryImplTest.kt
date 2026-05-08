package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.dto.AttributesDto
import com.gabedev.mangako.data.dto.AuthorAttributes
import com.gabedev.mangako.data.dto.AuthorData
import com.gabedev.mangako.data.dto.AuthorResponseDTO
import com.gabedev.mangako.data.dto.CoverArtAttributes
import com.gabedev.mangako.data.dto.CoverArtAttributesDTO
import com.gabedev.mangako.data.dto.CoverArtDTO
import com.gabedev.mangako.data.dto.CoverArtData
import com.gabedev.mangako.data.dto.CoverArtListResponseDTO
import com.gabedev.mangako.data.dto.CoverArtResponseDTO
import com.gabedev.mangako.data.dto.LinksDto
import com.gabedev.mangako.data.dto.MangaDto
import com.gabedev.mangako.data.dto.MangaListResponse
import com.gabedev.mangako.data.dto.MangaResponseDto
import com.gabedev.mangako.data.dto.RelationshipDto
import com.gabedev.mangako.data.dto.RelationshipAttributesDto
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MangaDexRepositoryImplTest {

    private lateinit var api: MangaDexAPI
    private lateinit var logger: FileLogger
    private lateinit var repository: MangaDexRepositoryImpl

    private fun createMangaDto(
        id: String = "manga-1",
        titleEn: String = "One Piece"
    ): MangaDto {
        return MangaDto(
            id = id,
            type = "manga",
            attributes = AttributesDto(
                title = mapOf("en" to titleEn),
                altTitles = listOf(mapOf("ja-ro" to "Wan Pīsu")),
                description = mapOf("en" to "A pirate adventure"),
                isLocked = false,
                links = LinksDto(null, null, null, null, null, null, null),
                originalLanguage = "ja",
                lastVolume = null,
                lastChapter = null,
                publicationDemographic = null,
                status = "ongoing",
                year = 1997,
                contentRating = "safe",
                tags = emptyList(),
                state = "published",
                chapterNumbersResetOnNewVolume = false,
                createdAt = "2020-01-01",
                updatedAt = "2024-01-01",
                version = 1,
                availableTranslatedLanguages = listOf("en"),
                latestUploadedChapter = null
            ),
            relationships = listOf(
                RelationshipDto(id = "author-1", type = "author"),
                RelationshipDto(id = "cover-1", type = "cover_art")
            )
        )
    }

    private fun createAuthorResponse() = AuthorResponseDTO(
        result = "ok",
        response = "entity",
        data = AuthorData(
            id = "author-1",
            type = "author",
            attributes = AuthorAttributes(
                name = "Eiichiro Oda",
                createdAt = "2020-01-01",
                updatedAt = "2024-01-01",
                version = 1
            ),
            relationships = emptyList()
        )
    )

    private fun createCoverResponse() = CoverArtResponseDTO(
        result = "ok",
        response = "entity",
        data = CoverArtData(
            id = "cover-1",
            type = "cover_art",
            attributes = CoverArtAttributes(
                description = "",
                volume = "1",
                fileName = "cover-file.jpg",
                locale = "ja",
                createdAt = "2020-01-01",
                updatedAt = "2024-01-01",
                version = 1
            ),
            relationships = emptyList()
        )
    )

    private fun createCoverListResponse(
        total: Int = 10,
        volume: String = "1"
    ) = CoverArtListResponseDTO(
        result = "ok",
        response = "collection",
        data = listOf(
            CoverArtDTO(
                id = "cover-1",
                type = "cover_art",
                attributes = CoverArtAttributesDTO(
                    description = "",
                    volume = volume,
                    fileName = "vol1.jpg",
                    locale = "ja",
                    createdAt = "2020-01-01",
                    updatedAt = "2024-01-01",
                    version = 1
                )
            )
        ),
        limit = 50,
        offset = 0,
        total = total
    )

    private fun createEmptyCoverListResponse() = CoverArtListResponseDTO(
        result = "ok",
        response = "collection",
        data = emptyList(),
        limit = 50,
        offset = 0,
        total = 0
    )

    @Before
    fun setup() {
        api = mockk()
        logger = mockk(relaxed = true)
        repository = MangaDexRepositoryImpl(api, logger)
    }

    // --- searchManga tests ---

    @Test
    fun `searchManga returns mapped manga list`() = runTest {
        val mangaDto = createMangaDto()
        coEvery { api.searchMangas(title = "One Piece", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns createCoverListResponse()

        val result = repository.searchManga("One Piece")

        assertEquals(1, result.size)
        assertEquals("manga-1", result[0].id)
        assertEquals("One Piece", result[0].title)
        assertEquals("Eiichiro Oda", result[0].author)
    }

    @Test
    fun `searchManga returns empty list when no results`() = runTest {
        coEvery { api.searchMangas(title = "xyz", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", emptyList(), 6, 0, 0)

        val result = repository.searchManga("xyz")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchManga returns empty list on API exception`() = runTest {
        coEvery { api.searchMangas(title = any(), offset = any(), limit = any(), orderRelevance = any(), orderFollowedCount = any()) } throws RuntimeException("Network error")

        val result = repository.searchManga("test")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchMangaPage maps base results without per item metadata calls`() = runTest {
        val mangaDto = createMangaDto().copy(
            attributes = createMangaDto().attributes.copy(lastVolume = "12")
        )
        coEvery {
            api.searchMangas(
                title = "One Piece",
                offset = 0,
                limit = any(),
                orderRelevance = any(),
                orderFollowedCount = any()
            )
        } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)

        val result = repository.searchMangaPage("One Piece", 0)

        assertEquals(1, result.size)
        assertEquals("manga-1", result[0].id)
        assertEquals(12, result[0].volumeCount)
        coVerify(exactly = 0) { api.getAuthorById(any()) }
        coVerify(exactly = 0) { api.getCoverById(any()) }
        coVerify(exactly = 0) { api.getCover(manga = any(), limit = 1, orderVolume = "desc") }
    }

    @Test
    fun `searchMangaPage uses included relationship attributes when available`() = runTest {
        val mangaDto = createMangaDto().copy(
            relationships = listOf(
                RelationshipDto(
                    id = "author-1",
                    type = "author",
                    attributes = RelationshipAttributesDto(name = "Eiichiro Oda")
                ),
                RelationshipDto(
                    id = "cover-1",
                    type = "cover_art",
                    attributes = RelationshipAttributesDto(fileName = "included-cover.jpg")
                )
            )
        )
        coEvery {
            api.searchMangas(
                title = "One Piece",
                offset = 0,
                limit = any(),
                orderRelevance = any(),
                orderFollowedCount = any()
            )
        } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)

        val result = repository.searchMangaPage("One Piece", 0)

        assertEquals("Eiichiro Oda", result[0].author)
        assertEquals("included-cover.jpg", result[0].coverFileName)
        assertTrue(result[0].coverUrl.contains("included-cover.jpg"))
        coVerify(exactly = 0) { api.getAuthorById(any()) }
        coVerify(exactly = 0) { api.getCoverById(any()) }
    }

    @Test
    fun `searchMangaPage uses stable ordering for search and discovery pages`() = runTest {
        coEvery {
            api.searchMangas(
                title = any(),
                offset = any(),
                limit = any(),
                orderRelevance = any(),
                orderFollowedCount = any()
            )
        } returns MangaListResponse("ok", "collection", emptyList(), 6, 0, 0)

        repository.searchMangaPage("One Piece", 0)
        repository.searchMangaPage("", 6)

        coVerify {
            api.searchMangas(
                title = "One Piece",
                offset = 0,
                limit = 6,
                orderRelevance = "desc",
                orderFollowedCount = null
            )
        }
        coVerify {
            api.searchMangas(
                title = "",
                offset = 6,
                limit = 6,
                orderRelevance = null,
                orderFollowedCount = "desc"
            )
        }
    }

    @Test
    fun `enrichManga falls back to author and cover endpoints when relationship attributes are missing`() = runTest {
        val manga = Manga(
            id = "manga-1",
            title = "One Piece",
            coverId = "cover-1",
            coverUrl = "placeholder",
            authorId = "author-1",
            description = "A pirate adventure"
        )
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns
                createCoverListResponse(volume = "27")

        val result = repository.enrichManga(manga)

        assertEquals("Eiichiro Oda", result.author)
        assertEquals("cover-file.jpg", result.coverFileName)
        assertEquals(27, result.volumeCount)
    }

    // --- getManga tests ---

    @Test
    fun `getManga returns mapped manga`() = runTest {
        val mangaDto = createMangaDto()
        coEvery { api.getManga("manga-1") } returns MangaResponseDto("ok", "entity", mangaDto)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns createCoverListResponse()

        val result = repository.getManga("manga-1")

        assertEquals("manga-1", result.id)
        assertEquals("One Piece", result.title)
        assertEquals("Eiichiro Oda", result.author)
        assertEquals("cover-1", result.coverId)
        assertTrue(result.coverUrl.contains("cover-file.jpg"))
    }

    // --- getCoverListByManga tests ---

    @Test
    fun `getCoverListByManga returns volume list`() = runTest {
        val manga = Manga(
            id = "manga-1", title = "One Piece",
            coverUrl = "url", description = "desc"
        )
        coEvery { api.getCover(manga = listOf("manga-1"), offset = 0, limit = 50) } returns
                createCoverListResponse()

        val result = repository.getCoverListByManga(manga)

        assertEquals(1, result.size)
        assertEquals("cover-1", result[0].id)
        assertEquals("manga-1", result[0].mangaId)
        assertTrue(result[0].coverUrl.contains("vol1.jpg"))
    }

    @Test
    fun `getCoverListByManga returns empty on exception`() = runTest {
        val manga = Manga(
            id = "manga-1", title = "Test",
            coverUrl = "url", description = "desc"
        )
        coEvery { api.getCover(manga = any(), offset = any(), limit = any()) } throws RuntimeException("error")

        val result = repository.getCoverListByManga(manga)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCoverListByManga with custom offset and limit`() = runTest {
        val manga = Manga(
            id = "manga-1", title = "Test",
            coverUrl = "url", description = "desc"
        )
        coEvery { api.getCover(manga = listOf("manga-1"), offset = 50, limit = 25) } returns
                createCoverListResponse()

        val result = repository.getCoverListByManga(manga, offset = 50, limit = 25)

        assertEquals(1, result.size)
    }

    @Test
    fun `getCoverListByManga handles null volume field gracefully`() = runTest {
        val manga = Manga(
            id = "manga-1", title = "Soul Eater",
            coverUrl = "url", description = "desc"
        )
        val responseWithNullVolume = CoverArtListResponseDTO(
            result = "ok",
            response = "collection",
            data = listOf(
                CoverArtDTO(
                    id = "cover-promo",
                    type = "cover_art",
                    attributes = CoverArtAttributesDTO(
                        description = "Promotional cover",
                        volume = null,
                        fileName = "promo.jpg",
                        locale = "ja",
                        createdAt = "2020-01-01",
                        updatedAt = "2024-01-01",
                        version = 1
                    )
                ),
                CoverArtDTO(
                    id = "cover-vol1",
                    type = "cover_art",
                    attributes = CoverArtAttributesDTO(
                        description = "",
                        volume = "1",
                        fileName = "vol1.jpg",
                        locale = "ja",
                        createdAt = "2020-01-01",
                        updatedAt = "2024-01-01",
                        version = 1
                    )
                )
            ),
            limit = 50,
            offset = 0,
            total = 2
        )
        coEvery { api.getCover(manga = listOf("manga-1"), offset = 0, limit = 50) } returns
                responseWithNullVolume

        val result = repository.getCoverListByManga(manga)
        val promoCover = result.firstOrNull { it.id == "cover-promo" }
        val volumeOneCover = result.firstOrNull { it.id == "cover-vol1" }

        assertEquals(2, result.size)
        assertTrue(promoCover != null)
        assertTrue(volumeOneCover != null)
        assertNull(promoCover?.volume)
        assertEquals(1.0f, volumeOneCover?.volume)
        assertTrue(promoCover!!.isSpecialEdition)
        assertFalse(volumeOneCover!!.isSpecialEdition)
    }

    // --- getMangaCoverFileName tests ---

    @Test
    fun `getMangaCoverFileName returns filename`() = runTest {
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()

        val result = repository.getMangaCoverFileName("cover-1")

        assertEquals("cover-file.jpg", result)
    }

    // --- getAuthorNameById tests ---

    @Test
    fun `getAuthorNameById returns author name`() = runTest {
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()

        val result = repository.getAuthorNameById("author-1")

        assertEquals("Eiichiro Oda", result)
    }

    // --- getLastVolumeNumber tests (via searchManga/getManga) ---

    @Test
    fun `searchManga sets volumeCount to last volume number from cover API`() = runTest {
        val mangaDto = createMangaDto()
        coEvery { api.searchMangas(title = "One Piece", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns
                createCoverListResponse(volume = "27")

        val result = repository.searchManga("One Piece")

        assertEquals(27, result[0].volumeCount)
    }

    @Test
    fun `searchManga truncates decimal volume number to Int`() = runTest {
        val mangaDto = createMangaDto()
        coEvery { api.searchMangas(title = "One Piece", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns
                createCoverListResponse(volume = "15.5")

        val result = repository.searchManga("One Piece")

        assertEquals(15, result[0].volumeCount)
    }

    @Test
    fun `searchManga falls back to lastVolume when cover has no parseable volume`() = runTest {
        val mangaDto = createMangaDto().copy(
            attributes = createMangaDto().attributes.copy(lastVolume = "42")
        )
        coEvery { api.searchMangas(title = "One Piece", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } returns
                createEmptyCoverListResponse()

        val result = repository.searchManga("One Piece")

        assertEquals(42, result[0].volumeCount)
    }

    @Test
    fun `searchManga returns 0 volumeCount on API exception`() = runTest {
        val mangaDto = createMangaDto()
        coEvery { api.searchMangas(title = "One Piece", offset = null, limit = any(), orderRelevance = any(), orderFollowedCount = any()) } returns
                MangaListResponse("ok", "collection", listOf(mangaDto), 6, 0, 1)
        coEvery { api.getAuthorById("author-1") } returns createAuthorResponse()
        coEvery { api.getCoverById("cover-1") } returns createCoverResponse()
        coEvery { api.getCover(manga = listOf("manga-1"), limit = 1, orderVolume = "desc") } throws
                RuntimeException("Network error")

        val result = repository.searchManga("One Piece")

        assertEquals(0, result[0].volumeCount)
    }

    // --- log tests ---

    @Test
    fun `log delegates to logger`() {
        val exception = Exception("test")

        repository.log(exception)

        verify { logger.log(match { it.contains("MangaDex LOG") }) }
    }
}
