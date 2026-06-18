package com.blogspot.yotsudev.prompttile.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.GridColumnsConfig
import com.blogspot.yotsudev.prompttile.data.preferences.StartupBehavior
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog
import com.blogspot.yotsudev.prompttile.ui.theme.PromptTileTheme

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
                        fontWeight = FontWeight.Bold,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.loadJsonFromUri(it) } }

    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToJsonUri(it) } }

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
                PromptTileTopAppBar(title = stringResource(R.string.settings_title))
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
            // ---- 外観 ----
            SettingsSectionHeader(stringResource(R.string.settings_header_appearance))
            SettingsGroup {
                ThemeSelectionRow(
                    currentTheme = prefs?.themeConfig ?: ThemeConfig.FOLLOW_SYSTEM,
                    onThemeSelected = viewModel::updateThemeConfig,
                )
            }

            // ---- 操作 ----
            SettingsSectionHeader(stringResource(R.string.settings_header_operation))
            SettingsGroup {
                SwitchSettingRow(
                    icon = Icons.Default.PhoneAndroid,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.settings_move_to_back_title),
                    description = stringResource(R.string.settings_move_to_back_description),
                    checked = prefs?.moveToBackOnCopy ?: false,
                    onCheckedChange = viewModel::updateMoveToBack,
                )
                SettingsGroupDivider()
                GridColumnsSelectionRow(
                    currentConfig = prefs?.gridColumnsConfig ?: GridColumnsConfig.AUTO,
                    onConfigSelected = viewModel::updateGridColumnsConfig,
                )
                SettingsGroupDivider()
                StartupBehaviorSelectionRow(
                    currentBehavior = prefs?.startupBehavior ?: StartupBehavior.RESTORE,
                    onBehaviorSelected = viewModel::updateStartupBehavior,
                )
            }

            // ---- 履歴 ----
            SettingsSectionHeader(stringResource(R.string.settings_header_history))
            SettingsGroup {
                HistoryLimitRow(
                    currentLimit = prefs?.maxHistoryCount ?: 50,
                    onLimitSelected = viewModel::updateMaxHistoryCount,
                )
                SettingsGroupDivider()
                ClearHistoryRow(onClear = { showClearHistoryConfirm = true })
            }

            // ---- バックアップ ----
            SettingsSectionHeader(stringResource(R.string.settings_header_backup))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.settings_backup_title),
                    description = stringResource(R.string.settings_backup_description),
                    trailingContent = {
                        FilledTonalButton(
                            onClick = { createFileLauncher.launch("prompt_backup.json") },
                            enabled = !isProcessing,
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.settings_backup_export),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                )
                SettingsGroupDivider()
                SettingsRow(
                    icon = Icons.Default.FileUpload,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = stringResource(R.string.settings_restore_title),
                    description = stringResource(R.string.settings_restore_description),
                    trailingContent = {
                        FilledTonalButton(
                            onClick = { pickFileLauncher.launch("application/json") },
                            enabled = !isProcessing,
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.settings_backup_restore),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── グループコンテナ ────────────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        ),
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

// インセット区切り線。ListItem のテキスト開始位置 (16 + 36icon + 16gap = 68dp) に合わせる
@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
    )
}

// ─── セクションヘッダー ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

// ─── 共通ウィジェット ─────────────────────────────────────────────────────────────

