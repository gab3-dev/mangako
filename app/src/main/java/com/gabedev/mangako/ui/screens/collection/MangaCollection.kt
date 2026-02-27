package com.gabedev.mangako.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.R
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.MangaCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaCollection(
    repository: LibraryRepository,
    onMangaClick: (Manga) -> Unit,
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

    val textFieldState = rememberTextFieldState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Apply debounce when user types
    LaunchedEffect(searchQuery) {
        delay(600) // Wait 600ms without changes
        debouncedQuery = searchQuery.trim()
    }

    // Update viewModel when debounced query changes
    LaunchedEffect(debouncedQuery) {
        viewModel.setSearchQuery(debouncedQuery)
    }

    Surface(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { isTraversalGroup = true }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_search_icon),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            placeholder = { Text(stringResource(R.string.search_collection_placeholder)) }
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {}

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
                                text = if (debouncedQuery.isNotBlank() || showIncompleteOnly || showSpecialEditionsOnly) {
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
                                .padding(horizontal = 20.dp, vertical = 10.dp)
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
}
