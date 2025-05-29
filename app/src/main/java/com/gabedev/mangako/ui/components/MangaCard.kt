package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gabedev.mangako.ui.theme.Green40

const val MANGA_COVER_PLACEHOLDER = "https://uploads.mangadex.org/covers/d65c0332-3764-4c89-84bd-b1a4e7278ad7/8e8a3e18-948d-402a-a9ea-f62366486771.jpg"
val STATUS_BORDER_RADIUS = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 0.dp,
    bottomStart = 0.dp,
    bottomEnd = 8.dp
)

@Composable
fun MangaCard(
    title: String,
    coverUrl: String,
    owned: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (owned) Green40 else MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(8.dp),
            contentAlignment = androidx.compose.ui.Alignment.BottomEnd,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .width(256.dp)
                        .fillMaxHeight(),
                )
            }

            Card (
                colors = CardDefaults.cardColors(
                    containerColor = if (owned) Green40 else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .height(28.dp),
                shape = STATUS_BORDER_RADIUS,
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (owned) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                            contentDescription = "Owned",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(18.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 14.sp,
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Not Owned",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.height(18.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MangaCardPreview() {
    MangaCard(
        title = "Example Manga",
        coverUrl = MANGA_COVER_PLACEHOLDER,
        owned = true,
    )
}