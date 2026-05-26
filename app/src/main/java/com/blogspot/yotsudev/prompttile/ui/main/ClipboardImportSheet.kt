package com.blogspot.yotsudev.prompttile.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blogspot.yotsudev.prompttile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardImportSheet(
    mode: PromptMode,
    items: List<ClipboardImportItem>,
    onToggleEnabled: (ClipboardImportItem) -> Unit,
    onToggleRegister: (ClipboardImportItem) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enabledCount = items.count { it.isEnabled }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.import_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val targetModeStr = if (mode == PromptMode.POSITIVE)
                stringResource(R.string.import_sheet_target_positive)
            else
                stringResource(R.string.import_sheet_target_negative)

            val countStr = stringResource(R.string.import_sheet_count_msg, enabledCount, items.size)

            Text(
                text = "$targetModeStr　$countStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp), // パディングを統一
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                items(
                    items = items,
                    key = { it.id },
                ) { item ->
                    ImportChip(
                        item = item,
                        onToggleEnabled = { onToggleEnabled(item) },
                        onToggleRegister = { onToggleRegister(item) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.import_sheet_register_legend),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.import_sheet_exclude_legend),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    enabled = enabledCount > 0,
                ) {
                    Text(stringResource(R.string.import_sheet_add_with_count, enabledCount))
                }
            }
        }
    }
}

@Composable
private fun ImportChip(
    item: ClipboardImportItem,
    onToggleEnabled: () -> Unit,
    onToggleRegister: () -> Unit,
) {
    FilterChip(
        selected = item.registerToDb,
        onClick = onToggleRegister,
        label = {
            Text(
                text = item.wordEn,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingIcon = {
            IconButton(
                onClick = onToggleRegister,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            ) {
                Icon(
                    imageVector = if (item.registerToDb)
                        Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (item.registerToDb)
                        stringResource(R.string.import_sheet_register_on)
                    else
                        stringResource(R.string.import_sheet_register_off),
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = onToggleEnabled,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (item.isEnabled)
                        stringResource(R.string.import_chip_exclude)
                    else
                        stringResource(R.string.import_chip_restore),
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = item.registerToDb,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            borderWidth = 0.5.dp
        ),
        modifier = Modifier.alpha(if (item.isEnabled) 1f else 0.35f),
        enabled = true,
    )
}
