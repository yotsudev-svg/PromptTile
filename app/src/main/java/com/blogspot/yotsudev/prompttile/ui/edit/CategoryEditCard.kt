package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog

@Composable
fun CategoryEditCard(
    category: CategoryEntity,
    isExpanded: Boolean,
    words: List<PromptWordEntity>,
    allCategories: List<CategoryEntity>,
    onToggleExpand: () -> Unit,
    onEditCategory: (nameJa: String, nameEn: String) -> Unit,
    onDeleteCategory: () -> Unit,
    onToggleCategoryVisibility: () -> Unit,
    onAddWord: (wordEn: String, wordJa: String) -> Unit,
    onEditWord: (PromptWordEntity, wordEn: String, wordJa: String, newCategoryId: Long?) -> Unit,
    onDeleteWord: (PromptWordEntity) -> Unit,
    onToggleWordVisibility: (PromptWordEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showAddWordDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<PromptWordEntity?>(null) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var deletingWord by remember { mutableStateOf<PromptWordEntity?>(null) }

    // 再描画を抑えるための色計算
    val containerAlpha = if (category.isHidden) 0.5f else 1.0f
    val contentAlpha = if (category.isHidden) 0.4f else 1.0f

    // ---- ダイアログ群 ----
    if (showEditCategoryDialog) {
        CategoryDialog(
            initial = category,
            onConfirm = { ja, en -> onEditCategory(ja, en); showEditCategoryDialog = false },
            onDismiss = { showEditCategoryDialog = false },
        )
    }
    if (showAddWordDialog) {
        WordDialog(
            onConfirm = { en, ja, _ -> onAddWord(en, ja); showAddWordDialog = false },
            onDismiss = { showAddWordDialog = false },
        )
    }
    editingWord?.let { word ->
        WordDialog(
            initial = word,
            allCategories = allCategories,
            onConfirm = { en, ja, newCategoryId ->
                onEditWord(word, en, ja, newCategoryId)
                editingWord = null
            },
            onDismiss = { editingWord = null },
        )
    }
    if (showDeleteCategoryDialog) {
        ConfirmDeleteDialog(
            targetName = category.nameJa,
            onConfirm = { onDeleteCategory(); showDeleteCategoryDialog = false },
            onDismiss = { showDeleteCategoryDialog = false },
        )
    }
    deletingWord?.let { word ->
        ConfirmDeleteDialog(
            targetName = word.wordEn,
            onConfirm = { onDeleteWord(word); deletingWord = null },
            onDismiss = { deletingWord = null },
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
        ),
    ) {
        // ---- ヘッダー行 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                IconButton(onClick = { showEditCategoryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "カテゴリを編集",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (category.isDefault) {
                IconButton(onClick = onToggleCategoryVisibility) {
                    Icon(
                        imageVector = if (category.isHidden)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (category.isHidden) "表示する" else "非表示にする",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = { showDeleteCategoryDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "カテゴリを削除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        // ---- 単語一覧（アコーディオン） ----
        AnimatedVisibility(visible = isExpanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (words.isEmpty()) {
                    Text(
                        text = "単語がありません",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else {
                    words.forEach { word ->
                        WordEditRow(
                            word = word,
                            onEdit = { editingWord = word },
                            onDelete = { deletingWord = word },
                            onToggleVisibility = { onToggleWordVisibility(word) },
                        )
                    }
                }
                TextButton(
                    onClick = { showAddWordDialog = true },
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 4.dp),
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
    modifier: Modifier = Modifier,
) {
    val alpha = if (word.isHidden) 0.4f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !word.isDefault) { onEdit() }
            .padding(start = 24.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "単語を編集",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (word.isDefault) {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (word.isHidden)
                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (word.isHidden) "表示する" else "非表示にする",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "単語を削除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
