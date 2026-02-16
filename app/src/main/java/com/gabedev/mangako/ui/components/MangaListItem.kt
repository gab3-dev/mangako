package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R

@Composable
fun MangaListItem(
    modifier: Modifier = Modifier,
    coverUrl: String,
    title: String,
    selected: Boolean,
    owned: Boolean,
) {
    val isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .alpha(if (owned) 0.5f else 1f)
            .fillMaxWidth()
            .fillMaxHeight(0.2f),
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
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
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    MangaCoverImage(
                        imageUrl = coverUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxHeight(),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 20.dp)
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = stringResource(R.string.cd_selected_icon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
