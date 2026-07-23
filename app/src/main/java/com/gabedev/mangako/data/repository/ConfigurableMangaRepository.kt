package com.gabedev.mangako.data.repository

import android.content.Context
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.getCatalogIntegration
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.Volume
import kotlinx.coroutines.flow.first

class ConfigurableMangaRepository(
    private val context: Context,
    private val mangaDexRepository: MangaDexRepository,
    private val mangaKoRepository: MangaDexRepository?,
) : MangaDexRepository {
    override suspend fun searchManga(title: String, offset: Int?): List<Manga> {
        return withFallback(
            mangaKo = { it.searchManga(title, offset) },
            mangaDex = { mangaDexRepository.searchManga(title, offset) },
        )
    }

    override suspend fun searchMangaPage(title: String, offset: Int?, limit: Int): List<Manga> {
        return withFallback(
            mangaKo = { it.searchMangaPage(title, offset, limit) },
            mangaDex = { mangaDexRepository.searchMangaPage(title, offset, limit) },
        )
    }

    override suspend fun enrichManga(manga: Manga): Manga {
        return withFallback(
            mangaKo = { it.enrichManga(manga) },
            mangaDex = { mangaDexRepository.enrichManga(manga) },
        )
    }

    override suspend fun getManga(id: String, refresh: Boolean): Manga {
        return withFallback(
            mangaKo = { it.getManga(id, refresh) },
            mangaDex = { mangaDexRepository.getManga(id, refresh) },
        )
    }

    override suspend fun getMangaCoverFileName(id: String): String {
        return withFallback(
            mangaKo = { it.getMangaCoverFileName(id) },
            mangaDex = { mangaDexRepository.getMangaCoverFileName(id) },
        )
    }

    override suspend fun getAuthorNameById(id: String): String {
        return withFallback(
            mangaKo = { it.getAuthorNameById(id) },
            mangaDex = { mangaDexRepository.getAuthorNameById(id) },
        )
    }

    override suspend fun getCoverListByManga(
        manga: Manga,
        offset: Int?,
        limit: Int,
        refresh: Boolean,
    ): List<Volume> {
        return withFallback(
            mangaKo = { it.getCoverListByManga(manga, offset, limit, refresh) },
            mangaDex = { mangaDexRepository.getCoverListByManga(manga, offset, limit, refresh) },
        )
    }

    override fun log(message: Exception) {
        mangaDexRepository.log(message)
    }

    private suspend fun <T> withFallback(
        mangaKo: suspend (MangaDexRepository) -> T,
        mangaDex: suspend () -> T,
    ): T {
        val useMangaKo = context.getCatalogIntegration().first() == CatalogIntegration.MANGAKO
        val repository = mangaKoRepository
        if (!useMangaKo || repository == null) return mangaDex()

        return try {
            mangaKo(repository)
        } catch (e: Exception) {
            mangaDexRepository.log(e)
            mangaDex()
        }
    }
}
