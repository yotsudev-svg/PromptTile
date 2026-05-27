package com.blogspot.yotsudev.prompttile.ui.edit

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.preferences.ManagementFilterMode
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    var showResetOrderDialog by remember { mutableStateOf(false) }

    val msgAdded = stringResource(R.string.msg_prompt_added)
    val msgSaved = stringResource(R.string.msg_prompt_saved)
    val msgDeleted = stringResource(R.string.msg_deleted)

    // リセット確認ダイアログ
    if (showResetOrderDialog) {
        AlertDialog(
            onDismissRequest = { showResetOrderDialog = false },
            title = { Text("並び順のリセット") },
            text = { Text("カテゴリの並び順をデフォルト（登録順）に戻しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetCategoryOrder()
                    showResetOrderDialog = false
                    scope.launch { snackbarHostState.showSnackbar("並び順をリセットしました") }
                }) { Text("リセット") }
            },
            dismissButton = {
                TextButton(onClick = { showResetOrderDialog = false }) { Text("キャンセル") }
            }
        )
    }

    // ---- 各種編集/削除ダイアログ ----
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
            PromptTileTopAppBar(
                title = stringResource(R.string.edit_title),
                filterMode = uiState.filterMode,
                onFilterModeChange = viewModel::setFilterMode,
                actions = {
                    IconButton(onClick = { showResetOrderDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "並び順をリセット")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.filterMode != ManagementFilterMode.DISABLED_ONLY) {
                FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.edit_add_category_desc))
                }
            }
        },
    ) { innerPadding ->
        val isEmpty = if (uiState.filterMode == ManagementFilterMode.DISABLED_ONLY)
            uiState.categories.isEmpty() && uiState.wordsInDisabledOnly.isEmpty() 
        else uiState.categories.isEmpty()

        if (isEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.filterMode == ManagementFilterMode.DISABLED_ONLY) "非表示の単語はありません" else stringResource(R.string.edit_empty_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val lazyListState = rememberLazyListState()
            val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
                viewModel.reorderCategories(from.index, to.index)
            }

            if (uiState.filterMode != ManagementFilterMode.DISABLED_ONLY) {
                LaunchedEffect(reorderableLazyColumnState) {
                    snapshotFlow { reorderableLazyColumnState.isAnyItemDragging }
                        .collect { isDragging ->
                            if (!isDragging) {
                                viewModel.persistCategoryOrder()
                            }
                        }
                }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. 非表示のカテゴリを最優先で表示
                itemsIndexed(uiState.categories, key = { _, category -> category.id }) { _, category ->
                    // ENABLED_ONLY の時は表示されない（ViewModelでフィルタ済み）
                    // DISABLED_ONLY の時は isHidden=true のものだけ表示される
                    // ALL の時は全部表示される
                    ReorderableItem(reorderableLazyColumnState, key = category.id) { isDragging ->
                        val isExpanded = category.id == uiState.expandedCategoryId
                        Box(modifier = Modifier.animateItem()) {
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
                                onReorderWords = { from, to -> viewModel.reorderWords(from, to) },
                                onSettleWords = { viewModel.persistWordOrder() },
                                isDragging = isDragging,
                                dragHandle = {
                                    Icon(
                                        imageVector = Icons.Default.Reorder,
                                        contentDescription = "ドラッグして移動",
                                        modifier = Modifier.draggableHandle().padding(horizontal = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. 「親カテゴリは表示中だが、中身の単語だけ非表示」な単語をフラットに表示
                // DISABLED_ONLY または ALL モードの時のみ表示
                if (uiState.filterMode != ManagementFilterMode.ENABLED_ONLY) {
                    val orphanDisabledWords = uiState.wordsInDisabledOnly.filter { item ->
                        // 親カテゴリが uiState.categories に含まれていない（=非表示カテゴリではない）
                        // かつ、その単語が既にツリー内で表示されていない場合に抽出
                        uiState.categories.none { it.id == item.word.categoryId }
                    }

                    if (orphanDisabledWords.isNotEmpty()) {
                        item {
                            Text(
                                text = "非表示の単語 (個別)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        itemsIndexed(orphanDisabledWords, key = { _, item -> "orphan_${item.word.id}" }) { _, item ->
                            Box(modifier = Modifier.animateItem()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                ) {
                                    WordEditRow(
                                        word = item.word,
                                        categoryName = item.categoryNameJa,
                                        onEdit = { editingWord = item.word },
                                        onDelete = { deletingWord = item.word },
                                        onToggleVisibility = { viewModel.toggleWordVisibility(item.word) },
                                        dragHandle = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
