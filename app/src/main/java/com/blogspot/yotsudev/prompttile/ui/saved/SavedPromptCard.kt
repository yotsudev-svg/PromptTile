package com.blogspot.yotsudev.prompttile.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val dateString = remember(entity.createdAt) { entity.createdAt.toDateString() }
    val contentAlpha = if (entity.isEnabled) 1.0f else 0.45f
    val hasPositive = entity.promptText.isNotBlank()
    val hasNegative = entity.negativeText.isNotBlank()
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        onClick = { onClick(entity) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)  // アクセントバーが本体と同じ高さになるよう固定
                .alpha(contentAlpha)
        ) {
            // ---- 左辺アクセントライン (4dp) ----
            when {
                hasPositive && hasNegative -> Column(
                    modifier = Modifier.width(4.dp).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(errorColor)
                    )
                }
                hasNegative -> Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(errorColor)
                )
                else -> Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(primaryColor)
                )
            }

            // ---- 本体コンテンツ ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // タイトル + DEFAULT バッジ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = entity.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entity.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // 日時
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )

                // プロンプトプレビュー
                if (hasPositive) {
                    PromptPreviewItem(
                        label = stringResource(R.string.preview_mode_positive),
                        text = entity.promptText,
                        isNegative = false,
                    )
                }
                if (hasNegative) {
                    PromptPreviewItem(
                        label = stringResource(R.string.preview_mode_negative),
                        text = entity.negativeText,
                        isNegative = true,
                    )
                }

                // ---- アクションバー ----
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dragHandle()
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onToggleEnabled(entity) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (entity.isEnabled) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (entity.isEnabled) "有効" else "無効",
                            tint = if (entity.isEnabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { onEdit(entity) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編集",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { onCopy(entity) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.saved_copy),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptPreviewItem(
    label: String,
    text: String,
    isNegative: Boolean,
) {
    val labelBg = if (isNegative)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val labelColor = if (isNegative)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer
    val previewBg = if (isNegative)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val previewColor = if (isNegative)
        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(
            color = labelBg,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = labelColor,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        Surface(
            color = previewBg,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
                color = previewColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

private fun Long.toDateString(): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(this))