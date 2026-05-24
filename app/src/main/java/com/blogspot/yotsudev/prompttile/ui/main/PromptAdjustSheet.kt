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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity

/** 重み選択肢の定義。(labelRes, weight値) */
private val WEIGHT_OPTIONS = listOf(
    R.string.adjust_weight_none  to null,
    R.string.adjust_weight_1_2   to 1.2f,
    R.string.adjust_weight_1_5   to 1.5f,
    R.string.adjust_weight_0_8   to 0.8f,
)

/**
 * プロンプトエリアのチップをタップすると表示される調整コックピット。
 *
 * 機能:
 * 1. 重みの一発変更（なし / 1.2 / 1.5 / 0.8）
 * 2. トッピングの変更（対応単語のみ動的表示）
 * 3. 単語の削除
 *
 * [item] が null のときシートは非表示になる（呼び出し元で制御）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptAdjustSheet(
    item: PromptItem,
    toppingItems: List<ToppingItemEntity>,
    onWeightSelect: (Float?) -> Unit,
    onToppingSelect: (String?) -> Unit,
    onDelete: () -> Unit,
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

            // ---- ヘッダー: 単語名 + 削除ボタン ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = item.baseText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (item.wordJa.isNotBlank()) {
                        Text(
                            text = item.wordJa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(
                    onClick = { onDelete(); onDismiss() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.adjust_delete))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // ---- セクション1: 重みの一発変更 ----
            Text(
                text = stringResource(R.string.adjust_section_weight),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WEIGHT_OPTIONS.forEach { (labelRes, weight) ->
                    val isSelected = item.weight == weight ||
                            (weight == null && (item.weight == null || item.weight == 1.0f))
                    WeightButton(
                        label = stringResource(labelRes),
                        isSelected = isSelected,
                        onClick = { onWeightSelect(weight) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ---- セクション2: トッピング変更（対応単語のみ） ----
            if (toppingItems.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.adjust_section_topping),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    // 「なし」チップ
                    item(key = "topping_none") {
                        ToppingSelectChip(
                            nameJa = stringResource(R.string.adjust_topping_none),
                            colorHex = null,
                            isSelected = item.selectedTopping == null,
                            onClick = { onToppingSelect(null) },
                        )
                    }
                    items(items = toppingItems, key = { it.id }) { toppingItem ->
                        ToppingSelectChip(
                            nameJa = toppingItem.nameJa,
                            colorHex = toppingItem.colorHex,
                            isSelected = item.selectedTopping == toppingItem.valueEn,
                            onClick = { onToppingSelect(toppingItem.valueEn) },
                        )
                    }
                }
            }
        }
    }
}

// ─── WeightButton ─────────────────────────────────────────────────────────────

@Composable
private fun WeightButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ─── ToppingSelectChip（調整シート内） ────────────────────────────────────────

@Composable
private fun ToppingSelectChip(
    nameJa: String,
    colorHex: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val parsedColor = remember(colorHex) {
        colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    }
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                ) else Modifier
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (parsedColor != null) {
                val needsBorder = parsedColor.luminance() > 0.8f
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .then(
                            if (needsBorder) Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ) else Modifier
                        ),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = nameJa,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
    }
}