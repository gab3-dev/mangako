package com.gabedev.mangako.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.data.local.AppTheme
import com.gabedev.mangako.data.local.AppearancePreferences
import com.gabedev.mangako.data.local.ThemeMode
import com.gabedev.mangako.data.local.getAppearancePreferences
import com.gabedev.mangako.data.local.saveAppTheme
import com.gabedev.mangako.data.local.saveThemeMode
import com.gabedev.mangako.ui.theme.LocalAppDarkTheme
import com.gabedev.mangako.ui.theme.MangaKōTheme
import kotlinx.coroutines.launch

@Composable
fun AppearanceSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferencesFlow = remember(context) { context.getAppearancePreferences() }
    val preferences by preferencesFlow.collectAsState(initial = AppearancePreferences())
    val scope = rememberCoroutineScope()
    val darkTheme = LocalAppDarkTheme.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.appearance_settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.appearance_settings_description),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.selectableGroup()) {
            AppTheme.entries.forEach { theme ->
                val dynamic = theme == AppTheme.DYNAMIC
                AppearanceOption(
                    title = stringResource(
                        if (dynamic) R.string.appearance_theme_dynamic
                        else R.string.appearance_theme_mangako
                    ),
                    selected = preferences.theme == theme,
                    enabled = !dynamic || dynamicAvailable,
                    onClick = { scope.launch { context.saveAppTheme(theme) } },
                ) {
                    Text(
                        text = stringResource(
                            when {
                                !dynamic -> R.string.appearance_theme_mangako_description
                                dynamicAvailable -> R.string.appearance_theme_dynamic_description
                                else -> R.string.appearance_theme_dynamic_unavailable
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MangaKōTheme(darkTheme = darkTheme, dynamicColor = dynamic) {
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                            ).forEach { color ->
                                Box(Modifier.size(24.dp).background(color, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.appearance_mode_title),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Column(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                AppearanceOption(
                    title = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.appearance_mode_system
                            ThemeMode.LIGHT -> R.string.appearance_mode_light
                            ThemeMode.DARK -> R.string.appearance_mode_dark
                        }
                    ),
                    selected = preferences.mode == mode,
                    onClick = { scope.launch { context.saveThemeMode(mode) } },
                )
            }
        }
    }
}

@Composable
private fun AppearanceOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
                .alpha(if (enabled) 1f else 0.38f),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
