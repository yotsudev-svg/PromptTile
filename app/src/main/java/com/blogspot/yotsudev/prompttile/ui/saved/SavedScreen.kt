package com.blogspot.yotsudev.prompttile.ui.saved

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.ui.components.ConfirmDeleteDialog
import com.blogspot.yotsudev.prompttile.ui.main.PromptMode
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    promptViewModel: PromptViewModel,
) {
    val savedPrompts by viewModel.savedPrompts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var deletingPrompt by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    // ---- 削除確認ダイアログ ----
    deletingPrompt?.let { prompt ->
        ConfirmDeleteDialog(
            targetName = prompt.title,
            onConfirm = {
                viewModel.delete(prompt)
                deletingPrompt = null
            },
            onDismiss = { deletingPrompt = null },
        )
    }

    // ---- 手動追加ダイアログ ----
    if (showAddDialog) {
        AddPromptDialog(
            onConfirm = { title, positive, negative ->
                viewModel.addManualPrompt(title, positive, negative)
                showAddDialog = false
                Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("保存済み") })
        },
        floatingActionButton = {
            /**
             * FAB で手動追加ダイアログを開く。
             * EditScreen の「カテゴリ追加」と同じ配置パターンなので
             * ユーザーが迷わない。
             */
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "プロンプトを手動追加",
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
                    text = "保存済みのプロンプトはありません\nメイン画面で単語を選んで保存しましょう",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp, // FABと重ならないよう余白
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = savedPrompts,
                    key = { it.id },
                ) { entity ->
                    SavedPromptCard(
                        entity = entity,
                        onCopy = { prompt ->
                            copyToClipboard(context, prompt.promptText)
                            Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { deletingPrompt = it },
                        onLoadPositive = { prompt ->        // ← 追加
                            promptViewModel.loadFromSaved(prompt.promptText, PromptMode.POSITIVE)
                            Toast.makeText(context, "Positiveに追加しました", Toast.LENGTH_SHORT).show()
                        },
                        onLoadNegative = { prompt ->        // ← 追加
                            promptViewModel.loadFromSaved(prompt.negativeText, PromptMode.NEGATIVE)
                            Toast.makeText(context, "Negativeに追加しました", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

/**
 * 外部プロンプトを手動入力して保存するダイアログ。
 *
 * タイトルは任意入力（空欄時は日時で自動生成される）。
 * ポジティブ・ネガティブはどちらか一方でも入力があれば保存可能。
 * canSave の条件を「どちらかが非空白」にすることで
 * 「ネガティブのみ」「ポジティブのみ」のストックにも対応できる。
 *
 * OutlinedTextField を複数並べるため maxLines を設定せず、
 * singleLine = false のまま使うことでペースト時に長文でも見やすい。
 */
@Composable
private fun AddPromptDialog(
    onConfirm: (title: String, positive: String, negative: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title    by rememberSaveable { mutableStateOf("") }
    var positive by rememberSaveable { mutableStateOf("") }
    var negative by rememberSaveable { mutableStateOf("") }

    // タイトルは任意。ポジ・ネガどちらか一方でも入力があれば保存可能
    val canSave = positive.isNotBlank() || negative.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プロンプトを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル（任意）") },
                    placeholder = { Text("例: キャラクターA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = positive,
                    onValueChange = { positive = it },
                    label = { Text("ポジティブ") },
                    placeholder = { Text("例: masterpiece, best quality, 1girl") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text("ネガティブ") },
                    placeholder = { Text("例: lowres, bad anatomy, blurry") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(title, positive, negative) },
                enabled = canSave,
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
}