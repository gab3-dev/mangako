package com.gabedev.mangako.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.gabedev.mangako.R

@Composable
fun LanguageSettingsSection(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    var selectedTag by remember(configuration) {
        mutableStateOf(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag().orEmpty())
    }

    Column(modifier) {
        Text(stringResource(R.string.language_settings_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.language_settings_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup()) {
            listOf(
                "" to R.string.language_follow_system,
                "pt-BR" to R.string.language_portuguese,
                "en" to R.string.language_english,
                "ja" to R.string.language_japanese,
            ).forEach { (tag, label) ->
                val selected = selectedTag.substringBefore('-') == tag.substringBefore('-')
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("app-language-${tag.ifEmpty { "system" }}")
                        .selectable(selected = selected, role = Role.RadioButton) {
                            selectedTag = tag
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        stringResource(label),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
