package com.mind.app.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindDeleteSelectedDialog
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindEmptyState
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindMemoryCard
import com.mind.app.core.designsystem.component.MindPastelIconDisc
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSelectionActionDock
import com.mind.app.core.designsystem.component.MindSelectionTopBar
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.OneUiListDivider
import com.mind.app.core.designsystem.component.dshPressableScale
import com.mind.app.core.designsystem.component.mindGlassBackdrop
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.component.mindHorizontalScrollEdgeFade
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.LayoutMode
import com.mind.app.feature.collections.MindSpacePickerSheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun SearchScreen(
    onOpenMemory: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasStructuredFilter = uiState.filter.categoryId != null ||
        uiState.filter.contentTypes.isNotEmpty() ||
        uiState.filter.favouritesOnly
    val hasActiveFilter = uiState.filter.query.isNotBlank() || hasStructuredFilter

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = 110.dp + navBottomInset
    val visibleSelectableIds = uiState.visibleSelectableIds
    val snackbarHostState = remember { SnackbarHostState() }
    val searchHazeState = rememberHazeState()
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showSpacePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStatusMessage()
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    CompositionLocalProvider(LocalMindHazeState provides searchHazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                if (uiState.isSelectionMode) {
                    MindSelectionTopBar(
                        selectedCount = uiState.selectedMemoryIds.size,
                        allSelected = visibleSelectableIds.isNotEmpty() && visibleSelectableIds.all { it in uiState.selectedMemoryIds },
                        onClose = viewModel::clearSelection,
                        onToggleSelectAll = viewModel::toggleSelectAllVisible,
                    )
                }

                // 1. Spacious Full-Width Search Bar + Edge-to-Edge Feathered Category & Type Rails
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SearchCapsuleField(
                        query = uiState.filter.query,
                        isAiRunning = uiState.isAiRunning,
                        hazeState = searchHazeState,
                        onQueryChange = viewModel::updateQuery,
                        onClearQuery = { viewModel.updateQuery("") },
                        onSubmitSmartQuery = { viewModel.submitSmartQuery() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MindTheme.spacing.screenHorizontal),
                    )

                    // Edge-to-edge Category Rail with horizontal cosine fade (no sharp cutout border)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .mindHorizontalScrollEdgeFade(leftFade = 24.dp, rightFade = 24.dp),
                        contentPadding = PaddingValues(horizontal = MindTheme.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item(key = "filter_all") {
                            SearchFilterPill(
                                label = "All",
                                selected = !hasStructuredFilter,
                                onClick = viewModel::clearFilters,
                            )
                        }
                        item(key = "filter_fav") {
                            SearchFilterPill(
                                label = "Favourites",
                                selected = uiState.filter.favouritesOnly,
                                onClick = viewModel::toggleFavouritesOnly,
                            )
                        }
                        items(uiState.categories, key = { "cat_${it.category.id}" }) { node ->
                            val selected = uiState.filter.categoryId == node.category.id ||
                                node.subcategories.any { it.category.id == uiState.filter.categoryId }
                            SearchFilterPill(
                                label = node.category.name,
                                selected = selected,
                                onClick = { viewModel.selectCategory(node.category.id) },
                            )
                        }
                    }

                    // Edge-to-edge Content-Type Rail with horizontal cosine fade
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .mindHorizontalScrollEdgeFade(leftFade = 24.dp, rightFade = 24.dp),
                        contentPadding = PaddingValues(horizontal = MindTheme.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val quickTypes = listOf(
                            ContentType.SCREENSHOT to "Screenshots",
                            ContentType.VOICE_NOTE to "Voice",
                            ContentType.PRODUCT to "Products",
                            ContentType.URL to "Links",
                            ContentType.ERROR_LOG to "Errors",
                            ContentType.PLACE to "Places",
                            ContentType.QUOTE to "Quotes",
                        )
                        items(quickTypes, key = { "type_${it.first.name}" }) { (type, label) ->
                            SearchFilterPill(
                                label = label,
                                selected = type in uiState.filter.contentTypes,
                                onClick = { viewModel.toggleContentType(type) },
                            )
                        }
                    }

                    // Clean sub-bar: result count on left, Reset / Select / View mode on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MindTheme.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val suffix = if (uiState.semanticMatchIds.isNotEmpty()) {
                            " · ${uiState.semanticMatchIds.size} AI match"
                        } else {
                            ""
                        }
                        Text(
                            text = if (hasActiveFilter) {
                                "${uiState.results.size} matching ${if (uiState.results.size == 1) "memory" else "memories"}$suffix"
                            } else {
                                "${uiState.results.size} saved ${if (uiState.results.size == 1) "memory" else "memories"}"
                            },
                            style = MindTheme.typography.labelMedium,
                            color = MindTheme.colors.textSecondary,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = hasActiveFilter,
                                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.9f, animationSpec = tween(180)),
                                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.9f, animationSpec = tween(140)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MindTheme.colors.accentSoft)
                                        .clickable(onClick = viewModel::clearFilters)
                                        .padding(horizontal = 11.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "Reset",
                                        style = MindTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MindTheme.colors.accent,
                                    )
                                }
                            }

                            if (!uiState.isSelectionMode && visibleSelectableIds.isNotEmpty()) {
                                MindGlassIconButton(
                                    icon = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = "Select memories",
                                    onClick = { viewModel.enterSelectionMode() },
                                    size = 36.dp,
                                    iconSize = 17.dp,
                                    tier = MindGlassTier.CHROME,
                                )
                            }

                            MindGlassIconButton(
                                icon = if (uiState.layoutMode == LayoutMode.GRID) {
                                    Icons.Rounded.ViewAgenda
                                } else {
                                    Icons.Rounded.GridView
                                },
                                contentDescription = if (uiState.layoutMode == LayoutMode.GRID) {
                                    "Switch to list view"
                                } else {
                                    "Switch to grid view"
                                },
                                onClick = viewModel::toggleLayoutMode,
                                size = 36.dp,
                                iconSize = 17.dp,
                                tier = MindGlassTier.CHROME,
                            )
                        }
                    }
                }

                // 2. Animated Search Results Canvas with Top/Bottom Edge Feather
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = searchHazeState)
                            .mindScrollEdgeFade(
                                topFade = 24.dp,
                                bottomFade = 72.dp + navBottomInset,
                            ),
                    ) {
                        AnimatedContent(
                            targetState = uiState.layoutMode == LayoutMode.GRID && uiState.results.isNotEmpty(),
                            transitionSpec = {
                                (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                                    .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
                            },
                            label = "search_layout_mode_transition",
                        ) { showGrid ->
                            if (showGrid) {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    contentPadding = PaddingValues(
                                        start = MindTheme.spacing.screenHorizontal,
                                        end = MindTheme.spacing.screenHorizontal,
                                        top = 12.dp,
                                        bottom = listBottomPadding,
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalItemSpacing = 12.dp,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    uiState.askAnswer?.let { answer ->
                                        item(key = "ai_answer_card", span = StaggeredGridItemSpan.FullLine) {
                                            SearchAiAnswerCard(
                                                answerText = answer.answerText,
                                                onDismiss = viewModel::dismissAiAssist,
                                            )
                                        }
                                    }

                                    uiState.aiError?.let { err ->
                                        item(key = "ai_error_card", span = StaggeredGridItemSpan.FullLine) {
                                            MindSurfaceCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                borderColor = MindTheme.colors.danger.copy(alpha = 0.35f),
                                            ) {
                                                Text(
                                                    text = err,
                                                    style = MindTheme.typography.bodySmall,
                                                    color = MindTheme.colors.danger,
                                                    modifier = Modifier.padding(14.dp),
                                                )
                                            }
                                        }
                                    }

                                    gridItems(uiState.results, key = { it.id }) { memory ->
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
                                        top = 12.dp,
                                        bottom = listBottomPadding,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    uiState.askAnswer?.let { answer ->
                                        item(key = "ai_answer_card") {
                                            SearchAiAnswerCard(
                                                answerText = answer.answerText,
                                                onDismiss = viewModel::dismissAiAssist,
                                            )
                                        }
                                    }

                                    uiState.aiError?.let { err ->
                                        item(key = "ai_error_card") {
                                            MindSurfaceCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                borderColor = MindTheme.colors.danger.copy(alpha = 0.35f),
                                            ) {
                                                Text(
                                                    text = err,
                                                    style = MindTheme.typography.bodySmall,
                                                    color = MindTheme.colors.danger,
                                                    modifier = Modifier.padding(14.dp),
                                                )
                                            }
                                        }
                                    }

                                    if (!hasActiveFilter && uiState.results.isEmpty()) {
                                        item(key = "prompt_starters") {
                                            val starters = listOf(
                                                "What products did I save recently?",
                                                "Summarize my error logs or debugging captures",
                                                "Find any restaurants or places I saved",
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "Ask or recall naturally",
                                                    style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MindTheme.colors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                )
                                                MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                                    starters.forEachIndexed { index, suggestion ->
                                                        if (index > 0) {
                                                            OneUiListDivider(insetStart = true)
                                                        }
                                                        PromptStarterRow(
                                                            prompt = suggestion,
                                                            tintIndex = index,
                                                            onClick = { viewModel.submitSmartQuery(suggestion) },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (uiState.results.isEmpty() && hasActiveFilter && !uiState.isAiRunning) {
                                        item(key = "empty_results") {
                                            val canRunAi = !uiState.hasRunAiSearch && uiState.filter.query.isNotBlank()
                                            MindEmptyState(
                                                title = "No matching memories",
                                                subtitle = if (canRunAi) {
                                                    "No exact keyword matches. Tap Ask Mind to search by meaning with Gemini."
                                                } else {
                                                    "Try different keywords or clear active filters."
                                                },
                                                action = {
                                                    MindPillButton(
                                                        text = if (canRunAi) "Ask Mind AI" else "Clear Filters",
                                                        onClick = {
                                                            if (canRunAi) {
                                                                viewModel.submitSmartQuery()
                                                            } else {
                                                                viewModel.clearFilters()
                                                            }
                                                        },
                                                        compact = true,
                                                        leadingIcon = if (canRunAi) Icons.Rounded.AutoAwesome else null,
                                                    )
                                                },
                                            )
                                        }
                                    } else {
                                        items(uiState.results, key = { it.id }) { memory ->
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

                    MindEdgeBlurVeil(
                        hazeState = searchHazeState,
                        fromTop = false,
                        height = 72.dp + navBottomInset,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            if (uiState.isSelectionMode) {
                MindSelectionActionDock(
                    selectedCount = uiState.selectedMemoryIds.size,
                    onDeleteSelected = { showBulkDeleteConfirm = true },
                    secondaryActionLabel = "Add to Space",
                    onSecondaryAction = { showSpacePicker = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 90.dp),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 144.dp),
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

    if (showSpacePicker && uiState.selectedMemoryIds.isNotEmpty()) {
        MindSpacePickerSheet(
            title = "Add ${uiState.selectedMemoryIds.size} ${if (uiState.selectedMemoryIds.size == 1) "item" else "items"} to Space",
            collections = uiState.collections,
            onSelectCollection = { collection ->
                showSpacePicker = false
                viewModel.addSelectedMemoriesToCollection(collection.id, collection.name)
            },
            onCreateAndSelect = { name ->
                showSpacePicker = false
                viewModel.createSpaceAndAddSelected(name)
            },
            onDismiss = { showSpacePicker = false },
        )
    }
}

@Composable
private fun SearchCapsuleField(
    query: String,
    isAiRunning: Boolean,
    hazeState: HazeState?,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmitSmartQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val barShape = CircleShape
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 10.dp,
                shape = barShape,
                ambientColor = colors.shadowInk,
                spotColor = colors.shadowInk,
            )
            .then(
                if (hazeState != null) {
                    Modifier.mindGlassBackdrop(
                        shape = barShape,
                        hazeState = hazeState,
                        blurRadius = 24.dp,
                    )
                } else {
                    Modifier.clip(barShape)
                },
            )
            .mindGlassEdge(
                shape = barShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
                strokeWidth = 0.75.dp,
            ),
        shape = barShape,
        color = if (hazeState != null) Color.Transparent else colors.glassCardFill,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search memories or ask Mind AI...",
                        style = MindTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MindTheme.typography.bodyLarge.copy(
                        fontSize = 15.5.sp,
                        color = colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitSmartQuery() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.glassPillFill)
                        .clickable(onClick = onClearQuery),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (isAiRunning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent,
                )
            } else {
                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180)),
                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.88f, animationSpec = tween(140)),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.accent, colors.accentSecondary),
                                ),
                            )
                            .clickable(onClick = onSubmitSmartQuery)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Ask Mind AI",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Ask AI",
                                style = MindTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAiAnswerCard(
    answerText: String,
    onDismiss: () -> Unit,
) {
    val colors = MindTheme.colors
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        shape = cardShape,
        color = colors.glassCardFill,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .mindGlassEdge(
                shape = cardShape,
                hairlineColor = colors.accent.copy(alpha = 0.24f),
                rimColor = colors.rimHighlight,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.accentSoft.copy(alpha = 0.34f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MindPastelIconDisc(
                        icon = Icons.Rounded.AutoAwesome,
                        tintIndex = 0,
                        size = 28.dp,
                        iconSize = 15.dp,
                    )
                    Text(
                        text = "Answer from your memories",
                        style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss AI answer",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = answerText,
                style = MindTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SearchFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.glassPillFill,
        animationSpec = tween(180),
        label = "search_pill_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else colors.textSecondary,
        animationSpec = tween(180),
        label = "search_pill_fg",
    )
    Box(
        modifier = Modifier
            .dshPressableScale(interactionSource, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (!selected) {
                    Modifier.mindGlassEdge(
                        shape = CircleShape,
                        hairlineColor = colors.hairline,
                        rimColor = colors.rimHighlight,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MindTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
        )
    }
}

@Composable
private fun PromptStarterRow(
    prompt: String,
    tintIndex: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MindPastelIconDisc(
            icon = Icons.Rounded.AutoAwesome,
            tintIndex = tintIndex,
            size = 30.dp,
            iconSize = 15.dp,
        )
        Text(
            text = prompt,
            style = MindTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MindTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MindTheme.colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
