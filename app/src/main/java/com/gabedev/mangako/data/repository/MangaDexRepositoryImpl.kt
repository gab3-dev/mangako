package com.gabedev.mangako.data.repository

import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.remote.api.MangaDexAPI

class MangaDexRepositoryImpl(
    private val api: MangaDexAPI
) : MangaDexRepository {

    private fun handleCoverUrl(mangaId: String, coverFileName: String): String {
        return "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
    }

    override suspend fun searchManga(title: String): List<Manga> {
        val mangaList = api.searchMangas(title = title).data.map { dto ->
            val author = api.getAuthorById(dto.relationships.find {
                it.type == "author"
            }?.id.orEmpty())
            val cover = api.getCoverById(dto.relationships.find {
                it.type == "cover_art"
            }?.id.orEmpty())
            val coverTotal = api.getCover(manga = listOf(dto.id), limit = 1).total
            Manga(
                id = dto.id,
                title = dto.attributes.title["en"].orEmpty(),
                altTitle = dto.attributes.altTitles.find { it.containsKey("en") }?.get("en"),
                type = dto.type,
                coverId = cover.data.id,
                coverFileName = cover.data.attributes.fileName,
                coverUrl = handleCoverUrl(dto.id, cover.data.attributes.fileName),
                authorId = author.data.id,
                author = author.data.attributes.name,
                description = dto.attributes.description["pt"] ?: dto.attributes.description["en"] ?: "Nenhuma descrição disponível",
                status = dto.attributes.status,
                volumeCount = coverTotal,
            )
        }
        return mangaList
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
            title = dto.attributes.title["en"].orEmpty(),
            altTitle = dto.attributes.altTitles.find { it.containsKey("en") }?.get("en"),
            coverId = cover.data.id,
            coverFileName = cover.data.attributes.fileName,
            coverUrl = handleCoverUrl(dto.id, cover.data.attributes.fileName),
            authorId = author.data.id,
            author = author.data.attributes.name,
            description = dto.attributes.description["pt"] ?: dto.attributes.description["en"] ?: "Nenhuma descrição disponível",
            status = dto.attributes.status,
            volumeCount = coverTotal,
        )
    }

    override suspend fun getCoverListByManga(manga: Manga, offset: Int?, limit: Int): List<Volume> {
        val coverResponse = api.getCover(manga = listOf(manga.id), offset = offset ?: 0, limit = limit)
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
    }

    override suspend fun getMangaCoverFileName(id: String): String {
        val coverResponse = api.getCoverById(id)
        return coverResponse.data.attributes.fileName
    }

    override suspend fun getAuthorNameById(id: String): String {
        val author = api.getAuthorById(id)
        return author.data.attributes.name
    }
}