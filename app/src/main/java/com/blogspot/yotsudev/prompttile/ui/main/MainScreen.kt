package com.blogspot.yotsudev.prompttile.ui.main

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.repository.UNCATEGORIZED_NEGATIVE_NAME
import com.blogspot.yotsudev.prompttile.data.repository.UNCATEGORIZED_POSITIVE_NAME
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

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    /**
     * ボトムシートのプレビューアイテムリスト。
     */
    var importItems by remember { mutableStateOf<List<ClipboardImportItem>?>(null) }

    if (showSaveDialog) {
        val msgSaved = stringResource(R.string.msg_prompt_saved)
        SavePromptDialog(
            onConfirm = { title ->
                viewModel.saveCurrentPrompt(title)
                showSaveDialog = false
                scope.launch { snackbarHostState.showSnackbar(msgSaved) }
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    // ---- ボトムシート ----
    importItems?.let { items ->
        val msgAdded = stringResource(R.string.msg_prompt_added)
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
                scope.launch { snackbarHostState.showSnackbar(msgAdded) }
            },
            onDismiss = { importItems = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopAppBar(
                hasItems = hasItems,
                canSave = uiState.positiveItems.isNotEmpty() || uiState.negativeItems.isNotEmpty(),
                onImportClick = {
                    handleClipboardImport(context, snackbarHostState, scope) { items -> importItems = items }
                },
                onClearClick = viewModel::clearAll,
                onSaveClick = { showSaveDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            val msgCopied = stringResource(R.string.msg_prompt_copied)
            PreviewArea(
                mode = uiState.mode,
                items = uiState.currentItems,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onModeChange = viewModel::switchMode,
                onRemove = viewModel::removeItem,
                onWeightCycle = viewModel::cycleWeight,
                onMove = viewModel::moveItem,
                onCopyAll = {
                    val text = viewModel.buildPromptText()
                    copyToClipboard(context, text)
                    scope.launch { snackbarHostState.showSnackbar(msgCopied) }
                    if (uiState.moveToBackOnCopy) {
                        (context as? Activity)?.moveTaskToBack(true)
                    }
                },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            CategoryBar(
                categories = uiState.currentCategories,
                selectedCategoryId = uiState.currentSelectedCategoryId,
                onCategorySelected = viewModel::selectCategory,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            val msgWordCopiedTemplate = stringResource(R.string.msg_word_copied)
            WordPool(
                words = uiState.wordsInCategory,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    hasItems: Boolean,
    canSave: Boolean,
    onImportClick: () -> Unit,
    onClearClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "PromptTile",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            IconButton(onClick = onImportClick) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = stringResource(R.string.main_import_from_clipboard),
                )
            }
            IconButton(
                onClick = onClearClick,
                enabled = hasItems,
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = stringResource(R.string.main_clear_all),
                )
            }
            IconButton(
                onClick = onSaveClick,
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.main_save),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
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
        .map { it.trim().cleanForImport() }
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

/**
 * インポート時に重み記号を除去する。
 */
private fun String.cleanForImport(): String =
    this.replace(Regex("[()\\[\\]{}]"), "")
        .split(":")[0]
        .trim()
