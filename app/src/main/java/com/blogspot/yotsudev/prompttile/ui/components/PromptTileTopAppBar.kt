package com.blogspot.yotsudev.prompttile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTileTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    showSearchAction: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isWideScreen: Boolean = false,
    filterMode: ManagementFilterMode? = null,
    onFilterModeChange: (ManagementFilterMode) -> Unit = {},
) {
    TopAppBar(
        title = {
            if (showSearchAction) {
                // 常時表示の検索バー
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.word_pool_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CircleShape),
                    singleLine = true
                )
            } else if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        navigationIcon = {
            navigationIcon?.invoke()
        },
        actions = {
            if (filterMode != null) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ManagementFilterMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ManagementFilterMode.ALL -> "すべて表示"
                                ManagementFilterMode.ENABLED_ONLY -> "有効のみ"
                                ManagementFilterMode.DISABLED_ONLY -> "非表示のみ"
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onFilterModeChange(mode)
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (filterMode == mode) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
    )
}

@Preview
@Composable
fun PromptTileTopAppBarPreview() {
    MaterialTheme {
        PromptTileTopAppBar(
            showSearchAction = true,
            searchQuery = "Search query"
        )
    }
}

@Preview
@Composable
fun PromptTileTopAppBarTitlePreview() {
    MaterialTheme {
        PromptTileTopAppBar(
            title = "Settings"
        )
    }
}
