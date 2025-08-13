package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.dto.AuthorResponseDTO
import com.gabedev.mangako.data.dto.CoverArtResponseDTO
import com.gabedev.mangako.data.dto.MangaDto
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.remote.api.MangaDexAPI

class MangaDexRepositoryImpl(
    private val api: MangaDexAPI,
    private val logger: FileLogger
) : MangaDexRepository {

    private fun handleCoverUrl(mangaId: String, coverFileName: String): String {
        return "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
    }

    override suspend fun searchManga(title: String): List<Manga> {
        val mangaListWithAuthorAndCover = mutableListOf<Manga>()
        val mangaList: List<MangaDto>
        try {
            val tmp = api.searchMangas(title = title)
            logger.log("Search Manga: API response received with $tmp mangas for title '$title'")
            mangaList = tmp.data
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while searching manga: $e"))
            return emptyList()
        }
        if (mangaList.isEmpty()) {
            logger.log("Search Manga: No mangas found for title '$title'")
            return emptyList()
        }
        mangaList.forEach { dto ->
            val author = getAuthor(api, dto, logger)
            val cover = getCover(api, dto, logger)
            val coverTotal = getCoverTotal(api, dto, logger)
            try {
                val mangaTitle = dto.attributes.title?.get("pt") ?: dto.attributes.title?.get("en")
                mangaListWithAuthorAndCover.add(
                    Manga(
                        id = dto.id,
                        title = mangaTitle.orEmpty(),
                        altTitle = dto.attributes.altTitles?.find { it.containsKey("en") }
                            ?.get("en"),
                        type = dto.type,
                        coverId = cover?.data?.id,
                        coverFileName = cover?.data?.attributes?.fileName,
                        coverUrl = if (cover != null) {
                            handleCoverUrl(dto.id, cover.data.attributes.fileName)
                        } else {
                            "https://uploads.mangadex.org/covers/${dto.id}/default-cover.png"
                        },
                        authorId = author?.data?.id,
                        author = author?.data?.attributes?.name,
                        description = dto.attributes.description?.get("pt")
                            ?: dto.attributes.description?.get("en")
                            ?: "Nenhuma descrição disponível",
                        status = dto.attributes.status,
                        volumeCount = coverTotal,
                    )
                )
            } catch (e: Exception) {
                logger.logError(Throwable(message = "Error while try to add manga data, to the final list: $e"))
                Manga(
                    id = dto.id,
                    title = dto.attributes.title?.get("pt")
                        ?: dto.attributes.title?.get("en")
                        ?: "Título não encontrado",
                    coverUrl = "https://uploads.mangadex.org/covers/${dto.id}/default-cover.png",
                    description = "Nenhuma descrição disponível"
                )
            }
        }
        logger.log("Search Manga: Found ${mangaList.size} mangas for title '$title'")
        if (mangaList.isNotEmpty()) {
            return mangaListWithAuthorAndCover
        }
        return emptyList()
    }

    override suspend fun getManga(id: String): Manga {
        val dto = api.getManga(id)
        val author = api.getAuthorById(dto.relationships.find {
            it.type == "author"
        }?.id.orEmpty())
        val cover = api.getCoverById(dto.id)
        val coverTotal = api.getCover(manga = listOf(dto.id), limit = 1).total
        return Manga(
            id = dto.id,
            title = dto.attributes.title?.get("pt")
                ?: dto.attributes.title?.get("en")
                ?: "",
            altTitle = dto.attributes.altTitles?.find { it.containsKey("en") }?.get("en"),
            coverId = cover.data.id,
            coverFileName = cover.data.attributes.fileName,
            coverUrl = handleCoverUrl(dto.id, cover.data.attributes.fileName),
            authorId = author.data.id,
            author = author.data.attributes.name,
            description = dto.attributes.description?.get("pt")
                ?: dto.attributes.description?.get("en")
                ?: "Nenhuma descrição disponível",
            status = dto.attributes.status,
            volumeCount = coverTotal,
        )
    }

    override suspend fun getCoverListByManga(manga: Manga, offset: Int?, limit: Int): List<Volume> {
        try {
            val coverResponse =
                api.getCover(manga = listOf(manga.id), offset = offset ?: 0, limit = limit)
            val covers = coverResponse.data.filter {
                it.attributes.volume.toIntOrNull() != null
            }.map { cover ->
                Volume(
                    id = cover.id,
                    mangaId = manga.id,
                    title = manga.title,
                    volume = cover.attributes.volume.toIntOrNull(),
                    coverUrl = handleCoverUrl(manga.id, cover.attributes.fileName),
                    owned = false
                )
            }
            return covers
        } catch (e: Exception) {
            logger.logError(e)
            return emptyList()
        }
    }

    override suspend fun getMangaCoverFileName(id: String): String {
        val coverResponse = api.getCoverById(id)
        return coverResponse.data.attributes.fileName
    }

    override suspend fun getAuthorNameById(id: String): String {
        val author = api.getAuthorById(id)
        return author.data.attributes.name
    }

    private suspend fun getAuthor(
        api: MangaDexAPI,
        dto: MangaDto,
        logger: FileLogger
    ): AuthorResponseDTO? {
        val author = try {
            api.getAuthorById(dto.relationships.find {
                it.type == "author"
            }?.id.orEmpty())
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while trying to get author data: $e"))
            null
        }
        if (author != null) {
            logger.log("Author data fetched successfully: ${author.data.id}")
            return author
        }
        logger.log("No author data found for manga: ${dto.id}")
        return null
    }

    private suspend fun getCover(
        api: MangaDexAPI,
        dto: MangaDto,
        logger: FileLogger
    ): CoverArtResponseDTO? {
        val cover = try {
            api.getCoverById(dto.relationships.find {
                it.type == "cover_art"
            }?.id.orEmpty())
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while trying to get cover data: $e"))
            null
        }
        if (cover != null) {
            logger.log("Cover data fetched successfully: ${cover.data.id}")
            return cover
        }
        logger.log("No cover data found for manga: ${dto.id}")
        return null
    }

    private suspend fun getCoverTotal(api: MangaDexAPI, dto: MangaDto, logger: FileLogger): Int {
        val coverTotal = try {
            api.getCover(manga = listOf(dto.id), limit = 1).total
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while trying to get cover total: $e"))
            0
        }
        logger.log("Cover total fetched successfully: $coverTotal for manga: ${dto.id}")
        return coverTotal
    }
}