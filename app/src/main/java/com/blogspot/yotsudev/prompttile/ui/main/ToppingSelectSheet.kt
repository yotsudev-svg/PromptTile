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
    word: PromptWordEntity,
    toppingItems: List<ToppingItemEntity>,
    onSelect: (topping: String?) -> Unit,
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
                text = stringResource(R.string.topping_sheet_title, word.wordEn),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ---- トッピングチップ一覧（横スクロール） ----
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                items(items = toppingItems, key = { it.id }) { item ->
                    ToppingChip(
                        item = item,
                        onClick = {
                            onSelect(item.valueEn)
                            onDismiss()
                        },
                    )
                }
            }

            // ---- 「選択なしで追加」ボタン ----
            OutlinedButton(
                onClick = {
                    onSelect(null)
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
    onClick: () -> Unit,
) {
    val parsedColor = remember(item.colorHex) {
        item.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // カラードット（colorHex がある場合のみ表示）
            if (parsedColor != null) {
                val isDark = parsedColor == Color.White || parsedColor.luminance() > 0.8f
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .then(
                            // 白など明るい色はボーダーで視認性を確保
                            if (isDark) Modifier.border(
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
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}