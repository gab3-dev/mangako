package com.gabedev.mangako.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListGridSwitch(
    modifier: Modifier = Modifier,
    initialMode: String = "list",
    onChange: (String) -> Unit = {},
) {
    val options = listOf("list", "grid")
    val unCheckedIcons =
        listOf(Icons.AutoMirrored.Filled.List, Icons.Outlined.GridView)
    val checkedIcons = listOf(Icons.AutoMirrored.Filled.List, Icons.Outlined.GridView)
    var selectedIndex = options.indexOf(initialMode)

    Row(
        modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {

        options.forEachIndexed { index, _ ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    selectedIndex = index
                    onChange(options[index])
                },
                modifier = Modifier
                    .semantics { role = Role.RadioButton }
                    .wrapContentWidth(),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,   // cor do botão desmarcado
                    contentColor = MaterialTheme.colorScheme.onSurface,   // cor do ícone desmarcado
                    checkedContainerColor = MaterialTheme.colorScheme.primary,   // cor do botão marcado
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,   // cor do ícone marcado
                ),
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Icon(
                    if (selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                    contentDescription = "Localized description",
                )
            }
        }
    }
}