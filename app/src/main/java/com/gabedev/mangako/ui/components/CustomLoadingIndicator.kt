package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CustomLoadingIndicator(
    modifier: Modifier = Modifier
) {
    LoadingIndicator(
        color = LoadingIndicatorDefaults.containedContainerColor,
        modifier = modifier
            .fillMaxWidth(0.3f)
            .fillMaxHeight(0.3f),
    )
}
