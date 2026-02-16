package com.gabedev.mangako.ui.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
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
    modifier: Modifier = Modifier
) {
    val viewModel: MangaCollectionViewModel = viewModel(
        factory = MangaCollectionViewModelFactory(repository)
    )

    val mangaCollection by viewModel.mangaCollection
    val isLoading by viewModel.isLoading

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observa retorno à tela
    LaunchedEffect(lifecycleOwner) {
        viewModel.loadLibrary()
    }

    Surface(modifier = modifier) {
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
                    Text(text = stringResource(R.string.welcome_message))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = modifier
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
