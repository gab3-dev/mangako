package com.gabedev.mangako.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

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
        placeholder = { Text("Search...") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        textStyle = TextStyle.Default.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
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
                Icons.Default.Search.let { icon ->
                    androidx.compose.material.Icon(
                        imageVector = icon,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        leadingIcon = {
            IconButton(
                onClick = { onBackIconClick() },
            ) {
                Icons.AutoMirrored.Filled.ArrowBack.let { icon ->
                    androidx.compose.material.Icon(
                        imageVector = icon,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}


