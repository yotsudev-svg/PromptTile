package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog

@Composable
fun CategoryDialog(
    initial: CategoryEntity? = null,
    onConfirm: (nameJa: String, nameEn: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameJa by rememberSaveable { mutableStateOf(initial?.nameJa ?: "") }
    var nameEn by rememberSaveable { mutableStateOf(initial?.nameEn ?: "") }
    val canSave = nameJa.isNotBlank() && nameEn.isNotBlank()

    StyledDialog(
        onDismissRequest = onDismiss,
        title = if (initial == null) stringResource(R.string.dialog_add_category_title)
        else stringResource(R.string.dialog_edit_category_title),
        icon = Icons.Default.Category,
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(nameJa.trim(), nameEn.trim()) },
                enabled = canSave,
            ) {
                Text(
                    text = if (initial == null) stringResource(R.string.dialog_add)
                    else stringResource(R.string.dialog_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        OutlinedTextField(
            value = nameJa,
            onValueChange = { nameJa = it },
            label = { Text(stringResource(R.string.dialog_category_name_ja)) },
            placeholder = { Text(stringResource(R.string.dialog_category_name_ja_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = nameEn,
            onValueChange = { nameEn = it },
            label = { Text(stringResource(R.string.dialog_category_name_en)) },
            placeholder = { Text(stringResource(R.string.dialog_category_name_en_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
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

    var selectedCategoryId by rememberSaveable {
        mutableStateOf(initial?.categoryId)
    }
    val selectedCategory = allCategories.find { it.id == selectedCategoryId }

    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val canSave = wordEn.isNotBlank()

    StyledDialog(
        onDismissRequest = onDismiss,
        title = if (initial == null) stringResource(R.string.dialog_add_word_title)
        else stringResource(R.string.dialog_edit_word_title),
        icon = Icons.Default.Translate,
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
                    text = if (initial == null) stringResource(R.string.dialog_add)
                    else stringResource(R.string.dialog_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        OutlinedTextField(
            value = wordEn,
            onValueChange = { wordEn = it },
            label = { Text(stringResource(R.string.dialog_word_en)) },
            placeholder = { Text(stringResource(R.string.dialog_word_en_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = wordJa,
            onValueChange = { wordJa = it },
            label = { Text(stringResource(R.string.dialog_word_ja)) },
            placeholder = { Text(stringResource(R.string.dialog_word_ja_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
                                selectedCategoryId = category.id
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
