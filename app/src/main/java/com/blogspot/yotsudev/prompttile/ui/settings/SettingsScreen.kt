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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.data.seed.PrefixTemplate
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    promptViewModel: PromptViewModel,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // 削除確認ダイアログ用: 削除対象テンプレートを保持する
    var deletingTemplate by rememberSaveable { mutableStateOf<PrefixTemplate?>(null) }

    // 削除確認ダイアログ（ボトムシートの外に置くことで確実に表示される）
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

    if (showSheet) {
        PrefixTemplateBottomSheet(
            templates = allTemplates,
            onSelect = { template ->
                promptViewModel.addTemplateItems(template.text)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                }
            },
            onDelete = { template ->
                // 削除ボタンタップ → 確認ダイアログを表示
                deletingTemplate = template
            },
            onAddNew = { showAddDialog = true },
            onDismiss = { showSheet = false },
            sheetState = sheetState,
        )
    }

    if (showAddDialog) {
        AddTemplateDialog(
            onConfirm = { name, text ->
                viewModel.addUserTemplate(name, text)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    val isSystemDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            key(prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM, isSystemDark) {
                TopAppBar(title = { Text("設定") })
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
            // ================================================================
            // セクション0: UI設定 (追加)
            // ================================================================
            SettingsSectionHeader(title = "外観")
            ThemeSelectionRow(
                title = "テーマ",
                description = "アプリの配色（ライト・ダーク）を設定します",
                // prefs が null の場合は FOLLOW_SYSTEM を表示
                currentTheme = prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM,
                onThemeSelected = viewModel::updateThemeConfig
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ================================================================
            // セクション1: プロンプト
            // ================================================================
            SettingsSectionHeader(title = "プロンプト")

            TemplateButtonRow(onOpenTemplates = { showSheet = true })

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ================================================================
            // セクション2: 操作
            // ================================================================
            SettingsSectionHeader(title = "操作")

            SwitchSettingRow(
                title = "コピー後にバックグラウンドへ",
                description = "コピーボタンを押したあと自動でアプリを背面に移動します",
                // prefs が null の場合は false をデフォルトにする
                checked = prefs?.moveToBackOnCopy ?: false,
                onCheckedChange = viewModel::updateMoveToBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionRow(
    title: String,
    description: String,
    currentTheme: ThemeConfig,
    onThemeSelected: (ThemeConfig) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val themeLabel = when (currentTheme) {
        ThemeConfig.FOLLOW_SYSTEM -> "システム設定"
        ThemeConfig.LIGHT -> "ライトモード"
        ThemeConfig.DARK -> "ダークモード"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 左側：タイトルと説明
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

        // 右側：ドロップダウンメニュー
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
                        ThemeConfig.FOLLOW_SYSTEM -> "システム設定に従う"
                        ThemeConfig.LIGHT -> "ライトモード"
                        ThemeConfig.DARK -> "ダークモード"
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
}

@Composable
private fun TemplateButtonRow(onOpenTemplates: () -> Unit) {
    Row(
        modifier = Modifier
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
            Text(
                text = "クオリティテンプレート",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "テンプレートの単語をメイン画面のプロンプトに追加します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpenTemplates) {
            Text("選ぶ")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrefixTemplateBottomSheet(
    templates: List<PrefixTemplate>,
    onSelect: (PrefixTemplate) -> Unit,
    onDelete: (PrefixTemplate) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "テンプレートを選ぶ",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onAddNew) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "テンプレートを追加",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider()

            if (templates.isEmpty()) {
                Text(
                    text = "テンプレートがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            templates.forEach { template ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
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
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onSelect(template) },
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(8.dp))
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
        title = { Text("テンプレートを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("テンプレート名") },
                    placeholder = { Text("例: 自分用クオリティ") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("テンプレート内容") },
                    placeholder = { Text("例: masterpiece, best quality") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(name.trim(), text.trim()) },
                enabled = canSave,
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
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
    Row(
        modifier = Modifier
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}