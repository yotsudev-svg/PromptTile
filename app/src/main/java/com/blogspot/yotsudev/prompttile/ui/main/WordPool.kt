package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity

@Composable
fun WordPool(
    words: List<PromptWordEntity>,
    searchResults: List<PromptWordEntity>,
    searchQuery: String,
    selectedWordIds: Set<Long>,
    uncategorizedIds: Set<Long>,
    resolveToppingConfig: (String?) -> ToppingConfiguration,
    onWordTap: (PromptWordEntity) -> Unit,
    onWordLongPress: (PromptWordEntity) -> Unit,
    onToppingIconTap: (PromptWordEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (searchQuery.isNotBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.word_pool_search_results))
            }
            if (searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.word_pool_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(items = searchResults, key = { "search_${it.id}" }) { word ->
                    WordChipRouter(
                        word = word,
                        isSelected = word.id in selectedWordIds,
                        isUncategorized = word.categoryId in uncategorizedIds,
                        toppingGroupIds = resolveToppingConfig(word.tags).toppingGroupIds,
                        onTap = { onWordTap(word) },
                        onLongPress = { onWordLongPress(word) },
                        onToppingIconTap = { onToppingIconTap(word) },
                    )
                }
            }
        } else {
            items(items = words, key = { "cat_${it.id}" }) { word ->
                WordChipRouter(
                    word = word,
                    isSelected = word.id in selectedWordIds,
                    isUncategorized = word.categoryId in uncategorizedIds,
                    toppingGroupIds = resolveToppingConfig(word.tags).toppingGroupIds,
                    onTap = { onWordTap(word) },
                    onLongPress = { onWordLongPress(word) },
                    onToppingIconTap = { onToppingIconTap(word) },
                )
            }
        }
    }
}

/** トッピンググループの有無で通常チップ／分割チップを切り替えるルーター */
@Composable
private fun WordChipRouter(
    word: PromptWordEntity,
    isSelected: Boolean,
    isUncategorized: Boolean,
    toppingGroupIds: List<Long>,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToppingIconTap: () -> Unit,
) {
    if (toppingGroupIds.isNotEmpty()) {
        SplitWordChip(
            word = word,
            isSelected = isSelected,
            isUncategorized = isUncategorized,
            onTap = onTap,
            onLongPress = onLongPress,
            onToppingIconTap = onToppingIconTap,
        )
    } else {
        WordChip(
            word = word,
            isSelected = isSelected,
            isUncategorized = isUncategorized,
            onTap = onTap,
            onLongPress = onLongPress,
        )
    }
}

// ─── 通常チップ（既存デザイン）────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordChip(
    word: PromptWordEntity,
    isSelected: Boolean,
    isUncategorized: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f, // 押し感を少し弱めて上品に
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale",
    )
    val (containerColor, contentColor) = chipColors(isSelected, isUncategorized)

    Surface(
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) contentColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onTap,
                onLongClick = onLongPress,
            ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = word.wordEn,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = word.wordJa.ifBlank { stringResource(R.string.word_pool_no_label) },
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = if (isUncategorized && word.wordJa.isBlank()) 0.4f else 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── 分割スマート・アシストチップ ──────────────────────────────────────────────

/**
 * toppingGroupIds を持つ単語用の分割デザインチップ。
 *
 * レイアウト: [ A: テキストエリア（weight=1） | 縦線 | B: ⚙️アイコン（固定幅） ]
 *
 * - エリアA: 通常のタップ＆長押し操作（toggleWord / クリップボードコピー）
 * - エリアB: トッピング選択ミニボトムシートを開く
 *
 * Row に IntrinsicSize.Min を指定することで、縦の Divider が
 * 実際のコンテンツ高さに追従する。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SplitWordChip(
    word: PromptWordEntity,
    isSelected: Boolean,
    isUncategorized: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToppingIconTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSourceA = remember { MutableInteractionSource() }
    val isPressedA by interactionSourceA.collectIsPressedAsState()
    val interactionSourceB = remember { MutableInteractionSource() }
    val isPressedB by interactionSourceB.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressedA || isPressedB) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale",
    )
    val (containerColor, contentColor) = chipColors(isSelected, isUncategorized)
    val dividerColor = contentColor.copy(alpha = 0.12f)

    Surface(
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) contentColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // ---- エリアA: テキスト ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .combinedClickable(
                        interactionSource = interactionSourceA,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onTap,
                        onLongClick = onLongPress,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Column {
                    Text(
                        text = word.wordEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = word.wordJa.ifBlank { stringResource(R.string.word_pool_no_label) },
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ---- 縦区切り線 ----
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = dividerColor,
                thickness = 1.dp,
            )

            // ---- エリアB: ⚙️アイコン ----
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(40.dp) // 少し広げる
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .combinedClickable(
                        interactionSource = interactionSourceB,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onToppingIconTap,
                    )
                    .background(if (isPressedB) contentColor.copy(alpha = 0.08f) else Color.Transparent),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.word_chip_topping_icon_desc),
                    tint = if (isPressedB) contentColor else contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── 共通ヘルパー ─────────────────────────────────────────────────────────────

@Composable
private fun chipColors(isSelected: Boolean, isUncategorized: Boolean): Pair<Color, Color> {
    val container = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.tertiaryContainer
        isSelected                    -> MaterialTheme.colorScheme.primaryContainer
        else                          -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val content = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.onTertiaryContainer
        isSelected                    -> MaterialTheme.colorScheme.onPrimaryContainer
        else                          -> MaterialTheme.colorScheme.onSurface
    }
    return container to content
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
    )
}