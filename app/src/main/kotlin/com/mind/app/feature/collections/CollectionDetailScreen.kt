package com.mind.app.feature.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindCompactPathPill
import com.mind.app.core.designsystem.component.MindDeleteSelectedDialog
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindEmptyState
import com.mind.app.core.designsystem.component.MindFloatingPageTopBar
import com.mind.app.core.designsystem.component.MindGlassDialog
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindMemoryCard
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSelectionActionDock
import com.mind.app.core.designsystem.component.MindSelectionTopBar
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.SmartCollection
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onOpenMemory: (String) -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val collection = uiState.collection
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val gridState = rememberLazyStaggeredGridState()
    val listState = rememberLazyListState()
    val collectionHazeState = rememberHazeState()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topBarClearance = statusTopInset + 72.dp
    val bottomContentPadding = if (uiState.isSelectionMode) 96.dp + navBottom else 32.dp + navBottom
    val bottomVeilHeight = if (uiState.isSelectionMode) 88.dp + navBottom else 52.dp + navBottom

    val scrollFraction by remember {
        derivedStateOf {
            if (uiState.layoutMode == LayoutMode.GRID) {
                if (gridState.firstVisibleItemIndex > 0) 1f
                else (gridState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
            } else {
                if (listState.firstVisibleItemIndex > 0) 1f
                else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = collectionHazeState)
                .mindScrollEdgeFade(
                    topFade = statusTopInset + 64.dp,
                    bottomFade = if (uiState.isSelectionMode) 76.dp + navBottom else 38.dp + navBottom,
                ),
        ) {
            if (uiState.memories.isEmpty() && !uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = MindTheme.spacing.screenHorizontal,
                            end = MindTheme.spacing.screenHorizontal,
                            top = topBarClearance,
                            bottom = bottomContentPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CollectionMetaHeader(
                        collection = collection,
                        memoryCount = uiState.memories.size,
                        onAcceptSuggested = viewModel::acceptSuggested,
                    )
                    MindEmptyState(
                        title = "No memories in this collection",
                        subtitle = "Memories linked automatically or assigned to this collection will appear here.",
                    )
                }
            } else if (uiState.layoutMode == LayoutMode.GRID) {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = MindTheme.spacing.screenHorizontal,
                        end = MindTheme.spacing.screenHorizontal,
                        top = topBarClearance,
                        bottom = bottomContentPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (collection != null) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            CollectionMetaHeader(
                                collection = collection,
                                memoryCount = uiState.memories.size,
                                onAcceptSuggested = viewModel::acceptSuggested,
                            )
                        }
                    }
                    items(uiState.memories, key = { it.id }) { memory ->
                        val isSelected = memory.id in uiState.selectedMemoryIds
                        MindMemoryCard(
                            memory = memory,
                            layoutMode = LayoutMode.GRID,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleMemorySelection(memory.id)
                                } else {
                                    onOpenMemory(memory.id)
                                }
                            },
                            onToggleFavourite = { viewModel.toggleFavourite(memory) },
                            selectionMode = uiState.isSelectionMode,
                            selected = isSelected,
                            onLongClick = { viewModel.enterSelectionMode(memory.id) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = MindTheme.spacing.screenHorizontal,
                        end = MindTheme.spacing.screenHorizontal,
                        top = topBarClearance,
                        bottom = bottomContentPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (collection != null) {
                        item {
                            CollectionMetaHeader(
                                collection = collection,
                                memoryCount = uiState.memories.size,
                                onAcceptSuggested = viewModel::acceptSuggested,
                            )
                        }
                    }
                    items(uiState.memories, key = { it.id }) { memory ->
                        val isSelected = memory.id in uiState.selectedMemoryIds
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MindMemoryCard(
                                memory = memory,
                                layoutMode = LayoutMode.LIST,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleMemorySelection(memory.id)
                                    } else {
                                        onOpenMemory(memory.id)
                                    }
                                },
                                onToggleFavourite = { viewModel.toggleFavourite(memory) },
                                selectionMode = uiState.isSelectionMode,
                                selected = isSelected,
                                onLongClick = { viewModel.enterSelectionMode(memory.id) },
                            )
                            if (!uiState.isSelectionMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    MindPillButton(
                                        text = "Remove from collection",
                                        onClick = { viewModel.removeMemoryFromCollection(memory.id) },
                                        isPrimary = false,
                                        compact = true,
                                        leadingIcon = Icons.Rounded.BookmarkRemove,
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        MindEdgeBlurVeil(
            hazeState = collectionHazeState,
            fromTop = false,
            height = bottomVeilHeight,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (uiState.isSelectionMode) {
            MindEdgeBlurVeil(
                hazeState = collectionHazeState,
                fromTop = true,
                height = statusTopInset + 74.dp,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            MindSelectionTopBar(
                selectedCount = uiState.selectedMemoryIds.size,
                allSelected = uiState.memories.isNotEmpty() && uiState.memories.all { it.id in uiState.selectedMemoryIds },
                onClose = viewModel::clearSelection,
                onToggleSelectAll = viewModel::toggleSelectAll,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
            )
        } else {
            CompositionLocalProvider(LocalMindHazeState provides collectionHazeState) {
                MindFloatingPageTopBar(
                    title = collection?.name ?: "Space",
                    onBack = onBack,
                    scrollFraction = scrollFraction,
                    alwaysShowTitle = true,
                    modifier = Modifier.align(Alignment.TopCenter),
                    actions = {
                        if (uiState.memories.isNotEmpty()) {
                            MindGlassIconButton(
                                icon = Icons.Rounded.CheckCircleOutline,
                                contentDescription = "Select memories",
                                onClick = { viewModel.enterSelectionMode() },
                                size = 38.dp,
                                iconSize = 17.dp,
                                tier = MindGlassTier.CHROME,
                            )
                        }
                        if (collection != null) {
                            MindGlassIconButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete collection",
                                onClick = { showDeleteDialog = true },
                                size = 38.dp,
                                iconSize = 17.dp,
                                tint = MindTheme.colors.danger,
                                tier = MindGlassTier.CHROME,
                            )
                        }
                    },
                )
            }
        }

        if (uiState.isSelectionMode) {
            MindSelectionActionDock(
                selectedCount = uiState.selectedMemoryIds.size,
                secondaryActionLabel = "Remove",
                onSecondaryAction = viewModel::removeSelectedFromCollection,
                onDeleteSelected = { showBulkDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
            )
        }
    }

    if (showDeleteDialog) {
        MindGlassDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete collection?",
            subtitle = "This deletes the collection grouping only. Your saved memories stay intact.",
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { showDeleteDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Delete",
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCollection(onBack)
                    },
                    isDanger = true,
                    compact = true,
                )
            },
        )
    }

    if (showBulkDeleteConfirm && uiState.selectedMemoryIds.isNotEmpty()) {
        MindDeleteSelectedDialog(
            selectedCount = uiState.selectedMemoryIds.size,
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirmDelete = {
                showBulkDeleteConfirm = false
                viewModel.deleteSelectedMemories()
            },
        )
    }
}

@Composable
private fun CollectionMetaHeader(
    collection: SmartCollection?,
    memoryCount: Int,
    onAcceptSuggested: () -> Unit,
) {
    if (collection == null) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MindCompactPathPill(
            segments = listOfNotNull(
                "Spaces",
                collection.name,
                collection.description?.takeIf { it.isNotBlank() } ?: "$memoryCount memories",
            ),
        )
        if (collection.isSuggested) {
            MindSurfaceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Suggested by Mind AI",
                        style = MindTheme.typography.titleSmall,
                        color = MindTheme.colors.accent,
                    )
                    MindPillButton(
                        text = "Keep Space",
                        onClick = onAcceptSuggested,
                        isPrimary = false,
                        compact = true,
                        leadingIcon = Icons.Rounded.CheckCircle,
                    )
                }
            }
        }
    }
}
