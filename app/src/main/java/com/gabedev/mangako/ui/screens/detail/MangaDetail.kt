package com.gabedev.mangako.ui.screens.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.data.dto.CoverArtDTO
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.MangaCard
import com.gabedev.mangako.ui.components.MangaCoverImage

fun handleCoverUrl(mangaId: String, coverFileName: String): String {
    return "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
}

@Composable
fun MangaDetail(
    manga: Manga,
    api: MangaDexAPI,
    repository: LibraryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MangaDetailViewModel = viewModel(
        factory = MangaDetailViewModelFactory(repository, manga.id)
    )
    val addResult by viewModel.addResult.collectAsState()
    val removeResult by viewModel.removeResult.collectAsState()
    val isMangaInLibrary by viewModel.isMangaInLibrary.collectAsState()

    LaunchedEffect(addResult) {
        addResult?.let {
            if (it.isSuccess) {
                Toast.makeText(context, "Manga adicionado à biblioteca!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erro ao adicionar manga", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearAddResult()
        }
    }

    LaunchedEffect(removeResult) {
        removeResult?.let {
            if (it.isSuccess) {
                Toast.makeText(context, "Manga removido da biblioteca!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erro ao remover manga", Toast.LENGTH_SHORT).show()
            }
            viewModel.clearRemoveResult()
        }
    }

    var covers by remember { mutableStateOf<List<CoverArtDTO>>(emptyList()) }
    LaunchedEffect(Unit) {
        covers = try {
            api.getCover(manga = listOf(manga.id)).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item (span = { GridItemSpan(maxLineSpan) }) {
            MangaHeader(
                title = manga.title,
                enTitle = manga.altTitle,
                author = manga.author,
                description = manga.description,
                situation = manga.status,
                coverUrl = manga.coverUrl,
                volume = covers.size
            )
        }
        item (span = { GridItemSpan(maxLineSpan) }) {
            if (isMangaInLibrary) {
                Button(
                    onClick = {
                        viewModel.removeManga()
                    }
                ) {
                    Icon (
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover manga da coleção",
                    )
                    Text(
                        text = "Remover manga da coleção",
                    )
                }
            } else {
                Button(
                    onClick = {
                        viewModel.addManga(
                            manga
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar à coleção"
                    )
                    Text(
                        text = "Adicionar à coleção",
                    )
                }
            }
        }
        items(covers) { cover ->
            MangaCard(
                title = manga.title,
                coverUrl = handleCoverUrl(manga.id, cover.attributes.fileName),
                owned = false,
                isVolumeCard = true,
                volume = cover.attributes.volume.toIntOrNull(),
            )
        }
    }
}

@Composable
fun MangaHeader(
    modifier: Modifier = Modifier,
    title: String,
    enTitle: String? = null,
    author: String? = null,
    description: String,
    situation: String? = null,
    coverUrl: String? = null,
    volume: Int? = null
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coverUrl != null) {
                MangaCoverImage(
                    imageUrl = coverUrl,
                    modifier = Modifier.height(190.dp),
                    contentDescription = title
                )
            } else {
                MangaCoverImage(
                    imageUrl = null,
                    modifier = Modifier.height(190.dp),
                    contentDescription = title
                )
            }
            Column {
                Column {
                    SimpleText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (enTitle != null) {
                        SimpleText(
                            text = enTitle,
                        )
                    }
                }
                Column {
                    SimpleText(
                        text = "Autor: ${author ?: "Desconhecido"}",
                    )
                    SimpleText(
                        text = "Situação: ${situation ?: "Desconhecida"}",
                    )
                    SimpleText (
                        text = "Volumes: ${volume?.toString() ?: "N/A"}",
                    )
                }
            }
        }
        SimpleText(
            text = description,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun SimpleText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(end = 16.dp, bottom = 4.dp)
    )
}