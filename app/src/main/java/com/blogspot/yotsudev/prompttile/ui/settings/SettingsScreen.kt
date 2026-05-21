package com.blogspot.yotsudev.prompttile.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.data.seed.PrefixTemplate
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel
import kotlinx.coroutines.launch

/**
 * 設定画面。
 *
 * パフォーマンス最適化（LazyColumn の採用）と
 * UI ロジックの整理（BottomSheet のカプセル化）を行っています。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    promptViewModel: PromptViewModel,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var deletingTemplate by rememberSaveable { mutableStateOf<PrefixTemplate?>(null) }
    val isSystemDark = isSystemInDarkTheme()

    // ---- 削除確認ダイアログ ----
    deletingTemplate?.let { template ->
        ConfirmDeleteDialog(
            targetName = template.name,
            onConfirm = {
                viewModel.removeUserTemplate(template.name, template.text)
                deletingTemplate = null
            },
            onDismiss = { deletingTemplate = null },
        )
    }

    // ---- テンプレート選択ボトムシート ----
    if (showSheet) {
        PrefixTemplateBottomSheet(
            templates = allTemplates,
            sheetState = sheetState,
            onSelect = { template ->
                promptViewModel.addTemplateItems(template.text)
                // アニメーション完了後にフラグを下ろす
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showSheet = false
                }
            },
            onDelete = { deletingTemplate = it },
            onAddNew = { showAddDialog = true },
            onDismiss = { showSheet = false },
        )
    }

    // ---- テンプレート追加ダイアログ ----
    if (showAddDialog) {
        AddTemplateDialog(
            onConfirm = { name, text ->
                viewModel.addUserTemplate(name, text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }


    Scaffold(
        topBar = {
            key(prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM, isSystemDark) {
                TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsSectionHeader(title = stringResource(R.string.settings_header_appearance))
            ThemeSelectionRow(
                currentTheme = prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM,
                onThemeSelected = viewModel::updateThemeConfig
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader(title = stringResource(R.string.settings_header_prompt))
            SettingsRow(
                title = stringResource(R.string.settings_template_title),
                description = stringResource(R.string.settings_template_description),
                trailingContent = {
                    TextButton(onClick = { showSheet = true }) {
                        Text(stringResource(R.string.settings_template_select))
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader(title = stringResource(R.string.settings_header_operation))
            SwitchSettingRow(
                title = stringResource(R.string.settings_move_to_back_title),
                description = stringResource(R.string.settings_move_to_back_description),
                checked = prefs?.moveToBackOnCopy ?: false,
                onCheckedChange = viewModel::updateMoveToBack,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingContent?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionRow(
    currentTheme: ThemeConfig,
    onThemeSelected: (ThemeConfig) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val themeLabel = when (currentTheme) {
        ThemeConfig.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeConfig.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeConfig.DARK -> stringResource(R.string.settings_theme_dark)
    }

    SettingsRow(
        title = stringResource(R.string.settings_theme_title),
        description = stringResource(R.string.settings_theme_description),
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextButton(
                    onClick = { },
                    modifier = Modifier.menuAnchor()
                ) {
                    Text(themeLabel)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ThemeConfig.entries.forEach { config ->
                        val label = when (config) {
                            ThemeConfig.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system_full)
                            ThemeConfig.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeConfig.DARK -> stringResource(R.string.settings_theme_dark)
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onThemeSelected(config)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrefixTemplateBottomSheet(
    templates: List<PrefixTemplate>,
    sheetState: SheetState,
    onSelect: (PrefixTemplate) -> Unit,
    onDelete: (PrefixTemplate) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.template_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = onAddNew) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.template_sheet_add_description),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
            }

            if (templates.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.template_sheet_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(
                    items = templates,
                    key = { "${it.name}_${it.text}" }
                ) { template ->
                    ListItem(
                        headlineContent = {
                            Text(text = template.name, style = MaterialTheme.typography.bodyLarge)
                        },
                        supportingContent = {
                            Text(
                                text = template.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            if (!template.isDefault) {
                                IconButton(onClick = { onDelete(template) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.template_delete_description),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onSelect(template) },
                    )
                    HorizontalDivider()
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AddTemplateDialog(
    onConfirm: (name: String, text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val canSave = name.isNotBlank() && text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_template_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_add_template_name_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_add_template_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.dialog_add_template_text_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_add_template_text_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(name.trim(), text.trim()) },
                enabled = canSave,
            ) { Text(stringResource(R.string.dialog_add_template_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        title = title,
        description = description,
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            role = Role.Switch
        ),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // toggleable側でハンドリング
            )
        }
    )
}
