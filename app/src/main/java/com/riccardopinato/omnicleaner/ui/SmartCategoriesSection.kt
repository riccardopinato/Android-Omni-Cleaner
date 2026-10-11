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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.CleanupFileSnapshot
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.SmartCategory
import com.riccardopinato.omnicleaner.cleaner.SmartCategoryItem
import com.riccardopinato.omnicleaner.cleaner.SmartCategoryScan
import com.riccardopinato.omnicleaner.cleaner.SmartCategoryScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SmartCategoriesSection(
    onCleanupFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember { SmartCategoryScanner() }
    val engine = remember(context.applicationContext) {
        StorageCleanupEngine(context.applicationContext)
    }

    var scanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SmartCategoryScan?>(null) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmCleanup by remember { mutableStateOf(false) }
    var pendingPreparation by remember { mutableStateOf<FileCleanupPreparation?>(null) }

    val androidTrashCancelledMessage =
        stringResource(R.string.smart_categories_android_trash_cancelled)
    val noSelectionMessage =
        stringResource(R.string.smart_categories_no_selection)
    val trashUnavailableMessage =
        stringResource(R.string.smart_categories_trash_unavailable)

    val androidTrashConfirmedMessage = stringResource(R.string.cleanup_android_trash_confirmed)
    val movedTemplate = stringResource(R.string.cleanup_safe_trash_moved)
    val rejectedTemplate = stringResource(R.string.cleanup_rejected)
    val changedTemplate = stringResource(R.string.cleanup_changed)
    val blockedTemplate = stringResource(R.string.cleanup_blocked)
    val safeTrashNotReclaimed = stringResource(R.string.cleanup_safe_trash_not_reclaimed)

    fun scan() {
        if (scanning) return
        scope.launch {
            scanning = true
            message = null
            runCatching {
                scanner.scan(StorageAccess.primaryRoot())
            }.onSuccess { scan ->
                result = scan
                selectedPaths = emptySet()
            }.onFailure { error ->
                message = error.message ?: error.javaClass.simpleName
            }
            scanning = false
        }
    }

    fun finishPreparedCleanup(preparation: FileCleanupPreparation) {
        scope.launch {
            val safeResult = engine.movePreparedSnapshotsToSafeTrash(
                preparation.safeTrashCandidates,
            )
            selectedPaths = emptySet()
            message = buildCleanupMessage(
                movedToSafeTrash = safeResult.moved,
                rejected = safeResult.rejected,
                stale = preparation.staleFiles.size,
                blocked = preparation.blockedFiles.size,
                androidTrashWasConfirmed = preparation.mediaUris.isNotEmpty(),
                androidTrashConfirmedMessage = androidTrashConfirmedMessage,
                movedTemplate = movedTemplate,
                rejectedTemplate = rejectedTemplate,
                changedTemplate = changedTemplate,
                blockedTemplate = blockedTemplate,
                safeTrashNotReclaimed = safeTrashNotReclaimed,
            )
            if (safeResult.moved > 0 || preparation.mediaUris.isNotEmpty()) {
                CleanupEventBus.publishCompleted()
            }
            onCleanupFinished()
            runCatching {
                scanner.scan(StorageAccess.primaryRoot())
            }.onSuccess { refreshed ->
                result = refreshed
            }
        }
    }

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        val preparation = pendingPreparation
        pendingPreparation = null
        if (activityResult.resultCode != Activity.RESULT_OK || preparation == null) {
            message = androidTrashCancelledMessage
        } else {
            finishPreparedCleanup(preparation)
        }
    }

    fun prepareSelectedCleanup() {
        val scan = result ?: return
        val snapshots = scan.items
            .asSequence()
            .filter { it.path in selectedPaths }
            .map { item ->
                CleanupFileSnapshot(
                    file = item.file,
                    expectedBytes = item.sizeBytes,
                    expectedModified = item.lastModified,
                )
            }
            .toList()

        if (snapshots.isEmpty()) {
            message = noSelectionMessage
            return
        }

        scope.launch {
            val preparation = engine.prepareSnapshots(snapshots)
            if (preparation.mediaUris.isNotEmpty()) {
                val request = engine.createSystemTrashRequest(preparation.mediaUris)
                if (request == null) {
                    message = trashUnavailableMessage
                    return@launch
                }
                pendingPreparation = preparation
                systemTrashLauncher.launch(
                    IntentSenderRequest.Builder(request.intentSender).build(),
                )
            } else {
                finishPreparedCleanup(preparation)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.smart_categories_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.smart_categories_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.smart_categories_scanning),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Button(
                    onClick = ::scan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.smart_categories_scan))
                }
            }

            result?.let { scan ->
                if (scan.items.isEmpty()) {
                    Text(
                        stringResource(R.string.smart_categories_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.smart_categories_candidate_summary,
                            scan.items.size,
                            formatSmartBytes(scan.totalUniqueBytes),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmartCategory.entries.forEach { category ->
                            val items = scan.itemsFor(category)
                            if (items.isNotEmpty()) {
                                Card {
                                    Text(
                                        text = categoryLabel(category) + " · " +
                                            items.size + " · " +
                                            formatSmartBytes(scan.bytesFor(category)),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    scan.items.take(MAX_VISIBLE_CANDIDATES).forEach { item ->
                        SmartCandidateRow(
                            item = item,
                            checked = item.path in selectedPaths,
                            onCheckedChange = { checked ->
                                selectedPaths = selectedPaths.withToggle(item.path, checked)
                            },
                        )
                    }

                    if (scan.items.size > MAX_VISIBLE_CANDIDATES) {
                        Text(
                            stringResource(
                                R.string.smart_categories_more_candidates,
                                scan.items.size - MAX_VISIBLE_CANDIDATES,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (selectedPaths.isNotEmpty()) {
                        Button(
                            onClick = { confirmCleanup = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    R.string.smart_categories_cleanup_selected,
                                    selectedPaths.size,
                                ),
                            )
                        }
                    }

                    if (scan.unreadable > 0 || scan.excludedDirectories > 0) {
                        Text(
                            stringResource(
                                R.string.smart_categories_partial_scan,
                                scan.unreadable,
                                scan.excludedDirectories,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            message?.let { text ->
                Text(text, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { message = null }) {
                    Text(stringResource(R.string.smart_categories_close))
                }
            }
        }
    }

    if (confirmCleanup) {
        AlertDialog(
            onDismissRequest = { confirmCleanup = false },
            title = { Text(stringResource(R.string.smart_categories_confirm_title)) },
            text = { Text(stringResource(R.string.smart_categories_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmCleanup = false
                        prepareSelectedCleanup()
                    },
                ) {
                    Text(stringResource(R.string.smart_categories_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCleanup = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SmartCandidateRow(
    item: SmartCategoryItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            val oldDownloads = if (SmartCategory.OLD_DOWNLOADS in item.categories) {
                stringResource(R.string.smart_category_old_downloads)
            } else {
                null
            }
            val installers = if (SmartCategory.INSTALLERS in item.categories) {
                stringResource(R.string.smart_category_installers)
            } else {
                null
            }
            val archives = if (SmartCategory.ARCHIVES in item.categories) {
                stringResource(R.string.smart_category_archives)
            } else {
                null
            }
            val screenshots = if (SmartCategory.SCREENSHOTS in item.categories) {
                stringResource(R.string.smart_category_screenshots)
            } else {
                null
            }
            val categoryText = listOfNotNull(
                oldDownloads,
                installers,
                archives,
                screenshots,
            ).joinToString(" · ")

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = categoryText + " · " + formatSmartBytes(item.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = item.displayPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun categoryLabel(category: SmartCategory): String = when (category) {
    SmartCategory.OLD_DOWNLOADS -> stringResource(R.string.smart_category_old_downloads)
    SmartCategory.INSTALLERS -> stringResource(R.string.smart_category_installers)
    SmartCategory.ARCHIVES -> stringResource(R.string.smart_category_archives)
    SmartCategory.SCREENSHOTS -> stringResource(R.string.smart_category_screenshots)
}

private fun buildCleanupMessage(
    movedToSafeTrash: Int,
    rejected: Int,
    stale: Int,
    blocked: Int,
    androidTrashWasConfirmed: Boolean,
    androidTrashConfirmedMessage: String,
    movedTemplate: String,
    rejectedTemplate: String,
    changedTemplate: String,
    blockedTemplate: String,
    safeTrashNotReclaimed: String,
): String = buildString {
    fun formatted(template: String, value: Int): String =
        String.format(Locale.getDefault(), template, value)

    if (androidTrashWasConfirmed) append(androidTrashConfirmedMessage)
    if (movedToSafeTrash > 0) {
        if (isNotEmpty()) append(" · ")
        append(formatted(movedTemplate, movedToSafeTrash))
    }
    if (stale > 0) {
        if (isNotEmpty()) append(" · ")
        append(formatted(changedTemplate, stale))
    }
    if (blocked > 0) {
        if (isNotEmpty()) append(" · ")
        append(formatted(blockedTemplate, blocked))
    }
    if (rejected > 0) {
        if (isNotEmpty()) append(" · ")
        append(formatted(rejectedTemplate, rejected))
    }
    if (movedToSafeTrash > 0) {
        if (isNotEmpty()) append(" · ")
        append(safeTrashNotReclaimed)
    }
}

private fun formatSmartBytes(bytes: Long): String {
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

private fun Set<String>.withToggle(value: String, checked: Boolean): Set<String> =
    if (checked) this + value else this - value

private const val MAX_VISIBLE_CANDIDATES = 24
