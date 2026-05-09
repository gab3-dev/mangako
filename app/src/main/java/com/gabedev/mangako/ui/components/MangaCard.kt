package com.gabedev.mangako.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
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
    volumeLocale: String = "ja",
    isSpecialEdition: Boolean = false,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val titleFontSize = when {
            maxWidth < 96.dp -> 10.sp
            maxWidth < 128.dp -> 12.sp
            maxWidth < 160.dp -> 14.sp
            else -> MaterialTheme.typography.bodyLarge.fontSize
        }
        val titleLineHeight = when {
            maxWidth < 96.dp -> 12.sp
            maxWidth < 128.dp -> 14.sp
            maxWidth < 160.dp -> 16.sp
            else -> MaterialTheme.typography.bodyLarge.lineHeight
        }
        val badgeFontSize = when {
            maxWidth < 96.dp -> 9.sp
            maxWidth < 128.dp -> 10.sp
            else -> 12.sp
        }
        val contentPadding = if (maxWidth < 128.dp) 6.dp else 8.dp
        val badgePaddingHorizontal = if (maxWidth < 128.dp) 6.dp else 8.dp
        val badgePaddingVertical = if (maxWidth < 128.dp) 3.dp else 4.dp
        val checkIconSize = when {
            maxWidth < 96.dp -> 14.dp
            maxWidth < 128.dp -> 16.dp
            else -> 18.dp
        }

        Card(
            modifier = Modifier
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
                            .align(Alignment.TopEnd)
                            .padding(contentPadding)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = badgePaddingHorizontal,
                                vertical = badgePaddingVertical
                            )
                    ) {
                        if (volumesOwned == volumeTotal) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.cd_completed),
                                tint = Color.White,
                                modifier = Modifier.size(checkIconSize)
                            )
                        } else {
                            Text(
                                text = "$volumesOwned/$volumeTotal",
                                color = Color.White,
                                fontSize = badgeFontSize,
                                lineHeight = badgeFontSize
                            )
                        }
                    }
                }
            }
            if (isVolumeCard) {
                Row {
                    Text(
                        text = stringResource(
                            R.string.label_volume_card_format,
                            Utils.handleVolumeLabel(volume, volumeLocale, isSpecialEdition)
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = titleFontSize,
                            lineHeight = titleLineHeight
                        ),
                        modifier = Modifier.padding(contentPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = titleFontSize,
                        lineHeight = titleLineHeight
                    ),
                    modifier = Modifier.padding(contentPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
