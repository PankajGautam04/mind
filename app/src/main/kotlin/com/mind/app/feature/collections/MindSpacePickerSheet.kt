package com.mind.app.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.SmartCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindSpacePickerSheet(
    title: String,
    collections: List<SmartCollection>,
    assignedCollectionIds: Set<String> = emptySet(),
    onSelectCollection: (SmartCollection) -> Unit,
    onCreateAndSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MindTheme.colors
    var newSpaceName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.glassChromeFallback,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MindGlassTextField(
                    value = newSpaceName,
                    onValueChange = { newSpaceName = it },
                    placeholder = "Create new Space...",
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                MindPillButton(
                    text = "Create",
                    onClick = {
                        val trimmed = newSpaceName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreateAndSelect(trimmed)
                            newSpaceName = ""
                        }
                    },
                    enabled = newSpaceName.isNotBlank(),
                    compact = true,
                    leadingIcon = Icons.Rounded.Add,
                )
            }

            if (collections.isEmpty()) {
                Text(
                    text = "No Spaces yet. Create one above to organize memories effortlessly.",
                    style = MindTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(collections, key = { it.id }) { collection ->
                        val isAssigned = collection.id in assignedCollectionIds
                        val itemShape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .background(
                                    if (isAssigned) colors.accentSoft else colors.surfaceSubtle,
                                )
                                .mindGlassEdge(
                                    shape = itemShape,
                                    hairlineColor = if (isAssigned) colors.accent.copy(alpha = 0.45f) else colors.hairline,
                                    rimColor = colors.rimHighlight,
                                )
                                .clickable { onSelectCollection(collection) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = if (isAssigned) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = collection.name,
                                        style = MindTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${collection.memoryCount} ${if (collection.memoryCount == 1) "item" else "items"}",
                                        style = MindTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                    )
                                }
                                if (isAssigned) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "In Space",
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
