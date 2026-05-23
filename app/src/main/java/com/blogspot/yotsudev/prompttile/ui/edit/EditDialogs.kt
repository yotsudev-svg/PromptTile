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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
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
        title = {
            Text(
                if (initial == null) stringResource(R.string.dialog_add_category_title)
                else stringResource(R.string.dialog_edit_category_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameJa,
                    onValueChange = { nameJa = it },
                    label = { Text(stringResource(R.string.dialog_category_name_ja)) },
                    placeholder = { Text(stringResource(R.string.dialog_category_name_ja_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(stringResource(R.string.dialog_category_name_en)) },
                    placeholder = { Text(stringResource(R.string.dialog_category_name_en_placeholder)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(nameJa.trim(), nameEn.trim()) },
                enabled = canSave,
            ) {
                Text(
                    if (initial == null) stringResource(R.string.dialog_add)
                    else stringResource(R.string.dialog_save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
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

    // 修正 #4: selectedCategoryId を Long? として rememberSaveable で保持する。
    //
    // 変更前は CategoryEntity をそのまま remember していたため、
    // rememberSaveableStateHolderNavEntryDecorator による状態保持が
    // 働いていても構成変更（画面回転など）でリセットされる可能性があった。
    //
    // CategoryEntity は Parcelable 非対応のため直接は saveable にできないが、
    // id（Long）は saveable なのでそちらを保持し、表示時に allCategories から
    // 逆引きする方式に変更した。これにより構成変更後も選択状態が正しく復元される。
    var selectedCategoryId by rememberSaveable {
        mutableStateOf(initial?.categoryId)
    }
    val selectedCategory = allCategories.find { it.id == selectedCategoryId }

    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val canSave = wordEn.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.dialog_add_word_title)
                else stringResource(R.string.dialog_edit_word_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wordEn,
                    onValueChange = { wordEn = it },
                    label = { Text(stringResource(R.string.dialog_word_en)) },
                    placeholder = { Text(stringResource(R.string.dialog_word_en_placeholder)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = wordJa,
                    onValueChange = { wordJa = it },
                    label = { Text(stringResource(R.string.dialog_word_ja)) },
                    placeholder = { Text(stringResource(R.string.dialog_word_ja_placeholder)) },
                    singleLine = true,
                )
                if (allCategories.isNotEmpty() && initial != null) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.nameJa
                                ?: stringResource(R.string.dialog_word_category_select),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.dialog_word_category)) },
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
                                        selectedCategoryId = category.id  // id のみ保存
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
                        selectedCategoryId,
                    )
                },
                enabled = canSave,
            ) {
                Text(
                    if (initial == null) stringResource(R.string.dialog_add)
                    else stringResource(R.string.dialog_save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}