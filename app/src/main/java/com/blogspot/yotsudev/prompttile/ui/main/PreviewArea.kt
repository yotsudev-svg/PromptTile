package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.blogspot.yotsudev.prompttile.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PreviewArea(
    mode: PromptMode,
    items: List<PromptItem>,
    canUndo: Boolean,
    canRedo: Boolean,
    onModeChange: (PromptMode) -> Unit,
    onRemove: (PromptItem) -> Unit,
    onWeightCycle: (PromptItem) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onCopyAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDeleteMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // ---- ポジティブ／ネガティブ タブ ----
        TabRow(
            selectedTabIndex = if (mode == PromptMode.POSITIVE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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

        // ---- ヘッダー行: カウンター + Undo/Redo + 削除モード + コピー ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val modeText = if (mode == PromptMode.POSITIVE) 
                stringResource(R.string.preview_mode_positive) 
            else 
                stringResource(R.string.preview_mode_negative)

            Text(
                text = "$modeText (${items.size})",
                style = MaterialTheme.typography.labelMedium,
                color = if (mode == PromptMode.NEGATIVE)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row {
                /**
                 * Undo ボタン。
                 * canUndo = false のとき非活性にすることで、
                 * 「これ以上戻れない」ことをユーザーに伝える。
                 */
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = stringResource(R.string.preview_undo),
                        tint = if (canUndo)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                /**
                 * Redo ボタン。
                 * 新しい操作をした後は canRedo = false になるため自動で非活性になる。
                 */
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = stringResource(R.string.preview_redo),
                        tint = if (canRedo)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                IconButton(
                    onClick = { isDeleteMode = !isDeleteMode },
                    enabled = items.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = if (isDeleteMode) 
                            stringResource(R.string.preview_delete_mode_off) 
                        else 
                            stringResource(R.string.preview_delete_mode_on),
                        tint = when {
                            items.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            isDeleteMode -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(
                    onClick = onCopyAll,
                    enabled = items.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.preview_copy),
                        tint = when {
                            items.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        )
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
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to -> onMove(from.index, to.index) },
    )

    if (items.isEmpty()) {
        Box(
            modifier = modifier.heightIn(min = 48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.preview_empty_msg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    } else {
        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.heightIn(min = 48.dp, max = 64.dp),
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
    val hasWeight = item.weight != null && item.weight != 1.0f

    val chipColors = if (mode == PromptMode.NEGATIVE) {
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }

    FilterChip(
        selected = hasWeight,
        onClick = { if (!isDeleteMode) onWeightCycle() },
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
        modifier = modifier
            .then(dragModifier)
            .then(
                if (isDragging) Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                ) else Modifier
            ),
    )
}