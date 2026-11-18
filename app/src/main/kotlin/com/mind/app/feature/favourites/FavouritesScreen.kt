package com.mind.app.feature.favourites

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.core.designsystem.component.MindDeleteSelectedDialog
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindEmptyState
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindMemoryCard
import com.mind.app.core.designsystem.component.MindSegmentedPillBar
import com.mind.app.core.designsystem.component.MindSelectionActionDock
import com.mind.app.core.designsystem.component.MindSelectionTopBar
import com.mind.app.core.designsystem.component.OneUiExpandedHeader
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.LayoutMode
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun FavouritesScreen(
    onOpenMemory: (String) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listTopPadding = if (uiState.isSelectionMode) statusTopInset + 64.dp else statusTopInset + 6.dp
    val listBottomPadding = 110.dp + navBottomInset
    val favHazeState = rememberHazeState()
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val headerBlock: @Composable () -> Unit = {
        OneUiExpandedHeader(
            title = "Favorites",
            subtitle = "${uiState.memories.size} favorite memories",
            modifier = Modifier.padding(horizontal = 2.dp),
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.memories.isNotEmpty()) {
                        MindGlassIconButton(
                            icon = Icons.Rounded.CheckCircleOutline,
                            contentDescription = "Select memories",
                            onClick = { viewModel.enterSelectionMode() },
                            size = 38.dp,
                            iconSize = 18.dp,
                            tier = MindGlassTier.CHROME,
                        )
                    }
                    MindSegmentedPillBar(
                        items = LayoutMode.entries,
                        selectedItem = uiState.layoutMode,
                        labelProvider = { if (it == LayoutMode.GRID) "Grid" else "List" },
                        onSelect = { target ->
                            if (target != uiState.layoutMode) {
                                viewModel.toggleLayoutMode()
                            }
                        },
                        modifier = Modifier.width(120.dp),
                    )
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(favHazeState)
                .mindScrollEdgeFade(
                    topFade = statusTopInset + 16.dp,
                    bottomFade = 72.dp + navBottomInset,
                ),
        ) {
            if (uiState.memories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = MindTheme.spacing.screenHorizontal,
                            end = MindTheme.spacing.screenHorizontal,
                            top = listTopPadding,
                            bottom = listBottomPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!uiState.isSelectionMode) {
                        headerBlock()
                    }
                    MindEmptyState(
                        title = "No pinned memories yet",
                        subtitle = "Tap the heart pill on any memory card or detail screen to keep key items right here.",
                    )
                }
            } else {
                AnimatedContent(
                    targetState = uiState.layoutMode,
                    transitionSpec = {
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                            .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
                    },
                    label = "fav_layout_mode_transition",
                ) { mode ->
                    if (mode == LayoutMode.GRID) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = MindTheme.spacing.screenHorizontal,
                                end = MindTheme.spacing.screenHorizontal,
                                top = listTopPadding,
                                bottom = listBottomPadding,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (!uiState.isSelectionMode) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    headerBlock()
                                }
                            }
                            items(uiState.memories, key = { it.id }) { memory ->
                                val isSelected = memory.id in uiState.selectedMemoryIds
                                MindMemoryCard(
                                    memory = memory,
                                    layoutMode = LayoutMode.GRID,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(260),
                                        placementSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                                        fadeOutSpec = tween(180),
                                    ),
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
                            contentPadding = PaddingValues(
                                start = MindTheme.spacing.screenHorizontal,
                                end = MindTheme.spacing.screenHorizontal,
                                top = listTopPadding,
                                bottom = listBottomPadding,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (!uiState.isSelectionMode) {
                                item {
                                    headerBlock()
                                }
                            }
                            items(uiState.memories, key = { it.id }) { memory ->
                                val isSelected = memory.id in uiState.selectedMemoryIds
                                MindMemoryCard(
                                    memory = memory,
                                    layoutMode = LayoutMode.LIST,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(260),
                                        placementSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                                        fadeOutSpec = tween(180),
                                    ),
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
                    }
                }
            }
        }

        if (uiState.memories.isNotEmpty()) {
            MindEdgeBlurVeil(
                hazeState = favHazeState,
                fromTop = true,
                height = statusTopInset + 14.dp,
                tintAlpha = 0.10f,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            MindEdgeBlurVeil(
                hazeState = favHazeState,
                fromTop = false,
                height = 74.dp + navBottomInset,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (uiState.isSelectionMode) {
            MindSelectionTopBar(
                selectedCount = uiState.selectedMemoryIds.size,
                allSelected = uiState.memories.isNotEmpty() && uiState.memories.all { it.id in uiState.selectedMemoryIds },
                onClose = viewModel::clearSelection,
                onToggleSelectAll = viewModel::toggleSelectAll,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
            )
        }

        if (uiState.isSelectionMode) {
            MindSelectionActionDock(
                selectedCount = uiState.selectedMemoryIds.size,
                onDeleteSelected = { showBulkDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp),
            )
        }
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
