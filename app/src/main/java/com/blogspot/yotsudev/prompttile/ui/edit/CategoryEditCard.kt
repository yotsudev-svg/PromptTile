package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity

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
    val contentAlpha = if (category.isHidden) 0.45f else 1.0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = if (isDragging) Modifier else Modifier.animateContentSize()
        ) {
            // ---- ヘッダー行 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(start = 8.dp, top = 10.dp, bottom = 10.dp, end = 10.dp)
                    .alpha(contentAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dragHandle()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                ) {
                    // カテゴリ名 + DEFAULTバッジ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = category.nameJa,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (category.isDefault || category.isSystem) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                            ) {
                                Text(
                                    text = "DEFAULT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    // 英語名 + 非表示ラベル
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = category.nameEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        if (category.isHidden) {
                            Text(
                                text = "· 非表示",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                // アクションボタン群
                if (!category.isDefault && !category.isSystem) {
                    IconButton(
                        onClick = onEditCategoryClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編集",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDeleteCategoryClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleCategoryVisibility,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (category.isHidden) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (category.isHidden) "非表示" else "表示",
                            tint = if (category.isHidden)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // ---- 単語一覧（展開時） ----
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.alpha(contentAlpha)) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    )

                    if (words.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            words.forEach { word ->
                                WordEditRow(
                                    word = word,
                                    categoryName = null,
                                    onEdit = { onEditWordClick(word) },
                                    onDelete = { onDeleteWordClick(word) },
                                    onToggleVisibility = { onToggleWordVisibility(word) },
                                    dragHandle = {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore, // Reorder icon
                                            contentDescription = "並び替え",
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(horizontal = 2.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.35f),
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

                    // ---- 単語を追加ボタン（破線風ボーダー） ----
                    if (contentAlpha > 0.5f) {
                        Surface(
                            onClick = onAddWordClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "単語を追加",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordEditRow(
    word: PromptWordEntity,
    categoryName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleVisibility: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (word.isHidden) 0.45f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !word.isDefault && !word.isHidden) { onEdit() }
            .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 5.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dragHandle()

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = word.wordEn,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (categoryName != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            // 日本語名 + 非表示ラベル
            if (word.wordJa.isNotBlank() || word.isHidden) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (word.wordJa.isNotBlank()) {
                        Text(
                            text = word.wordJa,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    if (word.isHidden) {
                        Text(
                            text = "· 非表示",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }

        // アクションボタン
        if (!word.isDefault) {
            IconButton(
                onClick = onEdit,
                enabled = !word.isHidden,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "編集",
                    tint = if (word.isHidden)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp),
                )
            }
        } else {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = if (word.isHidden) Icons.Default.VisibilityOff
                    else Icons.Default.Visibility,
                    contentDescription = if (word.isHidden) "非表示" else "表示",
                    tint = if (word.isHidden)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}