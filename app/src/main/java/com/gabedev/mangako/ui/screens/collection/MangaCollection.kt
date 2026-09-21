package com.gabedev.mangako.ui.screens.collection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabedev.mangako.R
import com.gabedev.mangako.data.local.getCollectionDensity
import com.gabedev.mangako.data.local.CollectionViewMode
import com.gabedev.mangako.data.local.getCollectionViewMode
import com.gabedev.mangako.data.local.saveCollectionDensity
import com.gabedev.mangako.data.local.saveCollectionViewMode
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.model.toManga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.ui.components.MangaCard
import com.gabedev.mangako.ui.TestTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MangaCollection(
    repository: LibraryRepository,
    onMangaClick: (Manga) -> Unit,
    startupSyncRefreshVersion: Int = 0,
    openSearchRequest: Int = 0,
    onOpenSearchRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    onExploreSearch: (String) -> Unit = {},
    contentBottomPadding: Dp = 0.dp,
    onVolumeSelectionCountChange: (Int) -> Unit = {},
) {
    val viewModel: MangaCollectionViewModel = viewModel(
        factory = MangaCollectionViewModelFactory(repository)
    )

    val mangaCollection by viewModel.mangaCollection
    val isLoading by viewModel.isLoading
    val showIncompleteOnly by viewModel.showIncompleteOnly
    val showSpecialEditionsOnly by viewModel.showSpecialEditionsOnly
    val showUnownedVolumesOnly by viewModel.showUnownedVolumesOnly
    val isVolumeMultiSelectActive by viewModel.isVolumeMultiSelectActive
    val sortOption by viewModel.sortOption

    val context = LocalContext.current
    val collectionViewMode by remember(context) { context.getCollectionViewMode() }
        .collectAsState(initial = CollectionViewMode.MANGA)
    val savedGridColumns by remember(collectionViewMode) {
        context.getCollectionDensity(collectionViewMode)
    }.collectAsState(initial = 2)
    val volumeGroups by viewModel.volumeGroups
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationBarBottomPadding = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
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
    var collapsedVolumeGroupIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var tabsDragDistance by remember { mutableFloatStateOf(0f) }
    val animatedImeBottomPadding by animateDpAsState(
        targetValue = imeBottomPadding,
        label = "collectionEmptyStateImePadding",
    )

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

    LaunchedEffect(openSearchRequest) {
        if (openSearchRequest > 0) {
            openCollectionSearch()
            onOpenSearchRequestHandled()
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

    LaunchedEffect(savedGridColumns) {
        gridColumns = savedGridColumns.coerceIn(1, 5)
    }

    LaunchedEffect(collectionViewMode) {
        viewModel.finishVolumeMultiSelect()
    }

    LaunchedEffect(viewModel.selectedVolumeIds.value.size) {
        onVolumeSelectionCountChange(viewModel.selectedVolumeIds.value.size)
    }

    DisposableEffect(Unit) {
        onDispose { onVolumeSelectionCountChange(0) }
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
                        .testTag(TestTags.CollectionSearch)
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
                        if (collectionViewMode == CollectionViewMode.MANGA) {
                            FilterChip(
                                selected = showIncompleteOnly,
                                onClick = { viewModel.toggleIncompleteFilter() },
                                label = { Text(stringResource(R.string.filter_incomplete)) }
                            )
                        }
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
                            val newDensity = value.roundToInt().coerceIn(1, 5)
                            gridColumns = newDensity
                            coroutineScope.launch {
                                context.saveCollectionDensity(collectionViewMode, newDensity)
                            }
                        },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadLibrary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(startupSyncRefreshVersion) {
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

                PrimaryTabRow(
                    selectedTabIndex = collectionViewMode.ordinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(collectionViewMode) {
                            detectHorizontalDragGestures(
                                onDragStart = { tabsDragDistance = 0f },
                                onHorizontalDrag = { _, dragAmount -> tabsDragDistance += dragAmount },
                                onDragEnd = {
                                    if (abs(tabsDragDistance) >= 48f) {
                                        val mode = if (tabsDragDistance < 0f) {
                                            CollectionViewMode.VOLUMES
                                        } else {
                                            CollectionViewMode.MANGA
                                        }
                                        if (mode != collectionViewMode) {
                                            coroutineScope.launch { context.saveCollectionViewMode(mode) }
                                        }
                                    }
                                },
                            )
                        },
                ) {
                    CollectionViewMode.entries.forEach { mode ->
                        Tab(
                            selected = collectionViewMode == mode,
                            onClick = {
                                coroutineScope.launch { context.saveCollectionViewMode(mode) }
                            },
                            text = {
                                Text(
                                    stringResource(
                                        if (mode == CollectionViewMode.MANGA) {
                                            R.string.nav_library
                                        } else {
                                            R.string.label_volumes
                                        }
                                    )
                                )
                            },
                        )
                    }
                }

                if (collectionViewMode == CollectionViewMode.VOLUMES) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = showUnownedVolumesOnly,
                            onClick = { viewModel.toggleUnownedVolumesFilter() },
                            label = { Text(stringResource(R.string.label_not_owned)) },
                        )
                        IconButton(onClick = { filterSheetOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.cd_filter_options),
                            )
                        }
                    }
                } else {
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
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val isVolumeView = collectionViewMode == CollectionViewMode.VOLUMES
                    val isEmpty = if (isVolumeView) volumeGroups.isEmpty() else mangaCollection.isEmpty()
                    if (isEmpty) {
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
                                    text = if (
                                        searchQuery.isNotBlank() || showIncompleteOnly ||
                                        showSpecialEditionsOnly || showUnownedVolumesOnly
                                    ) {
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
                                            text = stringResource(
                                                R.string.search_on_explore,
                                                searchQuery.trim()
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isVolumeView) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns),
                                    state = gridState,
                                    contentPadding = PaddingValues(bottom = contentBottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .nestedScroll(searchRevealNestedScrollConnection)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    volumeGroups.forEach { group ->
                                        val selectedVolumeCount = group.volumes.count {
                                            it.id in viewModel.selectedVolumeIds.value
                                        }
                                        item(
                                            key = "volume-group-${group.manga.id}",
                                            span = { GridItemSpan(maxLineSpan) },
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        collapsedVolumeGroupIds = if (
                                                            group.manga.id in collapsedVolumeGroupIds
                                                        ) {
                                                            collapsedVolumeGroupIds - group.manga.id
                                                        } else {
                                                            collapsedVolumeGroupIds + group.manga.id
                                                        }
                                                    }
                                                    .padding(top = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = group.manga.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (selectedVolumeCount > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    ) {
                                                        Text(
                                                            text = selectedVolumeCount.toString(),
                                                            modifier = Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 2.dp,
                                                            ),
                                                            style = MaterialTheme.typography.labelMedium,
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = if (
                                                        group.manga.id in collapsedVolumeGroupIds
                                                    ) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = group.manga.title,
                                                )
                                            }
                                        }
                                        if (group.manga.id !in collapsedVolumeGroupIds) {
                                            group.volumes.forEach { volume ->
                                            item(key = volume.id) {
                                                MangaCard(
                                                    modifier = Modifier
                                                        .testTag(TestTags.volumeCard(volume.id))
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (isVolumeMultiSelectActive) {
                                                                    viewModel.toggleVolumeSelection(volume.id)
                                                                } else {
                                                                    viewModel.toggleVolumeOwned(volume)
                                                                }
                                                            },
                                                            onLongClick = {
                                                                if (!isVolumeMultiSelectActive) {
                                                                    viewModel.toggleVolumeSelection(volume.id)
                                                                }
                                                            },
                                                        ),
                                                    title = group.manga.title,
                                                    coverUrl = volume.coverUrl,
                                                    owned = volume.owned,
                                                    selected = viewModel.selectedVolumeIds.value.contains(volume.id),
                                                    isVolumeCard = true,
                                                    volume = volume.volume,
                                                    volumeLocale = volume.locale,
                                                    isSpecialEdition = volume.isSpecialEdition,
                                                )
                                            }
                                        }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = navigationBarBottomPadding + 12.dp)
                                        .zIndex(1f),
                                ) {
                                    AnimatedVisibility(
                                        visible = isVolumeMultiSelectActive,
                                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                                    ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        tonalElevation = 6.dp,
                                        shadowElevation = 8.dp,
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            ToolbarTooltip(label = stringResource(R.string.cd_mark_as_owned)) {
                                                IconButton(onClick = { viewModel.markSelectedVolumesAsOwned(true) }) {
                                                    Icon(Icons.Default.Check, stringResource(R.string.cd_mark_as_owned))
                                                }
                                            }
                                            ToolbarTooltip(label = stringResource(R.string.cd_unmark_as_owned)) {
                                                IconButton(onClick = { viewModel.markSelectedVolumesAsOwned(false) }) {
                                                    Icon(Icons.Default.Close, stringResource(R.string.cd_unmark_as_owned))
                                                }
                                            }
                                            ToolbarTooltip(label = stringResource(R.string.cd_select_all)) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.selectAllVolumes(
                                                            volumeGroups.flatMap { it.volumes }.map { it.id }.toSet(),
                                                        )
                                                    },
                                                    enabled = viewModel.selectedVolumeIds.value.size <
                                                        volumeGroups.sumOf { it.volumes.size },
                                                ) {
                                                    Icon(Icons.Default.SelectAll, stringResource(R.string.cd_select_all))
                                                }
                                            }
                                            ToolbarTooltip(label = stringResource(R.string.cd_deselect)) {
                                                IconButton(
                                                    onClick = { viewModel.clearVolumeSelection() },
                                                    enabled = viewModel.selectedVolumeIds.value.isNotEmpty(),
                                                ) {
                                                    Icon(Icons.Default.Deselect, stringResource(R.string.cd_deselect))
                                                }
                                            }
                                            ToolbarTooltip(label = stringResource(R.string.cd_stop_multi_select)) {
                                                FilledIconButton(onClick = { viewModel.finishVolumeMultiSelect() }) {
                                                    Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.cd_stop_multi_select))
                                                }
                                            }
                                        }
                                    }
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns),
                                    state = gridState,
                                    contentPadding = PaddingValues(bottom = contentBottomPadding),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .nestedScroll(searchRevealNestedScrollConnection)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    items(mangaCollection.size) { index ->
                                        val manga = mangaCollection[index]
                                        MangaCard(
                                            modifier = Modifier
                                                .testTag(TestTags.mangaCard(manga.id))
                                                .combinedClickable(
                                                    onClick = { onMangaClick(manga.toManga()) },
                                                ),
                                            title = manga.title,
                                            coverUrl = manga.coverUrl,
                                            volumeTotal = manga.volumeCount,
                                            volumesOwned = manga.volumeOwned,
                                            selected = false,
                                        )
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
                        .matchParentSize()
                        .zIndex(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { closeCollectionSearch(clearQuery = false) },
                        )
                )

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
