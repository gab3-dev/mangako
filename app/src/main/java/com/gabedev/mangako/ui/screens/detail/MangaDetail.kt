package com.gabedev.mangako.ui.screens.detail

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.gabedev.mangako.R
import com.gabedev.mangako.core.Utils
import com.gabedev.mangako.data.local.getConfigText
import com.gabedev.mangako.data.local.saveConfigText
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.repository.LibraryRepository
import com.gabedev.mangako.data.repository.MangaDexRepository
import com.gabedev.mangako.ui.components.ConfirmDialog
import com.gabedev.mangako.ui.components.CustomLoadingIndicator
import com.gabedev.mangako.ui.components.ListGridSwitch
import com.gabedev.mangako.ui.components.MangaCard
import com.gabedev.mangako.ui.components.MangaCoverImage
import com.gabedev.mangako.ui.components.MangaListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterialApi::class)
@Composable
fun MangaDetail(
    manga: Manga,
    apiRepository: MangaDexRepository,
    localRepository: LibraryRepository,
    backStackEntry: NavBackStackEntry,
    modifier: Modifier = Modifier
) {
    var specialCoverFilter by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModeFlow = remember { context.getConfigText() }
    val viewMode by viewModeFlow.collectAsState(initial = "list")
    val viewModel: MangaDetailViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = MangaDetailViewModelFactory(apiRepository, localRepository, manga)
    )
    val mangaState by viewModel.mangaState.collectAsState()
    val volumeList by viewModel.volumeList.collectAsState()
    val filteredVolumeList by remember(specialCoverFilter, volumeList) {
        mutableStateOf(
            if (specialCoverFilter) {
                volumeList
            } else {
                volumeList.filter { !it.isSpecialEdition }
            }
        )
    }
    val isCoverLoading by viewModel.isVolumeLoading.collectAsState()
    val canLoadMore by viewModel.noMoreVolume.collectAsState()
    val addResult by viewModel.addResult.collectAsState()
    val removeResult by viewModel.removeResult.collectAsState()
    val isMangaInLibrary by viewModel.isMangaInLibrary.collectAsState()
    val listState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var callBackFunction by remember { mutableStateOf({}) }
    var showDialogMangaInLibrary by remember { mutableStateOf(false) }
    val isMultiSelectActive by viewModel.isMultiSelectActive
    if (showDialogMangaInLibrary) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_manga_not_in_library_title),
            text = stringResource(R.string.dialog_manga_not_in_library_text),
            onConfirm = {
                viewModel.addMangaToLibrary(manga)
                callBackFunction()
            },
            onDismiss = {
                showDialogMangaInLibrary = false
            },
        )
    }
    var showDialogMangaNotInLibrary by remember { mutableStateOf(false) }
    if (showDialogMangaNotInLibrary) {
        ConfirmDialog(
            onConfirm = {
                viewModel.removeMangaFromLibrary()
                // Atualiza lista de volumes após remoção
                viewModel.refreshManga()
                showDialogMangaNotInLibrary = false
            },
            onDismiss = {
                showDialogMangaNotInLibrary = false
            }
        )
    }
    val state = rememberPullToRefreshState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == mangaState.volumeCount || canLoadMore) {
                return@derivedStateOf false // já carregou todos os volumes
            }
            try {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= totalItems - 3 // quando estiver perto do fim
            } catch (e: Exception) {
                e.printStackTrace()
                false // caso ocorra algum erro, não carregar mais
            }
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreVolumes()
        }
    }

    LaunchedEffect(addResult) {
        addResult?.let {
            if (it.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_manga_added),
                    duration = SnackbarDuration.Short,
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_manga_add_error),
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.clearAddResult()
        }
    }

    LaunchedEffect(removeResult) {
        removeResult?.let {
            if (it.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_manga_removed),
                    duration = SnackbarDuration.Short,
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.snackbar_manga_remove_error),
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.clearRemoveResult()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        }
    ) { contentPadding ->

        PullToRefreshBox(
            isRefreshing = isCoverLoading,
            onRefresh = {
                viewModel.refreshManga()
            },
            state = state,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    isRefreshing = isCoverLoading,
                    containerColor = LoadingIndicatorDefaults.containedContainerColor,
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter),
                )
            },
            modifier = Modifier.padding(contentPadding)
        ) {
            if (isMultiSelectActive) {
                HorizontalFloatingToolbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = -ScreenOffset)
                        .zIndex(1f),
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    expanded = true
                ) {
                    IconButton(
                        onClick = {
                            viewModel.markSelectedListAsOwned(true)
                            viewModel.finishMultiSelect()
                        },
                        enabled = viewModel.selectedIds.value.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.cd_mark_as_owned),
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.markSelectedListAsOwned(false)
                            viewModel.finishMultiSelect()
                        },
                        enabled = viewModel.selectedIds.value.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = stringResource(R.string.cd_unmark_as_owned),
                        )
                    }
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
                    IconButton(
                        onClick = {
                            viewModel.selectAllVolumes()
                        },
                        enabled = viewModel.selectedIds.value.size < volumeList.size && isMultiSelectActive,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.cd_select_all),
                        )
                    }
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

            LazyVerticalGrid(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp),
                state = listState,
                columns = GridCells.Fixed(if (viewMode == "grid") 3 else 1),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(
                    key = mangaState.id,
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    MangaHeader(
                        title = mangaState.title,
                        enTitle = mangaState.altTitle,
                        author = mangaState.author,
                        description = mangaState.description,
                        situation = mangaState.status,
                        coverUrl = mangaState.coverUrl,
                        volume = mangaState.volumeCount
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (isMangaInLibrary) {
                        OutlinedButton(
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                showDialogMangaNotInLibrary = true
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.button_remove_from_collection),
                            )
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = stringResource(R.string.button_remove_from_collection),
                                fontSize = 18.sp,
                            )
                        }
                    } else {
                        Button(
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                viewModel.addMangaToLibrary(
                                    mangaState
                                )
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.button_add_to_collection)
                            )
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = stringResource(R.string.button_add_to_collection),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterChip(
                        onClick = { specialCoverFilter = !specialCoverFilter },
                        label = {
                            Text(stringResource(R.string.label_special_editions))
                        },
                        selected = specialCoverFilter,
                        leadingIcon = if (specialCoverFilter) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = stringResource(R.string.cd_done_icon),
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.label_volumes)
                        )
                        Box {
                            ListGridSwitch(
                                initialMode = viewMode.ifEmpty { "list" },
                                onChange = { selectedMode ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        context.saveConfigText(selectedMode)
                                    }
                                }
                            )
                        }
                    }
                }
                if (volumeList.isEmpty() && !isCoverLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.label_no_volumes_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    return@LazyVerticalGrid
                } else {
                    items(items = filteredVolumeList, key = { it.id }) { volume ->
                        if (viewMode == "grid") {
                            MangaCard(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (!isMultiSelectActive) {
                                                if (!isMangaInLibrary) {
                                                    callBackFunction = {
                                                        viewModel.toggleVolumeOwned(volume)
                                                        showDialogMangaInLibrary = false
                                                    }
                                                    showDialogMangaInLibrary = true
                                                    return@combinedClickable
                                                }
                                                viewModel.toggleVolumeOwned(volume)
                                            } else {
                                                viewModel.toggleSelection(volume.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectActive) {
                                                viewModel.toggleSelection(volume.id)
                                            }
                                        }
                                    ),
                                title = mangaState.title,
                                coverUrl = volume.coverUrl,
                                owned = volume.owned,
                                isVolumeCard = true,
                                selected = viewModel.selectedIds.value.contains(volume.id),
                                volume = volume.volume,
                            )
                        } else {
                            MangaListItem(
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (!isMultiSelectActive) {
                                                if (!isMangaInLibrary) {
                                                    callBackFunction = {
                                                        viewModel.toggleVolumeOwned(volume)
                                                        showDialogMangaInLibrary = false
                                                    }
                                                    showDialogMangaInLibrary = true
                                                    return@combinedClickable
                                                }
                                                viewModel.toggleVolumeOwned(volume)
                                            } else {
                                                viewModel.toggleSelection(volume.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectActive) {
                                                viewModel.toggleSelection(volume.id)
                                            }
                                        }
                                    ),
                                coverUrl = volume.coverUrl,
                                title = stringResource(R.string.label_volume_format, Utils.handleFloatVolume(volume.volume)),
                                selected = viewModel.selectedIds.value.contains(volume.id),
                                owned = volume.owned,
                            )
                        }
                    }
                }
                if (isCoverLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomLoadingIndicator(
                                Modifier
                                    .width(50.dp)
                                    .height(50.dp)
                            )
                            Text(
                                text = stringResource(R.string.label_loading_covers),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaHeader(
    modifier: Modifier = Modifier,
    title: String,
    enTitle: String? = null,
    author: String? = null,
    description: String,
    situation: String? = null,
    coverUrl: String? = null,
    volume: Int? = null
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (coverUrl != null) {
                MangaCoverImage(
                    imageUrl = coverUrl,
                    modifier = Modifier.height(190.dp),
                    contentDescription = title
                )
            } else {
                MangaCoverImage(
                    imageUrl = null,
                    modifier = Modifier.height(190.dp),
                    contentDescription = title
                )
            }
            Column {
                Column {
                    SimpleText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (enTitle != null) {
                        SimpleText(
                            text = enTitle,
                        )
                    }
                }
                Column {
                    SimpleText(
                        text = if (author != null) stringResource(R.string.header_author, author) else stringResource(R.string.header_author_unknown),
                    )
                    SimpleText(
                        text = if (situation != null) stringResource(R.string.header_status, situation) else stringResource(R.string.header_status_unknown),
                    )
                    SimpleText(
                        text = if (volume != null) stringResource(R.string.header_volumes, volume.toString()) else stringResource(R.string.header_volumes_na),
                    )
                }
            }
        }
        SimpleText(
            text = description,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun SimpleText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(end = 16.dp, bottom = 4.dp)
    )
}
