package com.blogspot.yotsudev.prompttile.ui.main

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.CategoryEntity
import com.blogspot.yotsudev.prompttile.data.entity.PromptWordEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import com.blogspot.yotsudev.prompttile.data.repository.UNCATEGORIZED_NEGATIVE_NAME
import com.blogspot.yotsudev.prompttile.data.repository.UNCATEGORIZED_POSITIVE_NAME
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar
import com.blogspot.yotsudev.prompttile.util.PromptFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PromptViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 常利用する文字列リソース
    val msgPromptCopied = stringResource(R.string.msg_prompt_copied)
    val msgWordCopiedTemplate = stringResource(R.string.msg_word_copied)
    val msgPromptSaved = stringResource(R.string.msg_prompt_saved)
    val msgPromptAdded = stringResource(R.string.msg_prompt_added)

    // 計算コストの高い処理を remember で保護 (Recomposition 最適化)
    val hasItems = remember(uiState.currentItems) { uiState.currentItems.isNotEmpty() }

    val selectedWordIds = remember(uiState.currentItems) {
        uiState.currentItems.map { it.wordId }.toHashSet()
    }

    val uncategorizedIds = remember(uiState.positiveCategories, uiState.negativeCategories) {
        (uiState.positiveCategories + uiState.negativeCategories)
            .filter { it.nameEn == UNCATEGORIZED_POSITIVE_NAME || it.nameEn == UNCATEGORIZED_NEGATIVE_NAME }
            .map { it.id }
            .toSet()
    }
    // ---- ボトムシート関連のステート ----
    var toppingTargetWord by remember { mutableStateOf<PromptWordEntity?>(null) }
    var toppingTargetGroup by remember { mutableStateOf<ToppingGroupEntity?>(null) }
    var toppingChoices by remember { mutableStateOf<List<ToppingItemEntity>>(emptyList()) }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        SavePromptDialog(
            onConfirm = { title ->
                viewModel.saveCurrentPrompt(title)
                showSaveDialog = false
                scope.launch { snackbarHostState.showSnackbar(msgPromptSaved) }
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    // ---- ボトムシート ----
    // 1. クリップボードインポート
    var importItems by remember { mutableStateOf<List<ClipboardImportItem>?>(null) }
    importItems?.let { items ->
        ClipboardImportSheet(
            mode = uiState.mode,
            items = items,
            onToggleEnabled = { item ->
                importItems = items.map {
                    if (it.id == item.id) it.copy(isEnabled = !it.isEnabled) else it
                }
            },
            onToggleRegister = { item ->
                importItems = items.map {
                    if (it.id == item.id) it.copy(registerToDb = !it.registerToDb) else it
                }
            },
            onConfirm = {
                viewModel.confirmClipboardImport(items)
                importItems = null
                scope.launch { snackbarHostState.showSnackbar(msgPromptAdded) }
            },
            onDismiss = { importItems = null },
        )
    }

    // 2. 単語追加時のトッピング選択シート (WordPool から)
    val targetWord = toppingTargetWord
    val targetGroup = toppingTargetGroup
    if (targetWord != null && targetGroup != null) {
        ToppingSelectSheet(
            word = targetWord,
            group = targetGroup,
            toppingItems = toppingChoices,
            onSelect = { groupId, topping, isPrefix ->
                viewModel.addWordWithTopping(targetWord, groupId, topping, isPrefix)
                toppingTargetWord = null
                toppingTargetGroup = null
            },
            onDismiss = {
                toppingTargetWord = null
                toppingTargetGroup = null
            }
        )
    }

    // 3. ワークスペースのチップ調整シート (PreviewArea から)
    uiState.adjustingItem?.let { item ->
        PromptAdjustSheet(
            item = item,
            toppingGroups = uiState.adjustingToppingGroups,
            onWeightSelect = { weight -> viewModel.setWeight(item, weight) },
            onToppingSelect = { groupId, topping, isPrefix ->
                viewModel.setTopping(item, groupId, topping, isPrefix)
            },
            onDelete = {
                viewModel.removeItem(item)
                viewModel.closeAdjustSheet()
            },
            onDismiss = viewModel::closeAdjustSheet
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                PromptTileTopAppBar(
                    title = "PromptTile",
                    showSearchAction = true,
                    isSearching = isSearching,
                    onSearchingChange = { isSearching = it },
                    isWideScreen = isWideScreen,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    actions = {
                        IconButton(onClick = {
                            handleClipboardImport(context, snackbarHostState, scope) { items -> importItems = items }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = stringResource(R.string.main_import_from_clipboard),
                            )
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.common_more_options)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.main_save)) },
                                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSaveDialog = true
                                    },
                                    enabled = uiState.positiveItems.isNotEmpty() || uiState.negativeItems.isNotEmpty()
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.main_clear_all),
                                            color = if (hasItems) MaterialTheme.colorScheme.error else Color.Unspecified
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ClearAll,
                                            contentDescription = null,
                                            tint = if (hasItems) MaterialTheme.colorScheme.error else LocalContentColor.current
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.clearAll()
                                    },
                                    enabled = hasItems
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    // ---- 3ペイン構成 (Expanded) ----
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 左ペイン: カテゴリ (固定幅)
                        CategorySidebar(
                            parentCategories = uiState.currentParentCategories,
                            selectedParentId = uiState.currentSelectedParentId,
                            onParentSelected = viewModel::selectParent,
                            childCategories = uiState.currentCategories,
                            selectedChildId = uiState.currentSelectedCategoryId,
                            onChildSelected = viewModel::selectCategory,
                            modifier = Modifier.width(240.dp) // 少し広げる
                        )

                        VerticalDivider()

                        // 中央ペイン: 単語プール (可変)
                        Column(modifier = Modifier.weight(1f)) {
                            WordPool(
                                words = uiState.wordsInCategory,
                                searchResults = uiState.searchResults,
                                searchQuery = uiState.searchQuery,
                                selectedWordIds = selectedWordIds,
                                uncategorizedIds = uncategorizedIds,
                                onWordTap = viewModel::toggleWord,
                                onWordLongPress = { word ->
                                    copyToClipboard(context, word.wordEn)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            msgWordCopiedTemplate.format(word.wordEn)
                                        )
                                    }
                                },
                                onToppingIconTap = { word ->
                                    // トッピング選択肢を読み込んでシートを表示
                                    scope.launch {
                                        val gids = word.toppingGroupIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
                                        gids.firstOrNull()?.let { groupId ->
                                            val group = viewModel.getToppingGroup(groupId)
                                            if (group != null) {
                                                toppingChoices = viewModel.getToppingItems(groupId)
                                                toppingTargetGroup = group
                                                toppingTargetWord = word
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        VerticalDivider()
                        PreviewArea(
                            mode = uiState.mode,
                            items = uiState.currentItems,
                            promptText = uiState.currentPromptText,
                            allTemplates = uiState.allTemplates,
                            canUndo = uiState.canUndo,
                            canRedo = uiState.canRedo,
                            onModeChange = viewModel::switchMode,
                            onRemove = viewModel::removeItem,
                            onWeightCycle = viewModel::openAdjustSheet,
                            onMove = viewModel::moveItem,
                            onCopyAll = {
                                val text = viewModel.buildPromptText()
                                copyToClipboard(context, text)
                                scope.launch { snackbarHostState.showSnackbar(msgPromptCopied) }
                            },
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onAddTemplate = viewModel::addTemplateItems,
                            modifier = Modifier.width(280.dp),
                            isVertical = true
                        )
                    }
                } else {
                    // ---- 1列構成 (Compact): 従来のモバイルレイアウト ----
                    Column(modifier = Modifier.fillMaxSize()) {
                        PreviewArea(
                            mode = uiState.mode,
                            items = uiState.currentItems,
                            promptText = uiState.currentPromptText,
                            allTemplates = uiState.allTemplates,
                            canUndo = uiState.canUndo,
                            canRedo = uiState.canRedo,
                            onModeChange = viewModel::switchMode,
                            onRemove = viewModel::removeItem,
                            onWeightCycle = viewModel::openAdjustSheet,
                            onMove = viewModel::moveItem,
                            onCopyAll = {
                                val text = viewModel.buildPromptText()
                                copyToClipboard(context, text)
                                scope.launch { snackbarHostState.showSnackbar(msgPromptCopied) }
                                if (uiState.moveToBackOnCopy) {
                                    (context as? Activity)?.moveTaskToBack(true)
                                }
                            },
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onAddTemplate = viewModel::addTemplateItems,
                            modifier = Modifier.fillMaxWidth(),
                            isVertical = false
                        )

                        HorizontalDivider()

                        CategoryBar(
                            parentCategories = uiState.currentParentCategories,
                            selectedParentId = uiState.currentSelectedParentId,
                            onParentSelected = viewModel::selectParent,
                            childCategories = uiState.currentCategories,
                            selectedChildId = uiState.currentSelectedCategoryId,
                            onChildSelected = viewModel::selectCategory,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HorizontalDivider()

                        WordPool(
                            words = uiState.wordsInCategory,
                            searchResults = uiState.searchResults,
                            searchQuery = uiState.searchQuery,
                            selectedWordIds = selectedWordIds,
                            uncategorizedIds = uncategorizedIds,
                            onWordTap = viewModel::toggleWord,
                            onWordLongPress = { word ->
                                copyToClipboard(context, word.wordEn)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        msgWordCopiedTemplate.format(word.wordEn)
                                    )
                                }
                                if (uiState.moveToBackOnCopy) {
                                    (context as? Activity)?.moveTaskToBack(true)
                                }
                            },
                            onToppingIconTap = { word ->
                                scope.launch {
                                    val gids = word.toppingGroupIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
                                    gids.firstOrNull()?.let { groupId ->
                                        val group = viewModel.getToppingGroup(groupId)
                                        if (group != null) {
                                            toppingChoices = viewModel.getToppingItems(groupId)
                                            toppingTargetGroup = group
                                            toppingTargetWord = word
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                }
            }
        }
    }
}


private fun handleClipboardImport(
    context: Context,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    onImportReady: (List<ClipboardImportItem>) -> Unit
) {
    val text = getClipboardText(context)
    if (text.isNullOrBlank()) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_clipboard_empty)) }
        return
    }

    val words = text.split(",")
        .map { PromptFormatter.cleanWord(it) }
        .filter { it.isNotBlank() }
        .distinct()

    if (words.isEmpty()) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_words_to_add)) }
        return
    }

    onImportReady(words.mapIndexed { i, w ->
        ClipboardImportItem(id = i, wordEn = w)
    })
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
}

private fun getClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.getItemAt(0)?.text?.toString()
}
