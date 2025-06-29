package com.gabedev.mangako.ui.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.ui.components.MangaSearchItem

@Composable
fun MangaSearchScreen(
    mangas: List<Manga> = emptyList(),
    isLoading: Boolean = false,
    updateTopBarVisibility: (Boolean) -> Unit = {},
    onResultClick: (manga: Manga) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        shadowElevation = 8.dp,
    ) {
        Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollable(
                            orientation = Orientation.Vertical,
                            state = rememberScrollableState { delta ->
                                // Try to optimize the delta value to avoid too frequent updates
                                if (delta > 20f) {
                                    updateTopBarVisibility(true)
                                } else if (delta < -20f) {
                                    updateTopBarVisibility(false)
                                } else {
                                    // No change, do nothing
                                }
                                delta
                            }
                        ),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                ) {
                    items(mangas) { manga: Manga ->
                        MangaSearchItem(
                            manga = manga,
                            navigate = {
                                onResultClick(manga)
                            },
                        )
                    }
                }
            }
        }
    }
}