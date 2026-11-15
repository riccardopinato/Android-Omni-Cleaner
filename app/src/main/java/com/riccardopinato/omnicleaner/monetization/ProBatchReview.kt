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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as OutlinedButton
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    var displayedFolders by remember { mutableStateOf(8) }
    var displayedGroups by remember { mutableStateOf(6) }
    var displayedGroupFiles by remember { mutableStateOf(5) }
    var displayedLargeFiles by remember { mutableStateOf(10) }
    var displayedThumbnails by remember { mutableStateOf(8) }
    val hasAnySelection = state.selectedFolderPaths.isNotEmpty() ||
        state.selectedDuplicatePaths.isNotEmpty() ||
        state.selectedLargeFilePaths.isNotEmpty() ||
        state.selectedThumbnailPaths.isNotEmpty()

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        controller.onSystemTrashResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(state.pendingSystemTrashRequest) {
        state.pendingSystemTrashRequest?.let { pendingIntent ->
            controller.markSystemTrashLaunched()
            systemTrashLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
            )
        }
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
            state.emptyFolders.take(displayedFolders).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedFolderPaths,
                    title = item.name,
                    detail = item.displayPath,
                    onChecked = { controller.toggleFolder(item.path, it) },
                )
            }
        }

        if (state.emptyFolders.size > displayedFolders) {
            OutlinedButton(onClick = { displayedFolders += 30 }) {
                Text(stringResource(R.string.audit_show_more))
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
            state.duplicateGroups.take(displayedGroups).forEach { group ->
                Text(
                    stringResource(R.string.pro_duplicate_group, group.files.size, formatBytes(group.sizeBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                group.files.take(displayedGroupFiles).forEach { file ->
                    ReviewCheckRow(
                        checked = file.path in state.selectedDuplicatePaths,
                        title = file.name,
                        detail = file.displayPath,
                        onChecked = { controller.toggleDuplicate(file.path, it) },
                    )
                }
            }
        }

        if (state.duplicateGroups.any { it.files.size > displayedGroupFiles }) {
            OutlinedButton(onClick = { displayedGroupFiles += 24 }) {
                Text(stringResource(R.string.audit_show_more))
            }
        }
        if (state.duplicateGroups.size > displayedGroups) {
            OutlinedButton(onClick = { displayedGroups += 24 }) {
                Text(stringResource(R.string.audit_show_more))
            }
        }
        if (state.largeFiles.isNotEmpty()) {
            Text(stringResource(R.string.pro_large_files), style = MaterialTheme.typography.labelLarge)
            state.largeFiles.take(displayedLargeFiles).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedLargeFilePaths,
                    title = item.name,
                    detail = "${formatBytes(item.sizeBytes)} · ${item.displayPath}",
                    onChecked = { controller.toggleLargeFile(item.path, it) },
                )
            }
        }

        if (state.largeFiles.size > displayedLargeFiles) {
            OutlinedButton(onClick = { displayedLargeFiles += 30 }) {
                Text(stringResource(R.string.audit_show_more))
            }
        }
        if (state.thumbnails.isNotEmpty()) {
            Text(stringResource(R.string.pro_thumbnail_cache), style = MaterialTheme.typography.labelLarge)
            state.thumbnails.take(displayedThumbnails).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedThumbnailPaths,
                    title = item.name,
                    detail = stringResource(R.string.pro_thumbnail_detail, item.fileCount, formatBytes(item.sizeBytes)),
                    onChecked = { controller.toggleThumbnail(item.path, it) },
                )
            }
        }

        if (state.thumbnails.size > displayedThumbnails) {
            OutlinedButton(onClick = { displayedThumbnails += 30 }) {
                Text(stringResource(R.string.audit_show_more))
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
            enabled = hasAnySelection && !state.resolving && !state.awaitingSystemTrash,
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
                val reviewItems = (
                    state.emptyFolders.filter { it.path in state.selectedFolderPaths }.map { it.path } +
                    state.duplicateGroups.flatMap { it.files }.filter {
                        it.path in state.selectedDuplicatePaths
                    }.map { it.path } +
                    state.largeFiles.filter { it.path in state.selectedLargeFilePaths }.map { it.path } +
                    state.thumbnails.filter { it.path in state.selectedThumbnailPaths }.map { it.path }
                ).distinct()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pro_confirm_body))
                    Text(stringResource(R.string.audit_review_selected_count, reviewItems.size))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(reviewItems) { itemPath ->
                            Text(itemPath, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResolveConfirmation = false
                        controller.resolve()
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onChecked,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
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
