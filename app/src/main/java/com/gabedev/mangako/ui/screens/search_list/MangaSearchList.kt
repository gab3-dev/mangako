package com.gabedev.mangako.ui.screens.search_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.MangaDexRepository
import com.gabedev.mangako.ui.components.CustomLoadingIndicator
import com.gabedev.mangako.ui.components.MangaSearchItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaSearchScreen(
    modifier: Modifier = Modifier,
    apiRepository: MangaDexRepository,
    onResultClick: (manga: Manga) -> Unit
) {
    val textFieldState = rememberTextFieldState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val viewModel: MangaSearchListViewModel = viewModel(
        factory = MangaSearchListViewModelFactory(apiRepository)
    )

    val mangaList by viewModel.mangaList.collectAsState()
    val isLoading by viewModel.isMangaLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMoreManga.collectAsState()

    val noMoreManga by viewModel.noMoreManga.collectAsState()

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

    // Para armazenar o texto com debounce
    var debouncedQuery by remember { mutableStateOf("") }

    // Aplica debounce quando o usuário digita
    LaunchedEffect(searchQuery) {
        delay(600) // Espera 600ms sem mudanças
        debouncedQuery = searchQuery.trim()
    }

    // Faz a chamada à API apenas quando o texto com debounce muda
    LaunchedEffect(debouncedQuery) {
        if (debouncedQuery.isBlank()) {
            return@LaunchedEffect
        }

        viewModel.setQueryString(debouncedQuery)
    }

    // Controls expansion state of the search bar
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        searchQuery = textFieldState.text.toString()
                    },
                    onSearch = {
                        debouncedQuery = textFieldState.text.toString()
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    leadingIcon = {
                        Icons.Default.Search.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    placeholder = { Text("Pesquisar mangas") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
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
                        LazyColumn (
                            modifier = Modifier
                                .fillMaxSize(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(mangaList) { manga: Manga ->
                                MangaSearchItem(
                                    manga = manga,
                                    navigate = {
                                        onResultClick(manga)
                                    },
                                )
                            }
                            if (isLoadingMore && !mangaList.isEmpty() && !noMoreManga) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomLoadingIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}