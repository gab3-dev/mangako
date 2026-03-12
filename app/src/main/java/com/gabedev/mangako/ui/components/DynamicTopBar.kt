package com.gabedev.mangako.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import com.gabedev.mangako.R
import com.gabedev.mangako.Screen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar(
    modifier: Modifier = Modifier,
    currentScreen: Screen? = null,
    topBarVisible: Boolean = true,
    alwaysShowSearchBar: Boolean = true,
    placeholderRes: Int = R.string.search_placeholder,
    initialQuery: String = "",
    onDebouncedQuery: (String) -> Unit,
    expandedContent: @Composable (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var debouncedQuery by remember { mutableStateOf(initialQuery) }
    var searchBarVisible by rememberSaveable { mutableStateOf(alwaysShowSearchBar) }
    // Only allow expansion when there is content to show
    val hasExpandedContent = expandedContent != null
    var expanded by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        if (searchBarVisible) {
            SearchBar(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            debouncedQuery = searchQuery.trim()
                            expanded = false
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it && hasExpandedContent },
                        leadingIcon = {
                            if (!alwaysShowSearchBar) {
                                // Show back arrow to return to TopAppBar
                                IconButton(onClick = {
                                    searchQuery = ""
                                    debouncedQuery = ""
                                    searchBarVisible = false
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_search_icon),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    debouncedQuery = ""
                                    if (!alwaysShowSearchBar) {
                                        searchBarVisible = false
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cd_close_search),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        placeholder = { Text(stringResource(placeholderRes)) }
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it && hasExpandedContent },
            ) {
                expandedContent?.invoke()
            }
        } else if (currentScreen != null) {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                title = {
                    Text(
                        text = stringResource(currentScreen.titleRes),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = currentScreen.icon,
                        contentDescription = stringResource(currentScreen.titleRes)
                    )
                },
                actions = {
                    IconButton(onClick = { searchBarVisible = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search)
                        )
                    }
                },
            )
        }
    }
}