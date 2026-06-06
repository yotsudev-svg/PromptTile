package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import com.blogspot.yotsudev.prompttile.ui.components.StyledDialog

@Composable
fun SavedPromptDetailDialog(
    entity: SavedPromptEntity,
    onDismiss: () -> Unit,
    onCopy: (SavedPromptEntity) -> Unit,
    onEdit: (SavedPromptEntity) -> Unit,
    onDelete: (SavedPromptEntity) -> Unit,
    onLoadPositive: ((SavedPromptEntity) -> Unit)? = null,
    onLoadNegative: ((SavedPromptEntity) -> Unit)? = null,
    onLoadFull: ((SavedPromptEntity) -> Unit)? = null,
) {
    StyledDialog(
        onDismissRequest = onDismiss,
        title = entity.title,
        icon = Icons.Default.Info,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), fontWeight = FontWeight.Bold)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- アクションボタン行 (Copy, Edit, Delete) ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onCopy(entity) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.saved_copy),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (!entity.isDefault) {
                    IconButton(onClick = { onEdit(entity) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onDelete(entity) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.saved_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ---- プロンプト内容 ----
            if (entity.promptText.isNotBlank()) {
                DetailItem(
                    label = stringResource(R.string.preview_mode_positive),
                    text = entity.promptText,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (entity.negativeText.isNotBlank()) {
                DetailItem(
                    label = stringResource(R.string.preview_mode_negative),
                    text = entity.negativeText,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ---- 適用ボタン群 ----
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onLoadFull != null && entity.promptText.isNotBlank() && entity.negativeText.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { 
                            onLoadFull(entity)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.saved_apply_all))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onLoadPositive != null && entity.promptText.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { 
                                onLoadPositive(entity)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(stringResource(R.string.saved_apply_positive))
                        }
                    }
                    if (onLoadNegative != null && entity.negativeText.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { 
                                onLoadNegative(entity)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(stringResource(R.string.saved_apply_negative))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, text: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
