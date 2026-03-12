package com.gabedev.mangako.ui.screens.collection

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.R
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.ConfirmDialog
import com.gabedev.mangako.ui.components.MangaCard

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaCollection(
    repository: LibraryRepository,
    onMangaClick: (Manga) -> Unit,
    searchQuery: String,
    modifier: Modifier = Modifier,
    onExploreSearch: (String) -> Unit = {}
) {
    val viewModel: MangaCollectionViewModel = viewModel(
        factory = MangaCollectionViewModelFactory(repository)
    )

    val mangaCollection by viewModel.mangaCollection
    val isLoading by viewModel.isLoading
    val showIncompleteOnly by viewModel.showIncompleteOnly
    val showSpecialEditionsOnly by viewModel.showSpecialEditionsOnly
    val isMultiSelectActive by viewModel.isMultiSelectActive

    val lifecycleOwner = LocalLifecycleOwner.current

    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_remove_selected_title),
            text = stringResource(R.string.dialog_remove_selected_text, viewModel.selectedIds.value.size),
            onConfirm = {
                viewModel.removeSelectedFromLibrary()
                showRemoveDialog = false
            },
            onDismiss = {
                showRemoveDialog = false
            },
        )
    }

    // Observa retorno à tela
    LaunchedEffect(lifecycleOwner) {
        viewModel.loadLibrary()
    }

    // Forward debounced search query to ViewModel
    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showIncompleteOnly,
                    onClick = { viewModel.toggleIncompleteFilter() },
                    label = { Text(stringResource(R.string.filter_incomplete)) }
                )
                FilterChip(
                    selected = showSpecialEditionsOnly,
                    onClick = { viewModel.toggleSpecialEditionsFilter() },
                    label = { Text(stringResource(R.string.filter_special_editions)) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (mangaCollection.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || showIncompleteOnly || showSpecialEditionsOnly) {
                                    stringResource(R.string.no_results_found)
                                } else {
                                    stringResource(R.string.welcome_message)
                                }
                            )
                            if (searchQuery.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        viewModel.clearSearchQuery()
                                        onExploreSearch(searchQuery)
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_on_explore, searchQuery),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            items(mangaCollection.size) { index ->
                                val manga = mangaCollection[index]
                                MangaCard(
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (!isMultiSelectActive) {
                                                    onMangaClick(manga.toManga())
                                                } else {
                                                    viewModel.toggleSelection(manga.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isMultiSelectActive) {
                                                    viewModel.toggleSelection(manga.id)
                                                }
                                            }
                                        ),
                                    title = manga.title,
                                    coverUrl = manga.coverUrl,
                                    volumeTotal = manga.volumeCount,
                                    volumesOwned = manga.volumeOwned,
                                    selected = viewModel.selectedIds.value.contains(manga.id),
                                )
                            }
                        }

                        if (isMultiSelectActive) {
                            HorizontalFloatingToolbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = -ScreenOffset)
                                    .zIndex(1f),
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                                expanded = true
                            ) {
                                IconButton(
                                    onClick = {
                                        showRemoveDialog = true
                                    },
                                    enabled = viewModel.selectedIds.value.isNotEmpty(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_remove_selected),
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        viewModel.finishMultiSelect()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = stringResource(R.string.cd_stop_multi_select),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.selectAll(mangaCollection.map { it.id }.toSet())
                                    },
                                    enabled = viewModel.selectedIds.value.size < mangaCollection.size && isMultiSelectActive,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = stringResource(R.string.cd_select_all),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.clearSelection()
                                    },
                                    enabled = viewModel.selectedIds.value.isNotEmpty() && isMultiSelectActive,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Deselect,
                                        contentDescription = stringResource(R.string.cd_deselect),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
