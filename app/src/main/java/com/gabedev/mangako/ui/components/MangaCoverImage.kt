package com.gabedev.mangako.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gabedev.mangako.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaCoverImage(
    imageUrl: String?, // Pode ser nulo se não tiver cover
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f) // Cover usually is 2:3
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl == null) {
            Text(
                text = "Sem cover",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = {
                    isLoading = false
                },
                error = painterResource(R.drawable.ic_broken_image)
            )

            // Adiciona um loading se quiser (opcional)
            if (isLoading) {
                CircularWavyProgressIndicator(
                    Modifier
                        .width(30.dp)
                        .height(30.dp),
                    gapSize = 8.dp,
                )
            }
        }
    }
}