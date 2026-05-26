package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog

/**
 * プロンプトを手動で追加するためのダイアログ。
 * [StyledDialog] を使用し、一貫した洗練されたデザインを提供します。
 */
@Composable
fun AddPromptDialog(
    onConfirm: (title: String, positive: String, negative: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title    by rememberSaveable { mutableStateOf("") }
    var positive by rememberSaveable { mutableStateOf("") }
    var negative by rememberSaveable { mutableStateOf("") }

    val canSave = positive.isNotBlank() || negative.isNotBlank()

    StyledDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_add_prompt_title),
        icon = Icons.Default.PostAdd,
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(title.trim(), positive.trim(), negative.trim()) },
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    }
}
