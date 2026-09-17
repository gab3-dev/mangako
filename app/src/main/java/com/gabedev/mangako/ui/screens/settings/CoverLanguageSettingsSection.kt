package com.gabedev.mangako.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.data.local.CoverLanguage
import com.gabedev.mangako.data.local.getCoverLanguage
import com.gabedev.mangako.data.local.saveCoverLanguage
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CoverLanguageSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val flow = remember(context) { context.getCoverLanguage() }
    val selected by flow.collectAsState(initial = CoverLanguage.JAPANESE)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val displayLocale = LocalConfiguration.current.locales[0]
    val originalLabel = stringResource(R.string.cover_language_original)
    val labels = CoverLanguage.entries.associateWith {
        if (it == CoverLanguage.ORIGINAL) originalLabel
        else Locale.forLanguageTag(it.tag).getDisplayName(displayLocale)
    }

    Column(modifier) {
        Text(stringResource(R.string.cover_language_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.cover_language_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().testTag("cover-language-picker"),
            ) {
                Text(labels.getValue(selected))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CoverLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(labels.getValue(language)) },
                        onClick = {
                            expanded = false
                            scope.launch { context.saveCoverLanguage(language) }
                        },
                        modifier = Modifier.testTag("cover-language-${language.tag}"),
                    )
                }
            }
        }
    }
}
