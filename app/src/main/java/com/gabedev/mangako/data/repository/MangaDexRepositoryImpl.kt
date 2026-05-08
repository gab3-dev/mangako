package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.core.Utils
import com.gabedev.mangako.data.dto.MangaDto
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.remote.api.MangaDexAPI

class MangaDexRepositoryImpl(
    private val api: MangaDexAPI,
    private val logger: FileLogger
) : MangaDexRepository {

    private fun defaultCoverUrl(mangaId: String): String {
        return "https://uploads.mangadex.org/covers/$mangaId/default-cover.png"
    }

    private fun handleCoverUrl(mangaId: String, coverFileName: String): String {
        return "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
    }

    override suspend fun searchManga(title: String, offset: Int?): List<Manga> {
        return searchMangaPage(title, offset).map { enrichManga(it) }
    }

    override suspend fun searchMangaPage(title: String, offset: Int?, limit: Int): List<Manga> {
        return try {
            val trimmedTitle = title.trim()
            val response = api.searchMangas(
                title = trimmedTitle,
                offset = offset,
                limit = limit,
                orderRelevance = if (trimmedTitle.isBlank()) null else "desc",
                orderFollowedCount = if (trimmedTitle.isBlank()) "desc" else null
            )
            logger.log("Search Manga: API response received with ${response.data.size} mangas for title '$title'")
            response.data.mapNotNull { dto ->
                try {
                    dto.toManga()
                } catch (e: Exception) {
                    logger.logError(Throwable(message = "Error while mapping manga data: $e"))
                    null
                }
            }.distinctBy { it.id }
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while searching manga: $e"))
            emptyList()
        }
    }

    override suspend fun enrichManga(manga: Manga): Manga {
        var enriched = manga

        val authorId = enriched.authorId
        if (enriched.author.isNullOrBlank() && !authorId.isNullOrBlank()) {
            try {
                val author = api.getAuthorById(authorId)
                enriched = enriched.copy(author = author.data.attributes.name)
            } catch (e: Exception) {
                logger.logError(Throwable(message = "Error while enriching manga author: $e"))
            }
        }

        val coverId = enriched.coverId
        if (enriched.coverFileName.isNullOrBlank() && !coverId.isNullOrBlank()) {
            try {
                val cover = api.getCoverById(coverId)
                enriched = enriched.copy(
                    coverId = cover.data.id,
                    coverFileName = cover.data.attributes.fileName,
                    coverUrl = handleCoverUrl(enriched.id, cover.data.attributes.fileName)
                )
            } catch (e: Exception) {
                logger.logError(Throwable(message = "Error while enriching manga cover: $e"))
            }
        }

        try {
            val lastVolumeNumber = getLastVolumeNumber(api, enriched.id, enriched.volumeCount, logger)
            enriched = enriched.copy(volumeCount = lastVolumeNumber)
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while enriching manga volume count: $e"))
        }

        return enriched
    }

    override suspend fun getManga(id: String): Manga {
        val dto = api.getManga(id).data
        val author = api.getAuthorById(dto.relationships.find {
            it.type == "author"
        }?.id.orEmpty())
        val cover = api.getCoverById(dto.relationships.find {
            it.type == "cover_art"
        }?.id.orEmpty())
        val lastVolumeNumber = getLastVolumeNumber(api, dto, logger)
        return Manga(
            id = dto.id,
            title = Utils.handleMangaTitle(dto.attributes),
            altTitle = dto.attributes.altTitles?.find { it.containsKey("ja-ro") }?.get("ja-ro"),
            coverId = cover.data.id,
            coverFileName = cover.data.attributes.fileName,
            coverUrl = handleCoverUrl(dto.id, cover.data.attributes.fileName),
            authorId = author.data.id,
            author = author.data.attributes.name,
            description = Utils.handleMangaDescription(dto.attributes),
            status = dto.attributes.status,
            volumeCount = lastVolumeNumber,
        )
    }

    override suspend fun getCoverListByManga(manga: Manga, offset: Int?, limit: Int): List<Volume> {
        try {
            val coverResponse =
                api.getCover(manga = listOf(manga.id), offset = offset ?: 0, limit = limit)
            val covers = coverResponse.data.map { cover ->
                val volumeNumber = cover.attributes.volume?.toFloatOrNull()
                Volume(
                    id = cover.id,
                    mangaId = manga.id,
                    title = manga.title,
                    volume = volumeNumber,
                    coverUrl = handleCoverUrl(manga.id, cover.attributes.fileName),
                    owned = false,
                    isSpecialEdition = volumeNumber?.let { it % 1.0f != 0.0f } ?: true,
                    locale = cover.attributes.locale,
                    updatedAt = cover.attributes.updatedAt
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

    private suspend fun getLastVolumeNumber(api: MangaDexAPI, dto: MangaDto, logger: FileLogger): Int {
        return getLastVolumeNumber(
            api = api,
            mangaId = dto.id,
            fallbackVolumeCount = dto.attributes.lastVolume?.toFloatOrNull()?.toInt(),
            logger = logger
        )
    }

    private suspend fun getLastVolumeNumber(
        api: MangaDexAPI,
        mangaId: String,
        fallbackVolumeCount: Int?,
        logger: FileLogger
    ): Int {
        try {
            val response = api.getCover(manga = listOf(mangaId), limit = 1, orderVolume = "desc")
            val volumeStr = response.data.firstOrNull()?.attributes?.volume
            val parsed = volumeStr?.toFloatOrNull()?.toInt()
            if (parsed != null) {
                logger.log("Last volume number: $parsed for manga: $mangaId")
                return parsed
            }
        } catch (e: Exception) {
            logger.logError(Throwable(message = "Error while trying to get last volume number: $e"))
        }
        if (fallbackVolumeCount != null) {
            logger.log("Last volume number (fallback): $fallbackVolumeCount for manga: $mangaId")
            return fallbackVolumeCount
        }
        logger.log("No volume number found for manga: $mangaId")
        return 0
    }

    private fun MangaDto.toManga(): Manga {
        val authorRelationship = relationships.firstOrNull { it.type == "author" }
        val coverRelationship = relationships.firstOrNull { it.type == "cover_art" }
        val coverFileName = coverRelationship?.attributes?.fileName
        val fallbackVolumeCount = attributes.lastVolume?.toFloatOrNull()?.toInt() ?: 0

        return Manga(
            id = id,
            title = Utils.handleMangaTitle(attributes),
            altTitle = attributes.altTitles?.find { it.containsKey("ja-ro") }?.get("ja-ro"),
            type = type,
            coverId = coverRelationship?.id,
            coverFileName = coverFileName,
            coverUrl = coverFileName?.let { handleCoverUrl(id, it) } ?: defaultCoverUrl(id),
            authorId = authorRelationship?.id,
            author = authorRelationship?.attributes?.name,
            description = Utils.handleMangaDescription(attributes),
            status = attributes.status,
            volumeCount = fallbackVolumeCount,
        )
    }

    override fun log(message: Exception) {
        message.printStackTrace()
        logger.log("MangaDex LOG: $message")
    }
}
