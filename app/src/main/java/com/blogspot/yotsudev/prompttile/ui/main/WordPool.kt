package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity

@Composable
fun WordPool(
    words: List<PromptWordEntity>,
    selectedWordIds: Set<Long>,
    uncategorizedIds: Set<Long>,          // 追加: MainScreenから渡される
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
        items(
            items = words,
            key = { it.id },
        ) { word ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordChip(
    word: PromptWordEntity,
    isSelected: Boolean,
    isUncategorized: Boolean,             // 追加: 呼び出し元で解決済みのフラグを受け取る
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.tertiaryContainer
        isUncategorized               -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        isSelected                    -> MaterialTheme.colorScheme.primaryContainer
        else                          -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isSelected && isUncategorized -> MaterialTheme.colorScheme.onTertiaryContainer
        isUncategorized               -> MaterialTheme.colorScheme.onSecondaryContainer
        isSelected                    -> MaterialTheme.colorScheme.onPrimaryContainer
        else                          -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = containerColor,
        tonalElevation = if (isSelected) 0.dp else 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
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