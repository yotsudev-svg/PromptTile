package com.blogspot.yotsudev.prompttile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTileTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    showSearchAction: Boolean = false,
    isSearching: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchingChange: (Boolean) -> Unit = {},
    isWideScreen: Boolean = false,
    filterMode: ManagementFilterMode? = null,
    onFilterModeChange: (ManagementFilterMode) -> Unit = {},
) {
    TopAppBar(
        title = {
            if (isSearching && !isWideScreen) {
                // コンパクト画面での検索中：タイトル部分をTextFieldに置き換える
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.word_pool_search_placeholder),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else if (isWideScreen) {
                // ワイド画面：タイトル + 検索バー
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DockedSearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange,
                                onSearch = {},
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.word_pool_search_placeholder),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                        }
                                    }
                                },
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.weight(1f)
                    ) {}
                }
            } else {
                // 通常時：タイトルのみ
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        navigationIcon = {
            if (isSearching && !isWideScreen) {
                // 検索中の戻るボタン
                IconButton(onClick = {
                    onSearchingChange(false)
                    onSearchQueryChange("")
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                navigationIcon?.invoke()
            }
        },
        actions = {
            if (!isSearching || isWideScreen) {
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

                // showSearchActionがtrueの場合のみ、かつコンパクト画面、かつ非検索時に検索アイコンを表示
                if (showSearchAction && !isWideScreen && !isSearching) {
                    IconButton(onClick = { onSearchingChange(true) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
                actions()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
    )
}
