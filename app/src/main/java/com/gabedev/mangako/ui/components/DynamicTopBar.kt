package com.gabedev.mangako.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabedev.mangako.R
import com.gabedev.mangako.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar (
    modifier: Modifier = Modifier,
    currentScreen: Screen,
    searchQuery: String,
    searchMode: Boolean,
    topBarVisible: Boolean = true,
    onBackIconClick: () -> Unit,
    onSearchIconClick: () -> Unit,
    onQueryChange: (String) -> Unit
) {
    val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    AnimatedVisibility(
        visible = topBarVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        if (searchMode) {
            CustomSearchBar(
                searchQuery = searchQuery,
                onSearchIconClick = onSearchIconClick,
                onBackIconClick = onBackIconClick,
                onQueryChange = onQueryChange,
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            )
        } else {
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
                        imageVector = currentScreen.icon, contentDescription = stringResource(currentScreen.titleRes)
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSearchIconClick,
                    ) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                    }
                },
            )
        }
    }
}