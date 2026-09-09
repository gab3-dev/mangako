package com.gabedev.mangako.data.repository

import com.gabedev.mangako.core.Utils
import com.gabedev.mangako.data.dto.MangaKoMangaDto
import com.gabedev.mangako.data.dto.MangaKoVolumeDto
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import java.util.Locale

class MangaKoRepositoryImpl(
    private val api: MangaKoAPI,
    private val localeProvider: () -> Locale = { Locale.getDefault() },
) : MangaDexRepository {
    override suspend fun searchManga(title: String, offset: Int?): List<Manga> {
        return searchMangaPage(title, offset, 10)
    }

    override suspend fun searchMangaPage(title: String, offset: Int?, limit: Int): List<Manga> {
        return api.searchMangas(
            title = title.trim().ifBlank { null },
            limit = limit,
            offset = offset ?: 0,
        ).map { it.toManga() }
    }

    override suspend fun enrichManga(manga: Manga): Manga = manga

    override suspend fun getManga(id: String, refresh: Boolean): Manga {
        return api.getManga(id, refresh).toManga()
    }

    override suspend fun getMangaCoverFileName(id: String): String {
        return api.getManga(id).covers.firstOrNull { it.isPrimary }?.sourceUrl.orEmpty()
    }

    override suspend fun getAuthorNameById(id: String): String = ""

    override suspend fun getCoverListByManga(
        manga: Manga,
        offset: Int?,
        limit: Int,
        refresh: Boolean,
    ): List<Volume> {
        return api.getVolumes(
            mangaRef = manga.id,
            limit = limit,
            offset = offset ?: 0,
            refresh = refresh,
        ).map { it.toVolume(manga) }
    }

    override fun log(message: Exception) = Unit

    private fun MangaKoMangaDto.toManga(): Manga {
        val title = localizations.localizedTitle() ?: primaryTitle
        return Manga(
            id = mangaDexId ?: id,
            title = title,
            altTitle = aliases.firstOrNull { it.language.normalized() == "ja-ro" }?.title,
            coverId = covers.firstOrNull { it.isPrimary }?.mangaDexCoverId
                ?: covers.firstOrNull { it.isPrimary }?.id,
            coverUrl = covers.firstOrNull { it.isPrimary }?.sourceUrl.orEmpty(),
            author = authors.firstOrNull()?.name,
            description = Utils.localizedDescription(
                localizations.sortedByDescending { it.isPrimary }.map { it.language to it.description },
                localeProvider(),
            ),
            status = status,
            volumeCount = latestVolumeNumber?.toFloatOrNull()?.toInt() ?: 0,
        )
    }

    private fun MangaKoVolumeDto.toVolume(manga: Manga): Volume {
        return Volume(
            id = mangaDexCoverId ?: id,
            mangaId = manga.id,
            title = manga.title,
            coverUrl = sourceUrl,
            volume = volume?.toFloatOrNull(),
            locale = locale,
            isSpecialEdition = isSpecialEdition,
            createdAt = sourceCreatedAt,
            updatedAt = sourceUpdatedAt ?: updatedAt,
        )
    }

    private fun List<com.gabedev.mangako.data.dto.MangaKoLocalizationDto>.localizedTitle(): String? {
        return firstOrNull { it.language.normalized() == "en" }?.title
            ?: firstOrNull { it.language.normalized() == "ja-ro" }?.title
            ?: firstOrNull { it.language.normalized() == "pt-br" }?.title
    }

    private fun String.normalized(): String = lowercase().replace('_', '-')
}
