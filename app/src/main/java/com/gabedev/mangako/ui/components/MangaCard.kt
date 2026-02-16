package com.gabedev.mangako.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabedev.mangako.R
import com.gabedev.mangako.core.Utils

const val MANGA_COVER_PLACEHOLDER = "https://uploads.mangadex.org/covers/d65c0332-3764-4c89-84bd-b1a4e7278ad7/8e8a3e18-948d-402a-a9ea-f62366486771.jpg"

@Composable
fun MangaCard(
    modifier: Modifier = Modifier,
    title: String,
    coverUrl: String,
    owned: Boolean = false,
    selected: Boolean = false,
    volumesOwned: Int = 0,
    volumeTotal: Int = 0,
    isVolumeCard: Boolean = false,
    volume: Float? = null,
) {
    Card(
        modifier = modifier
            .alpha(if (owned) 0.5f else 1f)
            .fillMaxWidth()
            .padding(if (selected) 4.dp else 0.dp)
            .heightIn(min = 200.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            MangaCoverImage(
                imageUrl = coverUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
            )
            if (!isVolumeCard) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd) // canto inferior direito
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (volumesOwned == volumeTotal) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_completed),
                            tint = Color.White,
                        )
                    } else {
                        Text(
                            text = "$volumesOwned/$volumeTotal",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        if (isVolumeCard) {
            Row {
                Text(
                    text = stringResource(R.string.label_volume_card_format, Utils.handleFloatVolume(volume)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp),
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(8.dp),
            )
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