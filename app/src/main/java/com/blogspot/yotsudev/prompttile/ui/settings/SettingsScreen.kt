package com.blogspot.yotsudev.prompttile.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()

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
