package com.gabedev.mangako.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import com.gabedev.mangako.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar(
    modifier: Modifier = Modifier,
    topBarVisible: Boolean = true,
    placeholderRes: Int = R.string.search_placeholder,
    onDebouncedQuery: (String) -> Unit,
    expandedContent: @Composable (() -> Unit)? = null
) {
    val textFieldState = rememberTextFieldState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    // Only allow expansion when there is content to show
    val hasExpandedContent = expandedContent != null
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Apply debounce when user types
    LaunchedEffect(searchQuery) {
        delay(600)
        debouncedQuery = searchQuery.trim()
    }

    // Notify parent when debounced query changes
    LaunchedEffect(debouncedQuery) {
        onDebouncedQuery(debouncedQuery)
    }

    AnimatedVisibility(
        visible = topBarVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        SearchBar(
            modifier = modifier
                .fillMaxWidth()
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        searchQuery = textFieldState.text.toString()
                    },
                    onSearch = {
                        debouncedQuery = textFieldState.text.toString().trim()
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it && hasExpandedContent },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search_icon),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    placeholder = { Text(stringResource(placeholderRes)) }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it && hasExpandedContent },
        ) {
            expandedContent?.invoke()
        }
    }
}