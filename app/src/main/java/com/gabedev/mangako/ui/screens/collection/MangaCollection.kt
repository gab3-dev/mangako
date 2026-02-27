package com.gabedev.mangako.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.R
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.MangaCard

@Composable
fun MangaCollection(
    repository: LibraryRepository,
    onMangaClick: (Manga) -> Unit,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val viewModel: MangaCollectionViewModel = viewModel(
        factory = MangaCollectionViewModelFactory(repository)
    )

    val mangaCollection by viewModel.mangaCollection
    val isLoading by viewModel.isLoading
    val showIncompleteOnly by viewModel.showIncompleteOnly
    val showSpecialEditionsOnly by viewModel.showSpecialEditionsOnly

    val lifecycleOwner = LocalLifecycleOwner.current

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
                        Text(
                            text = if (searchQuery.isNotBlank() || showIncompleteOnly || showSpecialEditionsOnly) {
                                stringResource(R.string.no_results_found)
                            } else {
                                stringResource(R.string.welcome_message)
                            }
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        items(mangaCollection.size) { index ->
                            val manga = mangaCollection[index]
                            MangaCard(
                                modifier = Modifier
                                    .clickable(
                                        onClick = { onMangaClick(manga.toManga()) }
                                    ),
                                title = manga.title,
                                coverUrl = manga.coverUrl,
                                volumeTotal = manga.volumeCount,
                                volumesOwned = manga.volumeOwned,
                            )
                        }
                    }
                }
            }
        }
    }
}
