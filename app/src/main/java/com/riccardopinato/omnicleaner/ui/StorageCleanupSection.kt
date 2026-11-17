package com.riccardopinato.omnicleaner.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as OutlinedButton
import androidx.compose.material3.Text
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.BigFileItem
import com.riccardopinato.omnicleaner.cleaner.DuplicateGroup
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.FolderItem
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.ManualMaintenance
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.launch
import java.util.Locale

private enum class CleanupDialog {
    EMPTY_FOLDERS,
    FILES,
}

/** Keep uncommitted selections in the Activity-root owner across detail pages. */
class CleanupSelectionUiState {
    var lastDuplicateScanEpoch: Long = 0L
    val selectedFolders = mutableStateOf<Set<String>>(emptySet())
    val selectedBigFiles = mutableStateOf<Set<String>>(emptySet())
    val selectedDuplicates = mutableStateOf<Set<String>>(emptySet())
}

@Composable
fun StorageCleanupSection(
    folders: List<FolderItem>,
    bigFiles: List<BigFileItem>,
    duplicateGroups: List<DuplicateGroup>,
    onCleanupFinished: () -> Unit,
    selectionState: CleanupSelectionUiState = remember { CleanupSelectionUiState() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember(context.applicationContext) {
        StorageCleanupEngine(context.applicationContext)
    }

    var visibleEmptyFolderLimit by remember { mutableStateOf(20) }
    var visibleBigFileLimit by remember { mutableStateOf(20) }
    var visibleDuplicateGroupLimit by remember { mutableStateOf(12) }
    var visibleDuplicateFilesLimit by remember { mutableStateOf(6) }
    var selectedFolders by selectionState.selectedFolders
    var selectedBigFiles by selectionState.selectedBigFiles
    var selectedDuplicates by selectionState.selectedDuplicates
    var dialog by remember { mutableStateOf<CleanupDialog?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingPreparation by remember { mutableStateOf<FileCleanupPreparation?>(null) }

    val androidTrashCancelled = stringResource(R.string.cleanup_android_trash_cancelled)
    val noSelection = stringResource(R.string.cleanup_no_selection)
    val systemTrashUnavailable = stringResource(R.string.cleanup_system_trash_unavailable)
    val androidTrashConfirmed = stringResource(R.string.cleanup_android_trash_confirmed)
    val movedTemplate = stringResource(R.string.cleanup_safe_trash_moved)
    val rejectedTemplate = stringResource(R.string.cleanup_rejected)
    val changedTemplate = stringResource(R.string.cleanup_changed)
    val blockedTemplate = stringResource(R.string.cleanup_blocked)
    val safeTrashNotReclaimed = stringResource(R.string.cleanup_safe_trash_not_reclaimed)
    val emptyResultTemplate = stringResource(R.string.cleanup_empty_result)

    fun localized(template: String, vararg args: Any): String =
        String.format(Locale.getDefault(), template, *args)

    fun cleanupMessage(
        preparation: FileCleanupPreparation,
        moved: Int,
        rejected: Int,
        androidConfirmed: Boolean,
    ): String = buildString {
        if (androidConfirmed) append(androidTrashConfirmed)
        if (moved > 0) {
            if (isNotEmpty()) append(" · ")
            append(localized(movedTemplate, moved))
        }
        if (rejected > 0) {
            if (isNotEmpty()) append(" · ")
            append(localized(rejectedTemplate, rejected))
        }
        if (preparation.staleFiles.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            append(localized(changedTemplate, preparation.staleFiles.size))
        }
        if (preparation.blockedFiles.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            append(localized(blockedTemplate, preparation.blockedFiles.size))
        }
        if (moved > 0) {
            if (isNotEmpty()) append(" · ")
            append(safeTrashNotReclaimed)
        }
    }

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val preparation = pendingPreparation
        pendingPreparation = null

        if (result.resultCode != Activity.RESULT_OK || preparation == null) {
            message = androidTrashCancelled
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val safeResult = engine.movePreparedSnapshotsToSafeTrash(preparation.safeTrashCandidates)
            message = cleanupMessage(
                preparation = preparation,
                moved = safeResult.moved,
                rejected = safeResult.rejected,
                androidConfirmed = true,
            )
            selectedBigFiles = emptySet()
            selectedDuplicates = emptySet()
            CleanupEventBus.publishCompleted()
            onCleanupFinished()
        }
    }

    fun executeSelectedFiles() {
        val big = engine.snapshotsForBigFiles(bigFiles, selectedBigFiles)
        val duplicates = engine.protectedDuplicateSnapshots(duplicateGroups, selectedDuplicates)
        val requested = big + duplicates
        if (requested.isEmpty()) {
            message = noSelection
            return
        }

        scope.launch {
            val preparation = engine.prepareSnapshots(requested)
            if (preparation.mediaUris.isNotEmpty()) {
                val request = engine.createSystemTrashRequest(preparation.mediaUris)
                if (request == null) {
                    message = systemTrashUnavailable
                    return@launch
                }
                pendingPreparation = preparation
                systemTrashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                return@launch
            }

            val safeResult = engine.movePreparedSnapshotsToSafeTrash(preparation.safeTrashCandidates)
            message = cleanupMessage(
                preparation = preparation,
                moved = safeResult.moved,
                rejected = safeResult.rejected,
                androidConfirmed = false,
            )
            selectedBigFiles = emptySet()
            selectedDuplicates = emptySet()
            if (safeResult.moved > 0) CleanupEventBus.publishCompleted()
            onCleanupFinished()
        }
    }

    val emptyFolders = folders.filter { it.status == FolderStatus.COMPLETELY_EMPTY }
    val hasAnyResult = folders.isNotEmpty() || bigFiles.isNotEmpty() || duplicateGroups.isNotEmpty()

    if (hasAnyResult) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.cleanup_review_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.cleanup_review_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (emptyFolders.isNotEmpty()) {
                    Text(stringResource(R.string.cleanup_empty_folders), style = MaterialTheme.typography.titleMedium)
                    emptyFolders.take(visibleEmptyFolderLimit).forEach { folder ->
                        SelectionRow(
                            checked = folder.path in selectedFolders,
                            label = folder.name,
                            detail = folder.displayPath,
                            onCheckedChange = { checked ->
                                selectedFolders = selectedFolders.withToggle(folder.path, checked)
                            },
                        )
                    }
                    if (emptyFolders.size > visibleEmptyFolderLimit) {
                        OutlinedButton(onClick = {
                            visibleEmptyFolderLimit = (visibleEmptyFolderLimit + 40).coerceAtMost(emptyFolders.size)
                        }) { Text(stringResource(R.string.audit_show_more)) }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedFolders = emptyFolders.mapTo(linkedSetOf()) { it.path }
                            },
                        ) {
                            Text(stringResource(R.string.cleanup_select_empty))
                        }
                        Button(
                            onClick = { dialog = CleanupDialog.EMPTY_FOLDERS },
                            enabled = selectedFolders.isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.cleanup_delete_selected))
                        }
                    }
                }

                if (bigFiles.isNotEmpty()) {
                    Text(stringResource(R.string.module_large_files), style = MaterialTheme.typography.titleMedium)
                    bigFiles.take(visibleBigFileLimit).forEach { file ->
                        SelectionRow(
                            checked = file.path in selectedBigFiles,
                            label = file.name,
                            detail = file.displayPath,
                            onCheckedChange = { checked ->
                                selectedBigFiles = selectedBigFiles.withToggle(file.path, checked)
                            },
                        )
                    }
                }

                if (bigFiles.size > visibleBigFileLimit) {
                    OutlinedButton(onClick = {
                        visibleBigFileLimit = (visibleBigFileLimit + 40).coerceAtMost(bigFiles.size)
                    }) { Text(stringResource(R.string.audit_show_more)) }
                }
                if (duplicateGroups.isNotEmpty()) {
                    Text(stringResource(R.string.module_duplicates), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedDuplicates = engine.selectDuplicatesExceptOldest(duplicateGroups)
                            },
                        ) {
                            Text(stringResource(R.string.action_keep_oldest))
                        }
                        OutlinedButton(
                            onClick = {
                                selectedDuplicates = engine.selectDuplicatesExceptNewest(duplicateGroups)
                            },
                        ) {
                            Text(stringResource(R.string.action_keep_newest))
                        }
                    }

                    duplicateGroups.take(visibleDuplicateGroupLimit).forEach { group ->
                        Text(
                            stringResource(
                                R.string.pro_duplicate_group,
                                group.files.size,
                                group.files.firstOrNull()?.formattedSize().orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        group.files.take(visibleDuplicateFilesLimit).forEach { file ->
                            SelectionRow(
                                checked = file.path in selectedDuplicates,
                                label = file.name,
                                detail = file.displayPath,
                                onCheckedChange = { checked ->
                                    selectedDuplicates = selectedDuplicates.withToggle(file.path, checked)
                                },
                            )
                        }
                    }
                    if (duplicateGroups.any { it.files.size > visibleDuplicateFilesLimit }) {
                        OutlinedButton(onClick = { visibleDuplicateFilesLimit += 24 }) {
                            Text(stringResource(R.string.audit_show_more))
                        }
                    }
                    if (duplicateGroups.size > visibleDuplicateGroupLimit) {
                        OutlinedButton(onClick = {
                            visibleDuplicateGroupLimit =
                                (visibleDuplicateGroupLimit + 24).coerceAtMost(duplicateGroups.size)
                        }) { Text(stringResource(R.string.audit_show_more)) }
                    }
                }

                if (selectedBigFiles.isNotEmpty() || selectedDuplicates.isNotEmpty()) {
                    Button(
                        onClick = { dialog = CleanupDialog.FILES },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.cleanup_move_selection_trash))
                    }
                }

                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { message = null }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }
    }

    when (dialog) {
        CleanupDialog.EMPTY_FOLDERS -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.cleanup_empty_confirm_title)) },
            text = {
                val selected = emptyFolders.filter { it.path in selectedFolders }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cleanup_empty_confirm_body))
                    Text(stringResource(R.string.audit_review_selected_count, selected.size))
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(selected, key = { it.path }) { folder ->
                            Text(folder.displayPath, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialog = null
                        val selected = emptyFolders
                            .filter { it.path in selectedFolders }
                            .map { it.file }
                        scope.launch {
                            val result = engine.deleteEmptyFolders(selected)
                            message = localized(emptyResultTemplate, result.removed, result.rejected)
                            selectedFolders = emptySet()
                            if (result.removed > 0) CleanupEventBus.publishCompleted()
                            onCleanupFinished()
                        }
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) }
            },
        )

        CleanupDialog.FILES -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.cleanup_confirm_title)) },
            text = {
                val chosenBig = bigFiles.filter { it.path in selectedBigFiles }
                val chosenDuplicates = duplicateGroups.flatMap { group ->
                    group.files.filter { it.path in selectedDuplicates }
                }
                val chosen = (chosenBig.map { it.path to it.displayPath } +
                    chosenDuplicates.map { it.path to it.displayPath })
                    .distinctBy { it.first }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cleanup_confirm_body))
                    Text(stringResource(R.string.audit_review_selected_count, chosen.size))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(chosen, key = { it.first }) { entry ->
                            Text(entry.second, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialog = null
                        executeSelectedFiles()
                    },
                ) { Text(stringResource(R.string.action_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) }
            },
        )

        null -> Unit
    }
}

@Composable
fun ManualMaintenanceCard() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val clipboardUnavailable = stringResource(R.string.manual_clipboard_unavailable)
    val clipboardCleared = stringResource(R.string.manual_clipboard_cleared)
    val clipboardRetry = stringResource(R.string.manual_clipboard_retry)
    val appManagementOpened = stringResource(R.string.manual_app_management_opened)
    val appManagementFailed = stringResource(R.string.manual_app_management_failed)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.manual_maintenance_title), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val activity = context as? Activity
                        message = when {
                            activity == null -> clipboardUnavailable
                            ManualMaintenance.clearClipboard(activity) -> clipboardCleared
                            else -> clipboardRetry
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.manual_clear_clipboard))
                }
                OutlinedButton(
                    onClick = {
                        message = if (ManualMaintenance.openAppManagement(context)) {
                            appManagementOpened
                        } else {
                            appManagementFailed
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.manual_app_management))
                }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SelectionRow(
    checked: Boolean,
    label: String,
    detail: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private fun Set<String>.withToggle(value: String, checked: Boolean): Set<String> =
    if (checked) this + value else this - value
