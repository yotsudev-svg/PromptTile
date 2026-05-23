package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onWordTap: (PromptWordEntity) -> Unit,
    onWordLongPress: (PromptWordEntity) -> Unit,
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
            // ---- 検索結果セクション ----
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.word_pool_search_results))
            }
            if (searchResults.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.word_pool_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(items = searchResults, key = { "search_${it.id}" }) { word ->
                    WordChip(
                        word = word,
                        isSelected = word.id in selectedWordIds,
                        isUncategorized = word.categoryId in uncategorizedIds,
                        onTap = { onWordTap(word) },
                        onLongPress = { onWordLongPress(word) },
                    )
                }
            }
        } else {
            // ---- 通常表示 (カテゴリ単語) ----
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.word_pool_category_words))
            }
            items(items = words, key = { "cat_${it.id}" }) { word ->
                WordChip(
                    word = word,
                    isSelected = word.id in selectedWordIds,
                    isUncategorized = word.categoryId in uncategorizedIds,
                    onTap = { onWordTap(word) },
                    onLongPress = { onWordLongPress(word) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    )
}


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

    // 押した時に少し小さくなるアニメーション（「ぷにっ」とした弾力感）
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.5f, // 低めに設定して弾力感を出す
            stiffness = 300f     // ややゆったりとした動き
        ),
        label = "scale"
    )

    val containerColor = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.tertiaryContainer
        isUncategorized               -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        isSelected                    -> MaterialTheme.colorScheme.primaryContainer
        else                          -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val contentColor = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.onTertiaryContainer
        isUncategorized               -> MaterialTheme.colorScheme.onSecondaryContainer
        isSelected                    -> MaterialTheme.colorScheme.onPrimaryContainer
        else                          -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 1.dp, // 選択時は少し浮かす
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        shape = RoundedCornerShape(12.dp), // 少し丸みを強くしてモダンに
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // デフォルトの波紋を消してスケールアニメを主役にする
                onClick = onTap,
                onLongClick = onLongPress,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
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
                color = contentColor.copy(
                    alpha = if (isUncategorized && word.wordJa.isBlank()) 0.4f else 0.7f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}