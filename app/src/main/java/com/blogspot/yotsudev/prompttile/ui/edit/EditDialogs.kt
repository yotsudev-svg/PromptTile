package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity

@Composable
fun CategoryDialog(
    initial: CategoryEntity? = null,
    onConfirm: (nameJa: String, nameEn: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameJa by rememberSaveable { mutableStateOf(initial?.nameJa ?: "") }
    var nameEn by rememberSaveable { mutableStateOf(initial?.nameEn ?: "") }
    val canSave = nameJa.isNotBlank() && nameEn.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "カテゴリを追加" else "カテゴリを編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameJa,
                    onValueChange = { nameJa = it },
                    label = { Text("日本語名") },
                    placeholder = { Text("例: 人物") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("英語名") },
                    placeholder = { Text("例: Character") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(nameJa.trim(), nameEn.trim()) },
                enabled = canSave,
            ) { Text(if (initial == null) "追加" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDialog(
    initial: PromptWordEntity? = null,
    allCategories: List<CategoryEntity> = emptyList(),
    onConfirm: (wordEn: String, wordJa: String, categoryId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var wordEn by rememberSaveable { mutableStateOf(initial?.wordEn ?: "") }
    var wordJa by rememberSaveable { mutableStateOf(initial?.wordJa ?: "") }

    /**
     * CategoryEntity は Parcelable 非対応のため rememberSaveable ではなく
     * remember を使う。
     * ダイアログは画面回転で再生成されるが、その際は onDismiss が呼ばれて
     * 閉じられるため、saveable にする必要はない。
     */
    var selectedCategory by remember {
        mutableStateOf(allCategories.find { it.id == initial?.categoryId })
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val canSave = wordEn.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "単語を追加" else "単語を編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wordEn,
                    onValueChange = { wordEn = it },
                    label = { Text("英語") },
                    placeholder = { Text("例: blue hair") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = wordJa,
                    onValueChange = { wordJa = it },
                    label = { Text("日本語（任意）") },
                    placeholder = { Text("例: 青髪") },
                    singleLine = true,
                )
                if (allCategories.isNotEmpty() && initial != null) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.nameJa ?: "カテゴリを選択",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("カテゴリ") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                        ) {
                            allCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text("${category.nameJa}  ${category.nameEn}") },
                                    onClick = {
                                        selectedCategory = category
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSave) onConfirm(
                        wordEn.trim(),
                        wordJa.trim(),
                        selectedCategory?.id,
                    )
                },
                enabled = canSave,
            ) { Text(if (initial == null) "追加" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}