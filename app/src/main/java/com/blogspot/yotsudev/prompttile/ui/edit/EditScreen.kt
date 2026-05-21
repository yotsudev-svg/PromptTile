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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import kotlinx.coroutines.launch

/**
 * 単語・カテゴリ編集画面。
 *
 * ダイアログの状態管理をこの Screen レベルに集約することで、
 * CategoryEditCard を Stateless (状態を持たない) にし、
 * リスト全体の描画パフォーマンスと保守性を向上させています。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ダイアログ管理の状態
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var addingWordToCategoryId by remember { mutableStateOf<Long?>(null) }
    var editingWord by remember { mutableStateOf<PromptWordEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingWord by remember { mutableStateOf<PromptWordEntity?>(null) }

    val msgAdded = stringResource(R.string.msg_prompt_added)
    val msgSaved = stringResource(R.string.msg_prompt_saved)
    val msgDeleted = stringResource(R.string.msg_deleted)

    // ---- ダイアログ群 ----
    if (showAddCategoryDialog) {
        CategoryDialog(
            onConfirm = { ja, en ->
                viewModel.addCategory(ja, en)
                showAddCategoryDialog = false
                scope.launch { snackbarHostState.showSnackbar(msgAdded) }
            },
            onDismiss = { showAddCategoryDialog = false },
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            initial = category,
            onConfirm = { ja, en ->
                viewModel.updateCategory(category, ja, en)
                editingCategory = null
                scope.launch { snackbarHostState.showSnackbar(msgSaved) }
            },
            onDismiss = { editingCategory = null },
        )
    }

    addingWordToCategoryId?.let { categoryId ->
        WordDialog(
            onConfirm = { en, ja, _ ->
                viewModel.addWord(categoryId, en, ja)
                addingWordToCategoryId = null
                scope.launch { snackbarHostState.showSnackbar(msgAdded) }
            },
            onDismiss = { addingWordToCategoryId = null },
        )
    }

    editingWord?.let { word ->
        WordDialog(
            initial = word,
            allCategories = uiState.categories,
            onConfirm = { en, ja, newCategoryId ->
                viewModel.updateWord(word, en, ja, newCategoryId)
                editingWord = null
                scope.launch { snackbarHostState.showSnackbar(msgSaved) }
            },
            onDismiss = { editingWord = null },
        )
    }

    deletingCategory?.let { category ->
        ConfirmDeleteDialog(
            targetName = category.nameJa,
            onConfirm = {
                viewModel.deleteCategory(category)
                deletingCategory = null
                scope.launch { snackbarHostState.showSnackbar(msgDeleted) }
            },
            onDismiss = { deletingCategory = null },
        )
    }

    deletingWord?.let { word ->
        ConfirmDeleteDialog(
            targetName = word.wordEn,
            onConfirm = {
                viewModel.deleteWord(word)
                deletingWord = null
                scope.launch { snackbarHostState.showSnackbar(msgDeleted) }
            },
            onDismiss = { deletingWord = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.edit_add_category_desc),
                )
            }
        },
    ) { innerPadding ->
        if (uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.edit_empty_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = uiState.categories,
                    key = { it.id },
                    contentType = { "category_card" }
                ) { category ->
                    val isExpanded = category.id == uiState.expandedCategoryId
                    CategoryEditCard(
                        category = category,
                        isExpanded = isExpanded,
                        words = if (isExpanded) uiState.wordsInExpanded else emptyList(),
                        onToggleExpand = { viewModel.toggleExpand(category.id) },
                        onEditCategoryClick = { editingCategory = category },
                        onDeleteCategoryClick = { deletingCategory = category },
                        onToggleCategoryVisibility = { viewModel.toggleCategoryVisibility(category) },
                        onAddWordClick = { addingWordToCategoryId = category.id },
                        onEditWordClick = { word -> editingWord = word },
                        onDeleteWordClick = { word -> deletingWord = word },
                        onToggleWordVisibility = { word -> viewModel.toggleWordVisibility(word) },
                    )
                }
            }
        }
    }
}
