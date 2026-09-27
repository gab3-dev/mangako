package com.gabedev.mangako.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R

@Composable
fun CreditsSettingsSection(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(modifier) {
        Text(stringResource(R.string.credits_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.credits_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CreditItem(
            title = stringResource(R.string.credits_development_title),
            description = stringResource(R.string.credits_development_description),
            url = "https://github.com/gab3-dev",
            onOpenUrl = uriHandler::openUri,
        )
        CreditItem(
            title = stringResource(R.string.credits_design_title),
            description = stringResource(R.string.credits_design_description),
            url = "https://www.linkedin.com/in/hevertton/",
            onOpenUrl = uriHandler::openUri,
        )
        CreditItem(
            title = stringResource(R.string.credits_mangadex_title),
            description = stringResource(R.string.credits_mangadex_description),
            url = "https://mangadex.org/",
            onOpenUrl = uriHandler::openUri,
        )
        CreditItem(
            title = stringResource(R.string.credits_android_title),
            description = stringResource(R.string.credits_android_description),
            url = "https://www.apache.org/licenses/LICENSE-2.0",
            onOpenUrl = uriHandler::openUri,
        )
        CreditItem(
            title = stringResource(R.string.credits_coil_title),
            description = stringResource(R.string.credits_coil_description),
            url = "https://github.com/coil-kt/coil",
            onOpenUrl = uriHandler::openUri,
        )
        CreditItem(
            title = stringResource(R.string.credits_square_title),
            description = stringResource(R.string.credits_square_description),
            url = "https://square.github.io/",
            onOpenUrl = uriHandler::openUri,
        )
    }
}

@Composable
private fun CreditItem(
    title: String,
    description: String,
    url: String,
    onOpenUrl: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onOpenUrl(url) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
