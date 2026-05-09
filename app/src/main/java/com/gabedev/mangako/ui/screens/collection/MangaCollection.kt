package com.gabedev.mangako.ui.screens.collection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.R
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.ConfirmDialog
import com.gabedev.mangako.ui.components.MangaCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaCollection(
    repository: LibraryRepository,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
    onExploreSearch: (String) -> Unit = {},
) {
    val viewModel: MangaCollectionViewModel = viewModel(
        factory = MangaCollectionViewModelFactory(repository)
    )

    val mangaCollection by viewModel.mangaCollection
    val isLoading by viewModel.isLoading
    val showIncompleteOnly by viewModel.showIncompleteOnly
    val showSpecialEditionsOnly by viewModel.showSpecialEditionsOnly
    val isMultiSelectActive by viewModel.isMultiSelectActive
    val sortOption by viewModel.sortOption

    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var searchSnapJob by remember { mutableStateOf<Job?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var collectionSearchHeightPx by remember { mutableFloatStateOf(0f) }
    var collectionSearchMaxHeightPx by remember { mutableFloatStateOf(0f) }
    var collectionSearchBarOpen by remember { mutableStateOf(false) }
    var collectionSearchDockedAboveKeyboard by remember { mutableStateOf(false) }
    var collectionSearchFocusRequest by remember { mutableIntStateOf(0) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterSheetOpen by remember { mutableStateOf(false) }
    var gridColumns by remember { mutableIntStateOf(2) }
    val animatedImeBottomPadding by animateDpAsState(
        targetValue = imeBottomPadding,
        label = "collectionEmptyStateImePadding",
    )

    var showRemoveDialog by remember { mutableStateOf(false) }
    val searchCommitThresholdFraction = 0.55f
    val fallbackSearchHeightPx = with(density) { 76.dp.toPx() }
    val targetCollectionSearchHeightPx = collectionSearchMaxHeightPx.takeIf { it > 0f }
        ?: fallbackSearchHeightPx
    val collectionSearchHeightFraction = if (targetCollectionSearchHeightPx > 0f) {
        (collectionSearchHeightPx / targetCollectionSearchHeightPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shouldDockSearchAboveKeyboard = collectionSearchDockedAboveKeyboard &&
        collectionSearchBarOpen
    val focusedSearchBottomPadding = if (imeBottomPadding > 0.dp) {
        imeBottomPadding + 32.dp
    } else {
        32.dp
    }

    fun closeCollectionSearch(clearQuery: Boolean) {
        if (clearQuery) {
            searchQuery = ""
            debouncedSearchQuery = ""
        }
        focusManager.clearFocus()
        keyboardController?.hide()
        collectionSearchDockedAboveKeyboard = false
        searchSnapJob?.cancel()
        searchSnapJob = coroutineScope.launch {
            animate(
                initialValue = collectionSearchHeightPx,
                targetValue = 0f,
            ) { value, _ ->
                collectionSearchHeightPx = value
            }
            collectionSearchBarOpen = false
        }
    }

    fun openCollectionSearch() {
        searchSnapJob?.cancel()
        searchSnapJob = coroutineScope.launch {
            animate(
                initialValue = collectionSearchHeightPx,
                targetValue = targetCollectionSearchHeightPx,
            ) { value, _ ->
                collectionSearchHeightPx = value
            }
            collectionSearchBarOpen = true
            collectionSearchDockedAboveKeyboard = true
            collectionSearchFocusRequest++
        }
    }

    suspend fun snapCollectionSearchAfterPull() {
        if (targetCollectionSearchHeightPx <= 0f) {
            return
        }

        val isRevealed = collectionSearchHeightPx > 0f
        val isNotSettled = collectionSearchHeightPx != 0f &&
            collectionSearchHeightPx != targetCollectionSearchHeightPx

        if (isRevealed && isNotSettled) {
            val shouldOpen = collectionSearchHeightPx >=
                targetCollectionSearchHeightPx * searchCommitThresholdFraction
            val targetHeight = if (shouldOpen) targetCollectionSearchHeightPx else 0f

            animate(
                initialValue = collectionSearchHeightPx,
                targetValue = targetHeight,
            ) { value, _ ->
                collectionSearchHeightPx = value
            }
            collectionSearchBarOpen = shouldOpen
            if (shouldOpen) {
                collectionSearchDockedAboveKeyboard = true
                collectionSearchFocusRequest++
            }
        } else if (collectionSearchHeightPx == targetCollectionSearchHeightPx && !collectionSearchBarOpen) {
            collectionSearchBarOpen = true
            collectionSearchDockedAboveKeyboard = true
            collectionSearchFocusRequest++
        }
    }

    val searchRevealNestedScrollConnection = remember(
        gridState,
        collectionSearchHeightPx,
        targetCollectionSearchHeightPx,
        collectionSearchBarOpen,
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || targetCollectionSearchHeightPx <= 0f) {
                    return Offset.Zero
                }

                val isGridAtTop = gridState.firstVisibleItemIndex == 0 &&
                    gridState.firstVisibleItemScrollOffset == 0

                if (available.y > 0f && isGridAtTop) {
                    searchSnapJob?.cancel()
                    val nextHeight = (collectionSearchHeightPx + available.y)
                        .coerceIn(0f, targetCollectionSearchHeightPx)
                    val consumed = nextHeight - collectionSearchHeightPx

                    if (consumed > 0f) {
                        collectionSearchBarOpen = false
                        collectionSearchHeightPx = nextHeight
                        return Offset(x = 0f, y = consumed)
                    }
                }

                if (available.y < 0f && collectionSearchBarOpen && collectionSearchHeightPx > 0f) {
                    collectionSearchBarOpen = false
                    closeCollectionSearch(clearQuery = false)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                snapCollectionSearchAfterPull()
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snapCollectionSearchAfterPull()
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(searchQuery) {
        delay(600)
        debouncedSearchQuery = searchQuery.trim()
    }

    LaunchedEffect(debouncedSearchQuery) {
        viewModel.setSearchQuery(debouncedSearchQuery)
    }

    LaunchedEffect(collectionSearchFocusRequest) {
        if (collectionSearchFocusRequest > 0 && collectionSearchBarOpen) {
            collectionSearchDockedAboveKeyboard = true
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(shouldDockSearchAboveKeyboard) {
        if (shouldDockSearchAboveKeyboard) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(collectionSearchBarOpen, collectionSearchHeightPx) {
        if (!collectionSearchBarOpen && collectionSearchHeightPx == 0f) {
            collectionSearchDockedAboveKeyboard = false
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = collectionSearchBarOpen || collectionSearchDockedAboveKeyboard) {
        closeCollectionSearch(clearQuery = false)
    }

    @Composable
    fun CollectionSearchBar(
        searchModifier: Modifier,
        measureHeight: Boolean,
    ) {
        SearchBar(
            modifier = searchModifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .then(
                    if (measureHeight) {
                        Modifier.onSizeChanged { size ->
                            if (size.height > 0 && size.height.toFloat() != collectionSearchMaxHeightPx) {
                                collectionSearchMaxHeightPx = size.height.toFloat()
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        debouncedSearchQuery = searchQuery.trim()
                        closeCollectionSearch(clearQuery = false)
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                searchSnapJob?.cancel()
                                collectionSearchHeightPx = targetCollectionSearchHeightPx
                                collectionSearchBarOpen = true
                                collectionSearchDockedAboveKeyboard = true
                            }
                    },
                    leadingIcon = {
                        IconButton(onClick = { closeCollectionSearch(clearQuery = false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { closeCollectionSearch(clearQuery = true) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_close_search),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_collection_placeholder)) }
                )
            },
            expanded = false,
            onExpandedChange = {},
        ) {}
    }

    @Composable
    fun sortOptionLabel(option: MangaCollectionSortOption): String {
        return when (option) {
            MangaCollectionSortOption.TITLE_ASC -> stringResource(R.string.sort_title_asc)
            MangaCollectionSortOption.TITLE_DESC -> stringResource(R.string.sort_title_desc)
            MangaCollectionSortOption.PROGRESS_DESC -> stringResource(R.string.sort_progress_desc)
            MangaCollectionSortOption.PROGRESS_ASC -> stringResource(R.string.sort_progress_asc)
        }
    }

    @Composable
    fun ToolbarTooltip(
        label: String,
        content: @Composable () -> Unit,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above
            ),
            tooltip = {
                PlainTooltip {
                    Text(label)
                }
            },
            state = rememberTooltipState(),
        ) {
            content()
        }
    }

    if (showRemoveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_remove_selected_title),
            text = stringResource(R.string.dialog_remove_selected_text, viewModel.selectedIds.value.size),
            onConfirm = {
                viewModel.removeSelectedFromLibrary()
                showRemoveDialog = false
            },
            onDismiss = {
                showRemoveDialog = false
            },
        )
    }

    if (filterSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterSheetOpen = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.collection_options),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.filters),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showIncompleteOnly,
                            onClick = { viewModel.toggleIncompleteFilter() },
                            label = { Text(stringResource(R.string.filter_incomplete)) }
                        )
                        FilterChip(
                            selected = showSpecialEditionsOnly,
                            onClick = { viewModel.toggleSpecialEditionsFilter() },
                            label = { Text(stringResource(R.string.filter_special_editions)) }
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.density),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.grid_columns_count, gridColumns),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = gridColumns.toFloat(),
                        onValueChange = { value ->
                            gridColumns = value.roundToInt().coerceIn(1, 5)
                        },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        }
    }

    // Observa retorno à tela
    LaunchedEffect(lifecycleOwner) {
        viewModel.loadLibrary()
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (shouldDockSearchAboveKeyboard) 18.dp else 0.dp)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { targetCollectionSearchHeightPx.toDp() })
                    .clipToBounds()
            ) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .alpha(if (shouldDockSearchAboveKeyboard) 0f else 1f - collectionSearchHeightFraction),
                    title = {
                        Text(
                            text = stringResource(R.string.nav_library),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Book,
                            contentDescription = stringResource(R.string.nav_library)
                        )
                    },
                    actions = {
                        IconButton(onClick = { openCollectionSearch() }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { collectionSearchHeightPx.toDp() })
                        .clipToBounds()
                ) {
                    if (!shouldDockSearchAboveKeyboard) {
                        CollectionSearchBar(
                            searchModifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .alpha(collectionSearchHeightFraction),
                            measureHeight = true,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text(stringResource(R.string.sort_by, sortOptionLabel(sortOption)))
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        MangaCollectionSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortOptionLabel(option)) },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = { filterSheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.cd_filter_options)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (mangaCollection.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = animatedImeBottomPadding)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || showIncompleteOnly || showSpecialEditionsOnly) {
                                    stringResource(R.string.no_results_found)
                                } else {
                                    stringResource(R.string.welcome_message)
                                }
                            )
                            if (searchQuery.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        val query = searchQuery.trim()
                                        searchQuery = ""
                                        debouncedSearchQuery = ""
                                        viewModel.clearSearchQuery()
                                        closeCollectionSearch(clearQuery = false)
                                        onExploreSearch(query)
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_on_explore, searchQuery.trim()),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            state = gridState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(searchRevealNestedScrollConnection)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(mangaCollection.size) { index ->
                                val manga = mangaCollection[index]
                                MangaCard(
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (!isMultiSelectActive) {
                                                    onMangaClick(manga.toManga())
                                                } else {
                                                    viewModel.toggleSelection(manga.id)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isMultiSelectActive) {
                                                    viewModel.toggleSelection(manga.id)
                                                }
                                            }
                                        ),
                                    title = manga.title,
                                    coverUrl = manga.coverUrl,
                                    volumeTotal = manga.volumeCount,
                                    volumesOwned = manga.volumeOwned,
                                    selected = viewModel.selectedIds.value.contains(manga.id),
                                )
                            }
                        }

                        if (isMultiSelectActive) {
                            HorizontalFloatingToolbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = -ScreenOffset)
                                    .zIndex(1f),
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                                expanded = true
                            ) {
                                ToolbarTooltip(
                                    label = stringResource(R.string.cd_remove_selected),
                                ) {
                                    IconButton(
                                        onClick = {
                                            showRemoveDialog = true
                                        },
                                        enabled = viewModel.selectedIds.value.isNotEmpty(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cd_remove_selected),
                                        )
                                    }
                                }
                                ToolbarTooltip(
                                    label = stringResource(R.string.cd_stop_multi_select),
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            viewModel.finishMultiSelect()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = stringResource(R.string.cd_stop_multi_select),
                                        )
                                    }
                                }
                                ToolbarTooltip(
                                    label = stringResource(R.string.cd_select_all),
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.selectAll(mangaCollection.map { it.id }.toSet())
                                        },
                                        enabled = viewModel.selectedIds.value.size < mangaCollection.size && isMultiSelectActive,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SelectAll,
                                            contentDescription = stringResource(R.string.cd_select_all),
                                        )
                                    }
                                }
                                ToolbarTooltip(
                                    label = stringResource(R.string.cd_deselect),
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.clearSelection()
                                        },
                                        enabled = viewModel.selectedIds.value.isNotEmpty() && isMultiSelectActive,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Deselect,
                                            contentDescription = stringResource(R.string.cd_deselect),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (shouldDockSearchAboveKeyboard) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = focusedSearchBottomPadding)
                        .zIndex(2f)
                ) {
                    CollectionSearchBar(
                        searchModifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        measureHeight = true,
                    )
                }
            }
        }
    }
}
