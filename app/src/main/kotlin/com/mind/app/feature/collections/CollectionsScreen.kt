package com.mind.app.feature.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindGlassDialog
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSelectableChip
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.MindTagPill
import com.mind.app.core.designsystem.component.OneUiExpandedHeader
import com.mind.app.core.designsystem.component.OneUiSectionTitle
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.domain.usecase.ManageCategoryUseCase
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.SmartCollection
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionsScreen(
    onOpenCategorySearch: (String) -> Unit,
    onOpenCollection: (String) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var subcategoryParent by remember { mutableStateOf<CategoryTreeNode?>(null) }
    var renameTarget by remember { mutableStateOf<CategoryTreeNode?>(null) }
    var mergeSource by remember { mutableStateOf<CategoryTreeNode?>(null) }
    var deleteSource by remember { mutableStateOf<CategoryTreeNode?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = 110.dp + navBottomInset
    val collectionsHazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(collectionsHazeState)
                .mindScrollEdgeFade(
                    topFade = statusTopInset + 22.dp,
                    bottomFade = 72.dp + navBottomInset,
                ),
            contentPadding = PaddingValues(top = statusTopInset, bottom = listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OneUiExpandedHeader(
                    title = "Spaces",
                    subtitle = "Smart groupings, folders, and editable topics",
                    actions = {
                        MindGlassIconButton(
                            icon = Icons.Rounded.CreateNewFolder,
                            contentDescription = "Create collection",
                            onClick = { showCreateCollectionDialog = true },
                            size = 40.dp,
                            iconSize = 19.dp,
                            tier = MindGlassTier.CHROME,
                        )
                    },
                )
            }

            item {
                Box(modifier = Modifier.padding(horizontal = MindTheme.spacing.screenHorizontal)) {
                    OneUiSectionTitle("Smart & Personal Spaces (${uiState.collections.size})")
                }
            }

            if (uiState.collections.isNotEmpty()) {
                val collectionRows = uiState.collections.chunked(2)
                items(
                    items = collectionRows,
                    key = { row -> "space_row_${row.first().id}" },
                ) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = tween(260),
                                placementSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                                fadeOutSpec = tween(180),
                            )
                            .padding(horizontal = MindTheme.spacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { collection ->
                            SpaceBentoCard(
                                collection = collection,
                                onClick = { onOpenCollection(collection.id) },
                                onAcceptSuggested = { viewModel.acceptSuggestedCollection(collection.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.padding(horizontal = MindTheme.spacing.screenHorizontal)) {
                        MindSurfaceCard(onClick = { showCreateCollectionDialog = true }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Create your first Collection",
                                        style = MindTheme.typography.titleSmall,
                                        color = MindTheme.colors.textPrimary,
                                    )
                                    Text(
                                        text = "Group related captures manually or accept Mind AI's smart suggestions.",
                                        style = MindTheme.typography.bodySmall,
                                        color = MindTheme.colors.textSecondary,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MindTheme.colors.accent,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MindTheme.spacing.screenHorizontal)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OneUiSectionTitle("Categories (${uiState.categoryTree.size})")
                    MindPillButton(
                        text = "New Category",
                        onClick = { showCreateCategoryDialog = true },
                        isPrimary = false,
                        compact = true,
                        leadingIcon = Icons.Rounded.Add,
                    )
                }
            }

            items(uiState.categoryTree, key = { it.category.id }) { node ->
                Box(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(260),
                            placementSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                            fadeOutSpec = tween(180),
                        )
                        .padding(horizontal = MindTheme.spacing.screenHorizontal),
                ) {
                    CategoryTreeCard(
                        node = node,
                        expanded = node.category.id in uiState.expandedCategoryIds,
                        onToggleExpand = { viewModel.toggleCategoryExpanded(node.category.id) },
                        onSelectCategory = onOpenCategorySearch,
                        onAddSubcategory = { subcategoryParent = node },
                        onRename = { renameTarget = node },
                        onMerge = { mergeSource = node },
                        onDelete = { deleteSource = node },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        MindEdgeBlurVeil(
            hazeState = collectionsHazeState,
            fromTop = true,
            height = statusTopInset + 28.dp,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        MindEdgeBlurVeil(
            hazeState = collectionsHazeState,
            fromTop = false,
            height = 84.dp + navBottomInset,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }

    if (showCreateCategoryDialog) {
        SimpleCreateDialog(
            title = "New Category",
            label = "Category name",
            onDismiss = { showCreateCategoryDialog = false },
            onConfirm = { name, _ ->
                viewModel.createCategory(name)
                showCreateCategoryDialog = false
            },
        )
    }

    subcategoryParent?.let { parent ->
        SimpleCreateDialog(
            title = "New Subcategory in ${parent.category.name}",
            label = "Subcategory name",
            onDismiss = { subcategoryParent = null },
            onConfirm = { name, _ ->
                viewModel.createCategory(name, parentId = parent.category.id)
                subcategoryParent = null
            },
        )
    }

    renameTarget?.let { target ->
        SimpleCreateDialog(
            title = "Rename Category",
            label = "Category name",
            initialName = target.category.name,
            confirmLabel = "Rename",
            onDismiss = { renameTarget = null },
            onConfirm = { name, _ ->
                viewModel.renameCategory(target.category.id, name)
                renameTarget = null
            },
        )
    }

    mergeSource?.let { source ->
        val candidates = uiState.categoryTree.filter { it.category.id != source.category.id }
        var selectedTargetId by remember(source.category.id) {
            mutableStateOf(candidates.firstOrNull()?.category?.id)
        }
        MindGlassDialog(
            onDismissRequest = { mergeSource = null },
            title = "Merge \"${source.category.name}\"",
            subtitle = "Move all memories in ${source.category.name} into:",
            content = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    candidates.forEach { target ->
                        MindSelectableChip(
                            label = target.category.name,
                            selected = selectedTargetId == target.category.id,
                            onClick = { selectedTargetId = target.category.id },
                        )
                    }
                }
            },
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { mergeSource = null },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Merge",
                    onClick = {
                        selectedTargetId?.let { targetId ->
                            viewModel.mergeCategory(source.category.id, targetId)
                        }
                        mergeSource = null
                    },
                    enabled = selectedTargetId != null,
                    compact = true,
                )
            },
        )
    }

    deleteSource?.let { source ->
        val reassignOptions = uiState.categoryTree.filter { it.category.id != source.category.id }
        var reassignId by remember(source.category.id) {
            mutableStateOf(ManageCategoryUseCase.UNCATEGORIZED_ID)
        }
        MindGlassDialog(
            onDismissRequest = { deleteSource = null },
            title = "Delete \"${source.category.name}\"?",
            subtitle = "Reassign its ${source.memoryCount} memories to:",
            content = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MindSelectableChip(
                        label = "Uncategorized",
                        selected = reassignId == ManageCategoryUseCase.UNCATEGORIZED_ID,
                        onClick = { reassignId = ManageCategoryUseCase.UNCATEGORIZED_ID },
                    )
                    reassignOptions.take(6).forEach { target ->
                        MindSelectableChip(
                            label = target.category.name,
                            selected = reassignId == target.category.id,
                            onClick = { reassignId = target.category.id },
                        )
                    }
                }
            },
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { deleteSource = null },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteCategory(source.category.id, reassignId)
                        deleteSource = null
                    },
                    isDanger = true,
                    compact = true,
                )
            },
        )
    }

    if (showCreateCollectionDialog) {
        SimpleCreateDialog(
            title = "New Collection",
            label = "Collection name",
            includeDescription = true,
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, description ->
                viewModel.createCollection(name, description)
                showCreateCollectionDialog = false
            },
        )
    }
}

