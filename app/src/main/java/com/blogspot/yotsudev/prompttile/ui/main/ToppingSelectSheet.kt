package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity

/**
 * 分割チップの🎨タップで表示されるミニボトムシート。
 *
 * トッピングを選択すると即座に単語をプロンプトに追加してシートを閉じる。
 * 「選択なしで追加」ボタンでトッピングなしの追加もできる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToppingSelectSheet(
    wordEn: String,
    excludeToppingValues: Set<String>,
    toppingGroups: List<ToppingGroupWithItems>,
    onSelect: (groupId: Long, topping: String?, isPrefix: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // ---- ヘッダー ----
            Text(
                text = stringResource(R.string.topping_sheet_title, wordEn),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ---- トッピンググループ一覧 ----
            toppingGroups.forEach { groupWithItems ->
                val group = groupWithItems.group
                val toppingItems = groupWithItems.items
                val filteredItems = remember(toppingItems, excludeToppingValues) {
                    toppingItems.filter { it.valueEn !in excludeToppingValues }
                }

                if (filteredItems.isNotEmpty()) {
                    // グループ名
                    Text(
                        text = group.nameJa,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )

                    // トッピングチップ一覧（横スクロール）
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        items(items = filteredItems, key = { it.id }) { item ->
                            ToppingChip(
                                item = item,
                                onClick = {
                                    onSelect(group.id, item.valueEn, group.isPrefix)
                                    // 複数ある場合でも、一個選んだら閉じる（追加動作なので）
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }

            // ---- 「選択なしで追加」ボタン ----
            // 最初のグループの情報を代表として使う（ViewModel側で toppings=null なら他は無視される想定）
            val firstGroupId = toppingGroups.firstOrNull()?.group?.id
            val firstGroupIsPrefix = toppingGroups.firstOrNull()?.group?.isPrefix ?: true

            OutlinedButton(
                onClick = {
                    if (firstGroupId != null) {
                        onSelect(firstGroupId, null, firstGroupIsPrefix)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.topping_sheet_add_without))
            }
        }
    }
}

/**
 * トッピング選択用のカラーチップ。
 *
 * colorHex が指定されている場合はカラードットを表示する。
 * null の場合（素材系）はドットなしでテキストのみ表示。
 */
@Composable
private fun ToppingChip(
    item: ToppingItemEntity,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val parsedColor = remember(item.colorHex) {
        item.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    }

    // PromptAdjustSheet のスタイルに合わせる（選択: FilledTonal, 未選択: Outlined）
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        Color.Transparent

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = if (isSelected) {
            null // 選択時は枠線なし
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        },
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // ... (color dot logic remains the same)
            if (parsedColor != null) {
                val isLight = parsedColor.luminance() > 0.8f
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .then(
                            if (isLight) Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ) else Modifier
                        ),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = item.nameJa,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
    }
}
