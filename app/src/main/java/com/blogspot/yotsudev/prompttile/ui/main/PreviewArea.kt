package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PreviewArea(
    mode: PromptMode,
    items: List<PromptItem>,
    promptText: String,
    allTemplates: List<SavedPromptEntity>,
    canUndo: Boolean,
    canRedo: Boolean,
    onModeChange: (PromptMode) -> Unit,
    onRemove: (PromptItem) -> Unit,
    onWeightCycle: (PromptItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onCopyAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAddTemplate: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
) {
    var isDeleteMode by remember { mutableStateOf(false) }
    var showTemplateMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ---- ポジティブ／ネガティブ タブ ----
        TabRow(
            selectedTabIndex = if (mode == PromptMode.POSITIVE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.background,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        ) {
            Tab(
                selected = mode == PromptMode.POSITIVE,
                onClick = {
                    isDeleteMode = false
                    onModeChange(PromptMode.POSITIVE)
                },
                text = {
                    Text(
                        text = stringResource(R.string.preview_mode_positive),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (mode == PromptMode.POSITIVE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
            )
            Tab(
                selected = mode == PromptMode.NEGATIVE,
                onClick = {
                    isDeleteMode = false
                    onModeChange(PromptMode.NEGATIVE)
                },
                text = {
                    Text(
                        text = stringResource(R.string.preview_mode_negative),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (mode == PromptMode.NEGATIVE)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
            )
        }

        // ---- ヘッダー行: カウンター + [Undo/Redo] | [Delete/Template] | [Copy] ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val modeColor = if (mode == PromptMode.NEGATIVE)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Text(
                text = "${if (mode == PromptMode.POSITIVE) stringResource(R.string.preview_mode_positive) else stringResource(R.string.preview_mode_negative)} (${items.size})",
                style = MaterialTheme.typography.labelMedium,
                color = modeColor.copy(alpha = 0.8f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // グループ1: 履歴操作
                Row {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = stringResource(R.string.preview_undo),
                            modifier = Modifier.size(20.dp),
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = stringResource(R.string.preview_redo),
                            modifier = Modifier.size(20.dp),
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.VerticalDivider(modifier = Modifier.height(16.dp).alpha(0.3f))
                Spacer(modifier = Modifier.width(4.dp))

                // グループ2: 編集・補助
                Row {
                    IconButton(
                        onClick = { isDeleteMode = !isDeleteMode },
                        enabled = items.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = if (isDeleteMode) stringResource(R.string.preview_delete_mode_off) else stringResource(R.string.preview_delete_mode_on),
                            modifier = Modifier.size(20.dp),
                            tint = when {
                                items.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                isDeleteMode -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    Box {
                        IconButton(onClick = { showTemplateMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = "Quality Template",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            allTemplates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.title, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onAddTemplate(template.promptText)
                                        showTemplateMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
                androidx.compose.material3.VerticalDivider(modifier = Modifier.height(16.dp).alpha(0.3f))
                Spacer(modifier = Modifier.width(4.dp))

                // グループ3: 出力
                IconButton(
                    onClick = onCopyAll,
                    enabled = items.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.preview_copy),
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            items.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            mode == PromptMode.NEGATIVE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        // ---- チップ一覧 ----
        ChipList(
            items = items,
            mode = mode,
            isDeleteMode = isDeleteMode,
            onRemove = onRemove,
            onWeightCycle = onWeightCycle,
            onMove = onMove,
            isVertical = isVertical,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .then(if (isVertical) Modifier.weight(1f) else Modifier),
        )

        // ---- 最終プロンプト表示エリア (Stable Layout) ----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .then(if (isVertical) Modifier.heightIn(min = 72.dp, max = 240.dp) else Modifier.height(72.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            tonalElevation = 1.dp
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                val scrollState = rememberScrollState()
                val isEmpty = items.isEmpty()
                Text(
                    text = if (isEmpty) stringResource(R.string.preview_placeholder) else promptText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    color = if (isEmpty) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .then(
                            if (isVertical) Modifier.verticalScroll(scrollState) 
                            else Modifier.horizontalScroll(scrollState)
                        ),
                )
            }
        }
    }
}

@Composable
private fun ChipList(
    items: List<PromptItem>,
    mode: PromptMode,
    isDeleteMode: Boolean,
    onRemove: (PromptItem) -> Unit,
    onWeightCycle: (PromptItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    isVertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to -> onMove(from.index, to.index) },
    )

    if (items.isEmpty()) {
        Box(
            modifier = modifier.height(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).alpha(0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.preview_empty_msg),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
    } else if (isVertical) {
        // 縦型表示（3ペイン時）: 縦に並べる
        androidx.compose.foundation.lazy.LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.wordId },
            ) { _, item ->
                ReorderableItem(state = reorderState, key = item.wordId) { isDragging ->
                    PromptChip(
                        item = item,
                        mode = mode,
                        isDeleteMode = isDeleteMode,
                        isDragging = isDragging,
                        onWeightCycle = { onWeightCycle(item) },
                        onRemove = { onRemove(item) },
                        dragModifier = Modifier.longPressDraggableHandle(),
                        modifier = Modifier.fillMaxWidth() // 縦並びなので幅いっぱいに
                    )
                }
            }
        }
    } else {
        // 横型表示（モバイル時）: 従来通り
        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.height(64.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.wordId },
            ) { _, item ->
                ReorderableItem(state = reorderState, key = item.wordId) { isDragging ->
                    PromptChip(
                        item = item,
                        mode = mode,
                        isDeleteMode = isDeleteMode,
                        isDragging = isDragging,
                        onWeightCycle = { onWeightCycle(item) },
                        onRemove = { onRemove(item) },
                        dragModifier = Modifier.longPressDraggableHandle(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptChip(
    item: PromptItem,
    mode: PromptMode,
    isDeleteMode: Boolean,
    isDragging: Boolean,
    onWeightCycle: () -> Unit,
    onRemove: () -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hasWeight = item.weight != null && item.weight != 1.0f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "scale"
    )

    val chipColors = if (mode == PromptMode.NEGATIVE) {
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    FilterChip(
        selected = hasWeight,
        onClick = { if (!isDeleteMode) onWeightCycle() },
        interactionSource = interactionSource,
        label = {
            Text(text = item.formatted, style = MaterialTheme.typography.bodySmall)
        },
        trailingIcon = if (isDeleteMode) {
            {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.preview_remove_word).format(item.wordEn),
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            }
        } else null,
        colors = chipColors,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = hasWeight,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            selectedBorderColor = if (mode == PromptMode.NEGATIVE) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            borderWidth = 0.5.dp,
            selectedBorderWidth = 1.dp
        ),
        modifier = modifier
            .then(dragModifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isDragging) Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                ) else Modifier
            ),
    )
}
