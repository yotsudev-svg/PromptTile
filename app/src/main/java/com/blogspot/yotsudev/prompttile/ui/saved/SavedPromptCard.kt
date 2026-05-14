package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.data.entity.SavedPromptEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedPromptCard(
    entity: SavedPromptEntity,
    onCopy: (SavedPromptEntity) -> Unit,
    onDelete: (SavedPromptEntity) -> Unit,
    onLoadPositive: ((SavedPromptEntity) -> Unit)? = null,
    onLoadNegative: ((SavedPromptEntity) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 8.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ---- タイトル行 + コピー・削除ボタン ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = entity.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entity.promptText.isNotBlank()) {
                        Text(
                            text = entity.promptText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (entity.negativeText.isNotBlank()) {
                        Text(
                            text = "Negative:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        )
                        Text(
                            text = entity.negativeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = entity.createdAt.toDateString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                IconButton(onClick = { onCopy(entity) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "コピー",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { onDelete(entity) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ---- 読み込みボタン行 ----
            /**
             * ポジ・ネガのボタンを横並びで配置する。
             * OutlinedButton を使うことで「コピー・削除」の IconButton と
             * 視覚的に区別しつつ、タップ領域を広く確保できる。
             *
             * null チェックを入れることで、コールバック未指定時は
             * ボタン行ごと非表示にできる（後方互換性の確保）。
             *
             * ポジティブは primary、ネガティブは error カラーにすることで
             * 「どちらに追加されるか」を色で直感的に伝える。
             */
            if (onLoadPositive != null || onLoadNegative != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (onLoadPositive != null && entity.promptText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onLoadPositive(entity) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = "+ Positive",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    if (onLoadNegative != null && entity.negativeText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onLoadNegative(entity) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(
                                text = "+ Negative",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Long.toDateString(): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(this))