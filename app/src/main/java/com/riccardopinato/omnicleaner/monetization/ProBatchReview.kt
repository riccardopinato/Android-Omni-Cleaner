package com.riccardopinato.omnicleaner.monetization

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import java.util.Locale

@Composable
fun ProBatchReview(
    controller: ProBatchController,
    state: ProBatchState,
) {
    if (!state.hasCompletedScan) return

    var showResolveConfirmation by remember { mutableStateOf(false) }

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        controller.onSystemTrashResult(result.resultCode == Activity.RESULT_OK)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider()
        Text(stringResource(R.string.pro_review_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.pro_review_candidates, formatBytes(state.totalRecoverableBytes)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.emptyFolders.isNotEmpty()) {
            Text(stringResource(R.string.pro_empty_folders), style = MaterialTheme.typography.labelLarge)
            state.emptyFolders.take(8).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedFolderPaths,
                    title = item.name,
                    detail = item.displayPath,
                    onChecked = { controller.toggleFolder(item.path, it) },
                )
            }
        }

        if (state.duplicateGroups.isNotEmpty()) {
            Text(stringResource(R.string.pro_duplicates), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = controller::selectDuplicateKeepOldest) {
                    Text(stringResource(R.string.action_keep_oldest))
                }
                OutlinedButton(onClick = controller::selectDuplicateKeepNewest) {
                    Text(stringResource(R.string.action_keep_newest))
                }
            }
            state.duplicateGroups.take(6).forEach { group ->
                Text(
                    stringResource(R.string.pro_duplicate_group, group.files.size, formatBytes(group.sizeBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                group.files.take(5).forEach { file ->
                    ReviewCheckRow(
                        checked = file.path in state.selectedDuplicatePaths,
                        title = file.name,
                        detail = file.displayPath,
                        onChecked = { controller.toggleDuplicate(file.path, it) },
                    )
                }
            }
        }

        if (state.largeFiles.isNotEmpty()) {
            Text(stringResource(R.string.pro_large_files), style = MaterialTheme.typography.labelLarge)
            state.largeFiles.take(10).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedLargeFilePaths,
                    title = item.name,
                    detail = "${formatBytes(item.sizeBytes)} · ${item.displayPath}",
                    onChecked = { controller.toggleLargeFile(item.path, it) },
                )
            }
        }

        if (state.thumbnails.isNotEmpty()) {
            Text(stringResource(R.string.pro_thumbnail_cache), style = MaterialTheme.typography.labelLarge)
            state.thumbnails.take(8).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedThumbnailPaths,
                    title = item.name,
                    detail = stringResource(R.string.pro_thumbnail_detail, item.fileCount, formatBytes(item.sizeBytes)),
                    onChecked = { controller.toggleThumbnail(item.path, it) },
                )
            }
        }

        if (state.residues.isNotEmpty()) {
            Text(stringResource(R.string.pro_residues), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.pro_residue_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            state.residues.take(8).forEach { item ->
                Text(
                    stringResource(R.string.pro_residue_detail, item.name, formatBytes(item.sizeBytes), item.displayPath),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.resolving || state.awaitingSystemTrash) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { showResolveConfirmation = true },
            enabled = !state.resolving && !state.awaitingSystemTrash,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.pro_resolve_selection))
        }

        state.message?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = controller::clearMessage) {
                Text(stringResource(R.string.action_close))
            }
        }

        state.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showResolveConfirmation) {
        AlertDialog(
            onDismissRequest = { showResolveConfirmation = false },
            title = { Text(stringResource(R.string.pro_confirm_title)) },
            text = {
                Text(stringResource(R.string.pro_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResolveConfirmation = false
                        controller.resolve { pendingIntent ->
                            systemTrashLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ReviewCheckRow(
    checked: Boolean,
    title: String,
    detail: String,
    onChecked: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
