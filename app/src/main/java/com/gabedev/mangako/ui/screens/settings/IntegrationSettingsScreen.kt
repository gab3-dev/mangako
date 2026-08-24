package com.gabedev.mangako.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.data.local.CatalogIntegration
import com.gabedev.mangako.data.local.NavigationBarStyle
import com.gabedev.mangako.data.local.getCatalogIntegration
import com.gabedev.mangako.data.local.getNavigationBarStyle
import com.gabedev.mangako.data.local.saveCatalogIntegration
import com.gabedev.mangako.data.local.saveNavigationBarStyle
import kotlinx.coroutines.launch

@Composable
fun IntegrationSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val selectedIntegration by context.getCatalogIntegration()
        .collectAsState(initial = CatalogIntegration.MANGAKO)
    val navigationBarStyle by context.getNavigationBarStyle()
        .collectAsState(initial = NavigationBarStyle.CLASSIC)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.integration_settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.integration_settings_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IntegrationOption(
            title = stringResource(R.string.integration_mangadex),
            description = stringResource(R.string.integration_mangadex_description),
            selected = selectedIntegration == CatalogIntegration.MANGADEX,
            onClick = { scope.launch { context.saveCatalogIntegration(CatalogIntegration.MANGADEX) } },
        )
        IntegrationOption(
            title = stringResource(R.string.integration_mangako),
            description = stringResource(R.string.integration_mangako_description),
            selected = selectedIntegration == CatalogIntegration.MANGAKO,
            onClick = { scope.launch { context.saveCatalogIntegration(CatalogIntegration.MANGAKO) } },
        )
        Text(
            text = stringResource(R.string.integration_fallback_description),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.navigation_settings_title),
            modifier = Modifier.padding(top = 32.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.navigation_settings_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IntegrationOption(
            title = stringResource(R.string.navigation_style_classic),
            description = stringResource(R.string.navigation_style_classic_description),
            selected = navigationBarStyle == NavigationBarStyle.CLASSIC,
            onClick = { scope.launch { context.saveNavigationBarStyle(NavigationBarStyle.CLASSIC) } },
        )
        IntegrationOption(
            title = stringResource(R.string.navigation_style_floating),
            description = stringResource(R.string.navigation_style_floating_description),
            selected = navigationBarStyle == NavigationBarStyle.FLOATING,
            onClick = { scope.launch { context.saveNavigationBarStyle(NavigationBarStyle.FLOATING) } },
        )
    }
}

@Composable
private fun IntegrationOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
