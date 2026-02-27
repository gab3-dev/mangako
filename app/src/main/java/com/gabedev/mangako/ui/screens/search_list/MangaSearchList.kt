package com.gabedev.mangako.ui.screens.search_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import com.gabedev.mangako.ui.components.CustomLoadingIndicator
import com.gabedev.mangako.ui.components.MangaSearchItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaSearchScreen(
    modifier: Modifier = Modifier,
    apiRepository: MangaDexRepository,
    searchQuery: String,
    onResultClick: (manga: Manga) -> Unit
) {
    val viewModel: MangaSearchListViewModel = viewModel(
        factory = MangaSearchListViewModelFactory(apiRepository)
    )

    val mangaList by viewModel.mangaList.collectAsState()
    val isLoading by viewModel.isMangaLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMoreManga.collectAsState()
    val noMoreManga by viewModel.noMoreManga.collectAsState()
    val listState = rememberLazyListState()

    // Forward debounced search query to ViewModel
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) return@LaunchedEffect
        viewModel.setQueryString(searchQuery)
    }

    // Infinite scroll: load more when reaching the end
    LaunchedEffect(listState, noMoreManga) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = info.totalItemsCount
            lastVisible to totalItems
        }.collect { (lastVisible, totalItems) ->
            if (!noMoreManga && lastVisible >= totalItems - 1) {
                viewModel.loadMangaList()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            if (isLoading && mangaList.isEmpty()) {
                CircularWavyProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 32.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    items(mangaList) { manga: Manga ->
                        MangaSearchItem(
                            manga = manga,
                            navigate = {
                                onResultClick(manga)
                            },
                        )
                    }
                    if (isLoadingMore && mangaList.isNotEmpty() && !noMoreManga) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomLoadingIndicator(
                                    Modifier
                                        .width(50.dp)
                                        .height(50.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}