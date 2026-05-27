package com.blogspot.yotsudev.prompttile.ui.saved

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar
import com.blogspot.yotsudev.prompttile.ui.main.PromptMode
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    promptViewModel: PromptViewModel,
) {
    val savedPrompts by viewModel.savedPrompts.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var deletingPrompt by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var editingPrompt by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val msgCopied = stringResource(R.string.msg_prompt_copied)
    val msgPositiveAdded = stringResource(R.string.msg_positive_added)
    val msgNegativeAdded = stringResource(R.string.msg_negative_added)
    val msgFullApplied = stringResource(R.string.msg_full_applied)
    val msgDeleted = stringResource(R.string.msg_deleted)
    val msgManualAdded = stringResource(R.string.msg_manual_added)
    val msgUpdated = stringResource(R.string.msg_prompt_updated)

    // ---- パフォーマンス向上のための Callback 定義 (remember 化) ----
    val onCopy = remember(context) {
        { entity: SavedPromptEntity ->
            copyToClipboard(context, entity.promptText)
            scope.launch { snackbarHostState.showSnackbar(msgCopied) }
            Unit
        }
    }

    val onLoadPositive = remember(promptViewModel) {
        { entity: SavedPromptEntity ->
            promptViewModel.loadFromSaved(entity.promptText, PromptMode.POSITIVE)
            scope.launch { snackbarHostState.showSnackbar(msgPositiveAdded) }
            Unit
        }
    }

    val onLoadNegative = remember(promptViewModel) {
        { entity: SavedPromptEntity ->
            promptViewModel.loadFromSaved(entity.negativeText, PromptMode.NEGATIVE)
            scope.launch { snackbarHostState.showSnackbar(msgNegativeAdded) }
            Unit
        }
    }

    val onLoadFull = remember(promptViewModel) {
        { entity: SavedPromptEntity ->
            promptViewModel.loadPromptSet(entity)
            scope.launch { snackbarHostState.showSnackbar(msgFullApplied) }
            Unit
        }
    }

    // ---- ダイアログ群 ----
    deletingPrompt?.let { prompt ->
        ConfirmDeleteDialog(
            targetName = prompt.title,
            onConfirm = {
                viewModel.delete(prompt)
                deletingPrompt = null
                scope.launch { snackbarHostState.showSnackbar(msgDeleted) }
            },
            onDismiss = { deletingPrompt = null },
        )
    }

    editingPrompt?.let { prompt ->
        AddPromptDialog(
            initialTitle = prompt.title,
            initialPositive = prompt.promptText,
            initialNegative = prompt.negativeText,
            onConfirm = { title, positive, negative ->
                viewModel.updatePrompt(prompt.copy(title = title, promptText = positive, negativeText = negative))
                editingPrompt = null
                scope.launch { snackbarHostState.showSnackbar(msgUpdated) }
            },
            onDismiss = { editingPrompt = null },
        )
    }

    if (showAddDialog) {
        AddPromptDialog(
            onConfirm = { title, positive, negative ->
                viewModel.addManualPrompt(title, positive, negative)
                showAddDialog = false
                scope.launch { snackbarHostState.showSnackbar(msgManualAdded) }
            },
            onDismiss = { showAddDialog = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PromptTileTopAppBar(
                title = stringResource(R.string.saved_title),
                filterMode = filterMode,
                onFilterModeChange = viewModel::setFilterMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.saved_add_prompt_desc),
                )
            }
        },
    ) { innerPadding ->
        if (savedPrompts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.saved_empty_msg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = savedPrompts,
                    key = { it.id },
                    contentType = { "saved_prompt_card" }
                ) { entity ->
                    Box(modifier = Modifier.animateItem()) {
                        SavedPromptCard(
                            entity = entity,
                            onCopy = onCopy,
                            onDelete = { deletingPrompt = it },
                            onEdit = { editingPrompt = it },
                            onToggleEnabled = { viewModel.togglePromptEnabled(it) },
                            onLoadPositive = if (entity.promptText.isNotBlank()) onLoadPositive else null,
                            onLoadNegative = if (entity.negativeText.isNotBlank()) onLoadNegative else null,
                            onLoadFull = if (entity.promptText.isNotBlank() && entity.negativeText.isNotBlank()) onLoadFull else null,
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
}
