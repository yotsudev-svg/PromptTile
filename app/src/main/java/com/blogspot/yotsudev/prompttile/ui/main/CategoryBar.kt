package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity

/**
 * エリアB: カテゴリ選択バー。
 *
 * LazyRowではなく Row + horizontalScroll を採用している理由:
 * カテゴリ数は多くても20件程度と想定されるため、
 * LazyRowの遅延初期化コストより、Rowのシンプルさを優先した。
 * カテゴリが50件を超えるようになったら LazyRow への切り替えを検討すること。
 */
@Composable
fun CategoryBar(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                label = {
                    Text(
                        text = category.nameJa,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}