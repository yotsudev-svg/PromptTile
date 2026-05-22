package com.blogspot.yotsudev.prompttile.ui.saved

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var deletingPrompt by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val msgCopied = stringResource(R.string.msg_prompt_copied)
    val msgPositiveAdded = stringResource(R.string.msg_positive_added)
    val msgNegativeAdded = stringResource(R.string.msg_negative_added)
    val msgFullApplied = stringResource(R.string.msg_full_applied)
    val msgDeleted = stringResource(R.string.msg_deleted)
    val msgManualAdded = stringResource(R.string.msg_manual_added)

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
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
                    SavedPromptCard(
                        entity = entity,
                        onCopy = onCopy,
                        onDelete = { deletingPrompt = it },
                        onLoadPositive = if (entity.promptText.isNotBlank()) onLoadPositive else null,
                        onLoadNegative = if (entity.negativeText.isNotBlank()) onLoadNegative else null,
                        onLoadFull = if (entity.promptText.isNotBlank() && entity.negativeText.isNotBlank()) onLoadFull else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPromptDialog(
    onConfirm: (title: String, positive: String, negative: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title    by rememberSaveable { mutableStateOf("") }
    var positive by rememberSaveable { mutableStateOf("") }
    var negative by rememberSaveable { mutableStateOf("") }

    val canSave = positive.isNotBlank() || negative.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.dialog_save_prompt_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_save_prompt_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = positive,
                    onValueChange = { positive = it },
                    label = { Text(stringResource(R.string.dialog_add_prompt_positive)) },
                    placeholder = { Text(stringResource(R.string.dialog_add_prompt_positive_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text(stringResource(R.string.dialog_add_prompt_negative)) },
                    placeholder = { Text(stringResource(R.string.dialog_add_prompt_negative_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(title, positive, negative) },
                enabled = canSave,
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
}