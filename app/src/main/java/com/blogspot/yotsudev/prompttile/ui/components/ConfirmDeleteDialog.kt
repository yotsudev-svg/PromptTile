package com.blogspot.yotsudev.prompttile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.blogspot.yotsudev.prompttile.R

/**
 * 削除確認ダイアログ。
 *
 * 削除対象の名前を [targetName] で受け取り、汎用的に使えるように設計。
 * カテゴリ削除・単語削除・保存済みプロンプト削除の3箇所で共用できる。
 *
 * 「削除」ボタンをerrorカラーにすることで、
 * 破壊的な操作であることをMaterial3のガイドラインに沿って表現する。
 */
@Composable
fun ConfirmDeleteDialog(
    targetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    StyledDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_delete_confirm_title),
        icon = Icons.Default.Delete,
        iconColor = MaterialTheme.colorScheme.error,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.dialog_delete),
                    color = MaterialTheme.colorScheme.error,
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
        Text(
            text = stringResource(R.string.dialog_delete_confirm_msg, targetName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
