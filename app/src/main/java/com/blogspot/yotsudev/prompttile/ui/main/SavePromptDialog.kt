package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog

/**
 * プロンプト保存時にタイトルを入力するダイアログ。
 *
 * タイトルの入力状態は rememberSaveable で保持する。
 * saveable にすることで画面回転時も入力内容が消えない。
 *
 * ダイアログの「表示/非表示」状態は呼び出し元(MainScreen)が持ち、
 * このComposableは表示中の振る舞いだけを担当する（単一責任）。
 */
@Composable
fun SavePromptDialog(
    onConfirm: (title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    val canSave = title.isNotBlank()

    StyledDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_save_prompt_title),
        icon = Icons.Default.Save,
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSave) {
                        onConfirm(title.trim())
                    }
                },
                enabled = canSave,
            ) {
                Text(
                    text = stringResource(R.string.dialog_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.dialog_save_prompt_label)) },
            placeholder = { Text(stringResource(R.string.dialog_save_prompt_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
