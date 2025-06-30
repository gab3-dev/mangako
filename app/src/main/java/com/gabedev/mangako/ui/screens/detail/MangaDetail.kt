package com.gabedev.mangako.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import com.gabedev.mangako.ui.components.ConfirmDialog
import com.gabedev.mangako.ui.components.MangaCard
import com.gabedev.mangako.ui.components.MangaCoverImage

@Composable
fun MangaDetail(
    manga: Manga,
    apiRepository: MangaDexRepository,
    localRepository: LibraryRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: MangaDetailViewModel = viewModel(
        factory = MangaDetailViewModelFactory(apiRepository, localRepository, manga)
    )
    val volumeList by viewModel.volumeList.collectAsState()
    val isCoverLoading by viewModel.isVolumeLoading.collectAsState()
    val addResult by viewModel.addResult.collectAsState()
    val removeResult by viewModel.removeResult.collectAsState()
    val isMangaInLibrary by viewModel.isMangaInLibrary.collectAsState()
    val listState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        ConfirmDialog(
            onConfirm = {
                viewModel.removeMangaFromLibrary()
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            }
        )
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == manga.volumeCount) {
                return@derivedStateOf false // já carregou todos os volumes
            }
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 3 // quando estiver perto do fim
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreVolumes()
        }
    }

    LaunchedEffect(addResult) {
        addResult?.let {
            if (it.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = "Manga adicionado à biblioteca!",
                    duration = SnackbarDuration.Short,
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "Erro ao adicionar manga!",
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.clearAddResult()
        }
    }

    LaunchedEffect(removeResult) {
        removeResult?.let {
            if (it.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = "Manga removido da biblioteca!",
                    duration = SnackbarDuration.Short,
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "Erro ao remover manga!",
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.clearRemoveResult()
        }
    }

    Scaffold (
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        }
    ) { contentPadding ->
        LazyVerticalGrid(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            state = listState,
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MangaHeader(
                    title = manga.title,
                    enTitle = manga.altTitle,
                    author = manga.author,
                    description = manga.description,
                    situation = manga.status,
                    coverUrl = manga.coverUrl,
                    volume = manga.volumeCount
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (isMangaInLibrary) {
                    Button(
                        onClick = {
                            showDialog = true
                        }
                    ) {
                        Icon(
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
                            viewModel.addMangaToLibrary(
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
            if (isCoverLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Carregando capas...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (volumeList.isEmpty() && !isCoverLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Nenhum volume encontrado.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@LazyVerticalGrid
            } else {
                items(volumeList) { volume ->
                    MangaCard(
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    viewModel.markVolumeAsOwned(volume)
                                }
                            ),
                        title = manga.title,
                        coverUrl = volume.coverUrl,
                        owned = volume.owned,
                        isVolumeCard = true,
                        volume = volume.volume,
                    )
                }
            }
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