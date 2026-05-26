package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem

/**
 * カテゴリ編集カード。
 * ドラッグ＆ドロップによる単語の並び替え機能を内包しています。
 */
@Composable
fun CategoryEditCard(
    category: CategoryEntity,
    isExpanded: Boolean,
    words: List<PromptWordEntity>,
    onToggleExpand: () -> Unit,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryClick: () -> Unit,
    onToggleCategoryVisibility: () -> Unit,
    onAddWordClick: () -> Unit,
    onEditWordClick: (PromptWordEntity) -> Unit,
    onDeleteWordClick: (PromptWordEntity) -> Unit,
    onToggleWordVisibility: (PromptWordEntity) -> Unit,
    onReorderWords: (Int, Int) -> Unit,
    onSettleWords: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    val containerAlpha = if (category.isHidden) 0.5f else 1.0f
    val contentAlpha = if (category.isHidden) 0.4f else 1.0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (isDragging) it else it.animateContentSize()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
        ),
    ) {
        // ---- ヘッダー行 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // カテゴリ用ドラッグハンドル
            dragHandle()

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = category.nameJa,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
                Text(
                    text = if (category.isHidden) "${category.nameEn}（非表示）" else category.nameEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f * contentAlpha),
                )
            }

            if (!category.isDefault) {
                IconButton(onClick = onEditCategoryClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "編集",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = {
                    if (category.isDefault) onToggleCategoryVisibility()
                    else onDeleteCategoryClick()
                }
            ) {
                Icon(
                    imageVector = if (category.isDefault) {
                        if (category.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Delete
                    },
                    contentDescription = null,
                    tint = if (category.isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // ---- 単語一覧 ----
        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (words.isNotEmpty()) {
                    // ReorderableColumn (sh.calvin.reorderable 3.1.0)
                    ReorderableColumn(
                        list = words,
                        onSettle = { from, to ->
                            onReorderWords(from, to)
                            onSettleWords()
                        },
                    ) { _, word, _ ->
                        ReorderableItem {
                            WordEditRow(
                                word = word,
                                onEdit = { onEditWordClick(word) },
                                onDelete = { onDeleteWordClick(word) },
                                onToggleVisibility = { onToggleWordVisibility(word) },
                                dragHandle = {
                                    Icon(
                                        imageVector = Icons.Default.Reorder,
                                        contentDescription = "並び替え",
                                        modifier = Modifier.draggableHandle().padding(horizontal = 12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "単語がありません",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                TextButton(
                    onClick = onAddWordClick,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("単語を追加")
                }
            }
        }
    }
}

@Composable
private fun WordEditRow(
    word: PromptWordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleVisibility: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (word.isHidden) 0.4f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !word.isDefault) { onEdit() }
            .padding(top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dragHandle()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.wordEn,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            if (word.wordJa.isNotBlank()) {
                Text(
                    text = word.wordJa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f * alpha),
                )
            }
        }

        if (!word.isDefault) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        } else {
            IconButton(onClick = onToggleVisibility) {
                Icon(if (word.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
            }
        }
    }
}