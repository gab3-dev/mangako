package com.gabedev.mangako.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.gabedev.mangako.R

@Composable
fun CustomSearchBar(
    searchQuery: String,
    modifier: Modifier = Modifier,
    onSearchIconClick: () -> Unit = {},
    onBackIconClick: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.search_placeholder_short)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        textStyle = TextStyle.Default.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true,
        maxLines = 1,
        trailingIcon = {
            IconButton(
                onClick = { onSearchIconClick() },
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search_icon),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        leadingIcon = {
            IconButton(
                onClick = { onBackIconClick() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}


