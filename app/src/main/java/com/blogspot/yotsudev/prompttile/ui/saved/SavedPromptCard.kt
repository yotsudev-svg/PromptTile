package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.blogspot.yotsudev.prompttile.R
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedPromptCard(
    entity: SavedPromptEntity,
    onClick: (SavedPromptEntity) -> Unit,
    onCopy: (SavedPromptEntity) -> Unit,
    onDelete: (SavedPromptEntity) -> Unit,
    onEdit: (SavedPromptEntity) -> Unit = {},
    onToggleEnabled: (SavedPromptEntity) -> Unit = {},
    dragHandle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 日付文字列を remember で保持
    val dateString = remember(entity.createdAt) { entity.createdAt.toDateString() }
    val contentAlpha = if (entity.isEnabled) 1.0f else 0.5f

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(entity) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ---- ヘッダー: タイトルと基本アクション ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entity.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entity.isDefault) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "DEFAULT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    dragHandle()
                    IconButton(onClick = { onToggleEnabled(entity) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (entity.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (entity.isEnabled) "Enabled" else "Disabled",
                            tint = if (entity.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { onCopy(entity) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.saved_copy),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ---- プロンプトプレビュー ----
            if (entity.promptText.isNotBlank()) {
                PromptPreviewItem(
                    label = stringResource(R.string.preview_mode_positive),
                    text = entity.promptText,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (entity.negativeText.isNotBlank()) {
                PromptPreviewItem(
                    label = stringResource(R.string.preview_mode_negative),
                    text = entity.negativeText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PromptPreviewItem(label: String, text: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun Long.toDateString(): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(this))