/** M3カラートークンを使ったカラーアイコンコンテナ */
@Composable
private fun SettingsLeadingIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 右側にトレーリングコンテンツを置く標準行（Switch / DropdownMenu / Button 向け）*/
@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    title: String,
    description: String = "",
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = if (description.isNotBlank()) {
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
        leadingContent = {
            SettingsLeadingIcon(icon, iconContainerColor, iconContentColor)
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * セグメントボタンなど幅広コントロール用。
 * テキストブロックの下にコントロールをフル幅で配置する。
 */
@Composable
private fun ExpandedSettingsRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    title: String,
    description: String = "",
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsLeadingIcon(icon, iconContainerColor, iconContentColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // コントロールをアイコン幅分だけインデント
        Box(modifier = Modifier.padding(start = 36.dp + 16.dp)) {
            content()
        }
    }
}

// ─── 各設定行 ───────────────────────────────────────────────────────────────────

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        iconContainerColor = iconContainerColor,
        iconContentColor = iconContentColor,
        title = title,
        description = description,
        modifier = Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            role = Role.Switch,
        ),
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionRow(
    currentTheme: ThemeConfig,
    onThemeSelected: (ThemeConfig) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val themeLabel = when (currentTheme) {
        ThemeConfig.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeConfig.LIGHT         -> stringResource(R.string.settings_theme_light)
        ThemeConfig.DARK          -> stringResource(R.string.settings_theme_dark)
    }

    SettingsRow(
        icon = Icons.Default.Palette,
        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        title = stringResource(R.string.settings_theme_title),
        description = stringResource(R.string.settings_theme_description),
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextButton(
                    onClick = {},
                    modifier = Modifier.menuAnchor(),
                ) {
                    Text(themeLabel)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    ThemeConfig.entries.forEach { config ->
                        val label = when (config) {
                            ThemeConfig.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_system_full)
                            ThemeConfig.LIGHT         -> stringResource(R.string.settings_theme_light)
                            ThemeConfig.DARK          -> stringResource(R.string.settings_theme_dark)
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onThemeSelected(config); expanded = false },
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridColumnsSelectionRow(
    currentConfig: GridColumnsConfig,
    onConfigSelected: (GridColumnsConfig) -> Unit,
) {
    ExpandedSettingsRow(
        icon = Icons.Default.GridView,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        title = stringResource(R.string.settings_grid_columns_title),
        description = stringResource(R.string.settings_grid_columns_description),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GridColumnsConfig.entries.forEachIndexed { idx, config ->
                val label = when (config) {
                    GridColumnsConfig.AUTO    -> stringResource(R.string.settings_grid_columns_auto)
                    GridColumnsConfig.FIXED_2 -> stringResource(R.string.settings_grid_columns_2)
                    GridColumnsConfig.FIXED_3 -> stringResource(R.string.settings_grid_columns_3)
                }
                SegmentedButton(
                    selected = currentConfig == config,
                    onClick = { onConfigSelected(config) },
                    shape = SegmentedButtonDefaults.itemShape(idx, GridColumnsConfig.entries.size),
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartupBehaviorSelectionRow(
    currentBehavior: StartupBehavior,
    onBehaviorSelected: (StartupBehavior) -> Unit,
) {
    ExpandedSettingsRow(
        icon = Icons.Default.Restore,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        title = stringResource(R.string.settings_startup_behavior_title),
        description = stringResource(R.string.settings_startup_behavior_description),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            StartupBehavior.entries.forEachIndexed { idx, behavior ->
                val label = when (behavior) {
                    StartupBehavior.RESTORE -> stringResource(R.string.settings_startup_behavior_restore)
                    StartupBehavior.CLEAR   -> stringResource(R.string.settings_startup_behavior_clear)
                }
                SegmentedButton(
                    selected = currentBehavior == behavior,
                    onClick = { onBehaviorSelected(behavior) },
                    shape = SegmentedButtonDefaults.itemShape(idx, StartupBehavior.entries.size),
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryLimitRow(
    currentLimit: Int,
    onLimitSelected: (Int) -> Unit,
) {
    val limits = listOf(50, 100, 0)
    ExpandedSettingsRow(
        icon = Icons.Default.History,
        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        title = stringResource(R.string.settings_history_limit_title),
        description = stringResource(R.string.settings_history_limit_description),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            limits.forEachIndexed { idx, limit ->
                val label = when (limit) {
                    50   -> stringResource(R.string.settings_history_limit_50)
                    100  -> stringResource(R.string.settings_history_limit_100)
                    else -> stringResource(R.string.settings_history_limit_unlimited)
                }
                SegmentedButton(
                    selected = currentLimit == limit,
                    onClick = { onLimitSelected(limit) },
                    shape = SegmentedButtonDefaults.itemShape(idx, limits.size),
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun ClearHistoryRow(onClear: () -> Unit) {
    SettingsRow(
        icon = Icons.Default.DeleteSweep,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        title = stringResource(R.string.settings_history_clear_title),
        description = stringResource(R.string.settings_history_clear_description),
        trailingContent = {
            FilledTonalButton(
                onClick = onClear,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.settings_history_clear_button),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PromptTileTheme {
        SettingsScreen()
    }
}