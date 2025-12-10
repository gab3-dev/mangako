package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.data.model.Manga

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaSearchItem(
    manga: Manga,
    navigate: () -> Unit,
) {
    val title = manga.title
    val isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f),
        onClick = {
            navigate()
        }
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.23f)
                    .heightIn(128.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(192.dp)
                            .padding(64.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    MangaCoverImage(
                        imageUrl = manga.coverUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxHeight(),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 20.dp)
                    .height(IntrinsicSize.Min),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = manga.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                if (manga.type != null) {
                    Surface (
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(4.dp),
                            text = manga.type.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
