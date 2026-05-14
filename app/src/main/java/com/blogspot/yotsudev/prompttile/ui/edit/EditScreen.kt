package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 単語・カテゴリ編集画面。
 *
 * FAB でカテゴリ追加ダイアログを開き、
 * 各カテゴリカードのアコーディオンで単語を編集する。
 *
 * EditScreen 自体はダイアログ状態を持たず、
 * カテゴリ追加ダイアログだけをローカルで管理する。
 * 単語・カテゴリ編集ダイアログは CategoryEditCard 内部に閉じている。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (showAddCategoryDialog) {
        CategoryDialog(
            onConfirm = { ja, en ->
                viewModel.addCategory(ja, en)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("単語・カテゴリ編集") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "カテゴリを追加",
                )
            }
        },
    ) { innerPadding ->
        if (uiState.categories.isEmpty()) {
            // ---- 空状態 ----
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("右下の＋ボタンでカテゴリを追加しましょう")
            }
        } else {
            // ---- カテゴリ一覧 ----
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = uiState.categories,
                    key = { it.id },
                ) { category ->
                    val isExpanded = category.id == uiState.expandedCategoryId
                    CategoryEditCard(
                        category = category,
                        isExpanded = isExpanded,
                        words = if (isExpanded) uiState.wordsInExpanded else emptyList(),
                        allCategories = uiState.categories,          // ← 追加
                        onToggleExpand = { viewModel.toggleExpand(category.id) },
                        onEditCategory = { ja, en -> viewModel.updateCategory(category, ja, en) },
                        onDeleteCategory = { viewModel.deleteCategory(category) },
                        onToggleCategoryVisibility = { viewModel.toggleCategoryVisibility(category) },
                        onAddWord = { en, ja -> viewModel.addWord(category.id, en, ja) },
                        onEditWord = { word, en, ja, newCategoryId ->  // ← newCategoryId 追加
                            viewModel.updateWord(word, en, ja, newCategoryId)
                        },
                        onDeleteWord = { viewModel.deleteWord(it) },
                        onToggleWordVisibility = { viewModel.toggleWordVisibility(it) },
                    )
                }
            }
        }
    }
}