import com.gabedev.mangako.data.api.MangaDexAPI
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaRepository

class MangaRepositoryImpl(
    private val api: MangaDexAPI
) : MangaRepository {

    override suspend fun searchManga(title: String): List<Manga> =
        api.searchMangas(title = title).data.map { dto ->
            Manga(
                id = dto.id,
                title = dto.attributes.title["en"]  // pega a versão em inglês
                    ?: dto.attributes.title.values.firstOrNull().orEmpty(),
                coverUrl = dto.attributes.links.raw.orEmpty()
            )
        }

    override suspend fun getManga(id: String): Manga {
        val dto = api.getManga(id)
        return Manga(
            id = dto.id,
            title = dto.attributes.title["en"].orEmpty(),
            coverUrl = dto.attributes.links.raw.orEmpty()
        )
    }
}

