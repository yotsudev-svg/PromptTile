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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import kotlin.math.roundToInt

/**
 * プロンプトエリアのチップをタップすると表示される調整コックピット。
 *
 * 改善後の機能:
 * 1. 強調度（重み）のハイブリッド調整（スライダー 0.1刻み / ボタン 0.05刻み）
 * 2. トッピングの変更（対応単語のみ動的表示）
 * 3. 単語の削除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptAdjustSheet(
    item: PromptItem,
    toppingGroups: List<ToppingGroupWithItems>,
    onWeightSelect: (Float?) -> Unit,
    onToppingSelect: (groupId: Long, topping: String?, isPrefix: Boolean, slot: String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // 現在の重み（null は 1.0f として扱う）
    val currentWeight = item.weight ?: 1.0f

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
            Spacer(Modifier.height(12.dp))

            // ---- セクション1: 強調度のハイブリッド調整 ----
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.adjust_section_weight),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = String.format("%.2f", currentWeight),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // マイナスボタン (0.05刻み)
                    IconButton(
                        onClick = {
                            val next = (currentWeight - 0.05f).coerceIn(0.2f, 1.8f)
                            onWeightSelect(if (next == 1.0f) null else next)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    // スライダー (滑らかに見えて 0.1 刻みにスナップ、中央 1.0)
                    Slider(
                        value = currentWeight,
                        onValueChange = { 
                            val next = (it * 10f).roundToInt() / 10f // 0.1刻みにスナップ
                            onWeightSelect(if (next == 1.0f) null else next)
                        },
                        valueRange = 0.2f..1.8f,
                        modifier = Modifier.weight(1f)
                    )

                    // プラスボタン (0.05刻み)
                    IconButton(
                        onClick = {
                            val next = (currentWeight + 0.05f).coerceIn(0.2f, 1.8f)
                            onWeightSelect(if (next == 1.0f) null else next)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            // ---- セクション2: トッピング変更（対応単語のみ） ----
            val tags = remember(item.tags) { item.tags?.split(",")?.map { it.trim() } ?: emptyList() }
            val isMultiColor = tags.contains("hair_multicolor") || tags.contains("eye_multicolor")
            val hasColorB = item.promptTemplate?.contains("{colorB}") == true

            toppingGroups.forEach { groupWithItems ->
                val group = groupWithItems.group
                val toppingItems = groupWithItems.items
                
                val filteredItems = remember(toppingItems, item.excludeToppingValues) {
                    val excludedSet = item.excludeToppingValues.toSet()
                    toppingItems.filter { it.valueEn !in excludedSet }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))

                if (isMultiColor && hasColorB && group.nameEn.lowercase().contains("color")) {
                    // ダブルカラー調整
                    Text(
                        text = "ベース色 (Color A)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        items(items = filteredItems, key = { "A_${it.id}" }) { toppingItem ->
                            val isSelected = item.selectedToppings.any { it.slot == "colorA" && it.valueEn == toppingItem.valueEn }
                            ToppingSelectChip(
                                nameJa = toppingItem.nameJa,
                                colorHex = toppingItem.colorHex,
                                isSelected = isSelected,
                                onClick = { onToppingSelect(group.id, toppingItem.valueEn, group.isPrefix, "colorA") },
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "差し色 (Color B)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        items(items = filteredItems, key = { "B_${it.id}" }) { toppingItem ->
                            val isSelected = item.selectedToppings.any { it.slot == "colorB" && it.valueEn == toppingItem.valueEn }
                            ToppingSelectChip(
                                nameJa = toppingItem.nameJa,
                                colorHex = toppingItem.colorHex,
                                isSelected = isSelected,
                                onClick = { onToppingSelect(group.id, toppingItem.valueEn, group.isPrefix, "colorB") },
                            )
                        }
                    }
                } else {
                    // 標準トッピング調整
                    Text(
                        text = group.nameJa,
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
                        item(key = "topping_none_${group.id}") {
                            val isSelected = item.selectedToppings.none { it.groupId == group.id }
                            ToppingSelectChip(
                                nameJa = stringResource(R.string.adjust_topping_none),
                                colorHex = null,
                                isSelected = isSelected,
                                onClick = { onToppingSelect(group.id, null, group.isPrefix, if (isMultiColor) "colorA" else null) },
                            )
                        }
                        items(items = filteredItems, key = { it.id }) { toppingItem ->
                            val isSelected = item.selectedToppings.any {
                                if (isMultiColor) it.slot == "colorA" && it.valueEn == toppingItem.valueEn
                                else it.groupId == group.id && it.valueEn == toppingItem.valueEn
                            }
                            ToppingSelectChip(
                                nameJa = toppingItem.nameJa,
                                colorHex = toppingItem.colorHex,
                                isSelected = isSelected,
                                onClick = { onToppingSelect(group.id, toppingItem.valueEn, group.isPrefix, if (isMultiColor) "colorA" else null) },
                            )
                        }
                    }
                }
            }
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
        Color.Transparent

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = if (isSelected) {
            null
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
