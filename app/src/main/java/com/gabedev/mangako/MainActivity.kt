package com.gabedev.mangako

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.data.api.MangaDexAPI
import com.gabedev.mangako.data.dto.MangaDto
import com.gabedev.mangako.ui.components.MangaCard
import com.gabedev.mangako.ui.theme.MangaKōTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.mangadex.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(MangaDexAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangaKōTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(5) { index ->
                            if (index % 2 == 0) {
                                MangaCard(
                                    title = "Example Manga $index",
                                    coverUrl = "https://uploads.mangadex.org/covers/d65c0332-3764-4c89-84bd-b1a4e7278ad7/8e8a3e18-948d-402a-a9ea-f62366486771.jpg",
                                    owned = index % 3 == 0,
                                )
                            } else {
                                // Placeholder for an empty card or different content
                                MangaCard(
                                    title = "Placeholder Manga",
                                    coverUrl = "https://uploads.mangadex.org/covers/d65c0332-3764-4c89-84bd-b1a4e7278ad7/8e8a3e18-948d-402a-a9ea-f62366486771.jpg",
                                    owned = false,
                                )
                            }
                        }
                    }
                    MangaSearchScreen(api = api)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MangaKōTheme {
        Greeting("Android")
        MangaCard(
            title = "Example Manga",
            coverUrl = "https://uploads.mangadex.org/covers/d65c0332-3764-4c89-84bd-b1a4e7278ad7/8e8a3e18-948d-402a-a9ea-f62366486771.jpg",
            owned = true,
        )
    }
}

@Composable
fun MangaSearchScreen(api: MangaDexAPI) {
    val mangas by produceState<List<MangaDto>>(initialValue = emptyList(), key1 = api) {
        value = api.searchMangas(title = "one piece").data
    }

    LazyColumn {
        items(mangas) { manga ->
            val title = manga.attributes.title["en"] ?: "No title"
            Text(text = title)
        }
    }
}
