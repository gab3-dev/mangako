package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CustomLoadingIndicator(
    modifier: Modifier = Modifier
) {
    ContainedLoadingIndicator(
        modifier = modifier
            .fillMaxWidth(0.3f)
            .fillMaxHeight(0.17f),
    )
}