@Composable
private fun SpaceBentoCard(
    collection: SmartCollection,
    onClick: () -> Unit,
    onAcceptSuggested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MindSurfaceCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MindTheme.colors.accentSoft, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (collection.isSmartCollection) Icons.Rounded.AutoAwesome else Icons.Rounded.FolderCopy,
                        contentDescription = null,
                        tint = MindTheme.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MindTheme.colors.surfaceElevated)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${collection.memoryCount}",
                        style = MindTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MindTheme.colors.textSecondary,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = collection.name,
                    style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MindTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = collection.description?.takeIf { it.isNotBlank() }
                        ?: if (collection.isSmartCollection) "Smart AI Space" else "Personal Space",
                    style = MindTheme.typography.bodySmall,
                    color = MindTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (collection.isSuggested) {
                MindPillButton(
                    text = "Keep Space",
                    onClick = onAcceptSuggested,
                    isPrimary = false,
                    compact = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTreeCard(
    node: CategoryTreeNode,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onAddSubcategory: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isProtected = node.category.id == ManageCategoryUseCase.UNCATEGORIZED_ID

    MindSurfaceCard(onClick = { onSelectCategory(node.category.id) }) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.category.name,
                        style = MindTheme.typography.titleMedium,
                        color = MindTheme.colors.textPrimary,
                    )
                    Text(
                        text = "${node.memoryCount} memories · ${node.subcategories.size} subcategories",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (node.subcategories.isNotEmpty()) {
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "cat_chevron_rot",
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleExpand),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ExpandMore,
                                contentDescription = "Expand subcategories",
                                tint = MindTheme.colors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = chevronRotation },
                            )
                        }
                    }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Category options",
                                tint = MindTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = MindTheme.colors.glassChromeFallback,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add Subcategory",
                                        color = MindTheme.colors.textPrimary,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAddSubcategory()
                                },
                            )
                            if (!isProtected) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Rename",
                                            color = MindTheme.colors.textPrimary,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRename()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Merge into...",
                                            color = MindTheme.colors.textPrimary,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onMerge()
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete Category",
                                            color = MindTheme.colors.danger,
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded && node.subcategories.isNotEmpty(),
                enter = expandVertically(spring(dampingRatio = 0.80f, stiffness = 380f)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
            ) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    node.subcategories.forEach { sub ->
                        MindTagPill(
                            label = "${sub.category.name} (${sub.memoryCount})",
                            modifier = Modifier.clickable { onSelectCategory(sub.category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleCreateDialog(
    title: String,
    label: String,
    initialName: String = "",
    confirmLabel: String = "Create",
    includeDescription: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember { mutableStateOf("") }

    MindGlassDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            MindGlassTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Enter $label",
                label = label,
                singleLine = true,
            )
            if (includeDescription) {
                MindGlassTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Optional short description",
                    label = "Description (optional)",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                )
            }
        },
        actions = {
            MindPillButton(
                text = "Cancel",
                onClick = onDismiss,
                isPrimary = false,
                compact = true,
            )
            MindPillButton(
                text = confirmLabel,
                onClick = { onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank(),
                compact = true,
            )
        },
    )
}
