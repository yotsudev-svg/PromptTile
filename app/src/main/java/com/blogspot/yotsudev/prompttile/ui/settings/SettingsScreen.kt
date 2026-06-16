package com.blogspot.yotsudev.prompttile.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.blogspot.yotsudev.prompttile.ui.theme.PromptTileTheme
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showClearHistoryConfirm) {
        StyledDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = stringResource(R.string.main_history_clear_confirm_title),
            icon = Icons.Default.Delete,
            iconColor = MaterialTheme.colorScheme.error,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryConfirm = false
                }) {
                    Text(
                        text = stringResource(R.string.dialog_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            Text(
                text = stringResource(R.string.main_history_clear_confirm_msg),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ファイル選択ランチャー (復元)
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadJsonFromUri(it) }
    }

    // ファイル保存ランチャー (バックアップ)
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToJsonUri(it) }
    }

    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            key(prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM, isSystemDark) {
                PromptTileTopAppBar(
                    title = stringResource(R.string.settings_title)
                )
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

            SettingsSectionHeader(title = stringResource(R.string.settings_header_operation))
            SwitchSettingRow(
                title = stringResource(R.string.settings_move_to_back_title),
                description = stringResource(R.string.settings_move_to_back_description),
                checked = prefs?.moveToBackOnCopy ?: false,
                onCheckedChange = viewModel::updateMoveToBack,
            )
            GridColumnsSelectionRow(
                currentConfig = prefs?.gridColumnsConfig ?: com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.AUTO,
                onConfigSelected = viewModel::updateGridColumnsConfig
            )

            SettingsSectionHeader(title = stringResource(R.string.settings_header_history))
            HistoryLimitRow(
                currentLimit = prefs?.maxHistoryCount ?: 50,
                onLimitSelected = viewModel::updateMaxHistoryCount
            )
            ClearHistoryRow(
                onClear = { showClearHistoryConfirm = true }
            )

            SettingsSectionHeader(title = stringResource(R.string.settings_header_backup))
            BackupOperationRow(
                onExport = { createFileLauncher.launch("prompt_backup.json") },
                onImport = { pickFileLauncher.launch("application/json") },
                isProcessing = isProcessing
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent, // 背景はScaffoldの色に任せる
        ),
        modifier = modifier.fillMaxWidth()
    )
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

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium, // 少し小さめに
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
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
                onCheckedChange = null,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridColumnsSelectionRow(
    currentConfig: com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig,
    onConfigSelected: (com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig) -> Unit
) {
    SettingsRow(
        title = stringResource(R.string.settings_grid_columns_title),
        description = stringResource(R.string.settings_grid_columns_description),
        trailingContent = {
            SingleChoiceSegmentedButtonRow {
                com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.entries.forEachIndexed { index, config ->
                    val label = when (config) {
                        com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.AUTO -> stringResource(R.string.settings_grid_columns_auto)
                        com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.FIXED_2 -> stringResource(R.string.settings_grid_columns_2)
                        com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.FIXED_3 -> stringResource(R.string.settings_grid_columns_3)
                    }
                    SegmentedButton(
                        selected = currentConfig == config,
                        onClick = { onConfigSelected(config) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig.entries.size
                        ),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PromptTileTheme {
        SettingsScreen()
    }
}

@Composable
private fun BackupOperationRow(
    onExport: () -> Unit,
    onImport: () -> Unit,
    isProcessing: Boolean
) {
    Column {
        SettingsRow(
            title = stringResource(R.string.settings_backup_title),
            description = stringResource(R.string.settings_backup_description),
            trailingContent = {
                TextButton(
                    onClick = onExport,
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_backup_export))
                }
            }
        )
        SettingsRow(
            title = stringResource(R.string.settings_restore_title),
            description = stringResource(R.string.settings_restore_description),
            trailingContent = {
                TextButton(
                    onClick = onImport,
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_backup_restore))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryLimitRow(
    currentLimit: Int,
    onLimitSelected: (Int) -> Unit
) {
    SettingsRow(
        title = stringResource(R.string.settings_history_limit_title),
        description = stringResource(R.string.settings_history_limit_description),
        trailingContent = {
            SingleChoiceSegmentedButtonRow {
                val limits = listOf(50, 100, 0)
                limits.forEachIndexed { index, limit ->
                    val label = when (limit) {
                        50 -> stringResource(R.string.settings_history_limit_50)
                        100 -> stringResource(R.string.settings_history_limit_100)
                        else -> stringResource(R.string.settings_history_limit_unlimited)
                    }
                    SegmentedButton(
                        selected = currentLimit == limit,
                        onClick = { onLimitSelected(limit) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = limits.size
                        ),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    )
}

@Composable
private fun ClearHistoryRow(
    onClear: () -> Unit
) {
    SettingsRow(
        title = stringResource(R.string.settings_history_clear_title),
        description = stringResource(R.string.settings_history_clear_description),
        trailingContent = {
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.settings_history_clear_button))
            }
        }
    )
}
