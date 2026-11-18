package com.riccardopinato.omnicleaner.exif

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import com.riccardopinato.omnicleaner.ui.theme.OmniActionButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.riccardopinato.omnicleaner.ui.theme.OmniSecondaryButton as OutlinedButton
import androidx.compose.material3.Text
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
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomeKind
import com.riccardopinato.omnicleaner.ui.redesign.OmniWelcomePanel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * Stable owner for SAF file/tree selections and in-flight export results while
 * navigating away from quick or batch tools. The coroutine scope is owned by the
 * top-level app, not the transient detail screen.
 */
internal class ExifWorkflowState {
    val singleSource = mutableStateOf<Uri?>(null)
    val sourceTree = mutableStateOf<Uri?>(null)
    val destinationTree = mutableStateOf<Uri?>(null)
    val recursive = mutableStateOf(true)
    val groups = mutableStateOf(setOf(ExifGroup.LOCATION))
    val showAdvancedExif = mutableStateOf(false)
    val scan = mutableStateOf<ExifScan?>(null)
    val export = mutableStateOf<ExifExport?>(null)
    val busy = mutableStateOf(false)
    val status = mutableStateOf<ExifStatus?>(null)
}

@Composable
internal fun ExifCleanerSection(
    advancedPage: Boolean? = null,
    workflow: ExifWorkflowState = remember { ExifWorkflowState() },
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    val context = LocalContext.current
    val cleaner = remember(context.applicationContext) {
        FolderExifCleaner(context.applicationContext)
    }

    var singleSource by workflow.singleSource
    var sourceTree by workflow.sourceTree
    var destinationTree by workflow.destinationTree
    var recursive by workflow.recursive
    var groups by workflow.groups
    var showAdvancedExif by workflow.showAdvancedExif
    var scan by workflow.scan
    var export by workflow.export
    var busy by workflow.busy
    var status by workflow.status


    // Default: pick one JPEG, then create a separate clean copy through SAF.
    // Existing directory batch export stays available without behavioral changes.
    val singleSourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            singleSource = uri
            export = null
            status = null
        }
    }

    val singleSavePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { destination ->
        val source = singleSource
        if (destination != null && source != null && !busy) {
            busy = true
            status = null
            scope.launch {
                try {
                    val result = cleaner.exportSingle(source, destination, groups.toSet())
                    export = result
                    status = if (result.created == 1 && result.failed == 0) {
                        ExifStatus(R.string.exif_export_status, listOf(1, 0))
                    } else {
                        ExifStatus(R.string.exif_export_failed)
                    }
                } finally {
                    busy = false
                }
            }
        }
    }

    val sourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        sourceTree = uri
        if (uri != null) cleaner.persistTreePermission(uri)
        scan = null
        export = null
    }

    val destinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        destinationTree = uri
        if (uri != null) cleaner.persistTreePermission(uri)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.exif_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Keep this visual guide out of the way after the user picks a
            // source, starts a batch flow or receives any meaningful result.
            if (advancedPage == null && singleSource == null && sourceTree == null &&
                scan == null && export == null && status == null &&
                !busy && !showAdvancedExif
            ) {
                OmniWelcomePanel(
                    kind = OmniWelcomeKind.PRIVACY,
                    title = stringResource(R.string.welcome_privacy_title),
                    message = stringResource(R.string.welcome_privacy_body),
                    highlights = listOf(
                        stringResource(R.string.welcome_privacy_step1),
                        stringResource(R.string.welcome_privacy_step2),
                        stringResource(R.string.welcome_privacy_step3),
                    ),
                )
            }

            if (advancedPage != true) {
            OutlinedButton(
                onClick = { singleSourcePicker.launch(arrayOf("image/jpeg")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.exif_choose_single_photo))
            }
            if (singleSource != null) {
                Button(
                    onClick = { singleSavePicker.launch("photo_clean.jpg") },
                    enabled = groups.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exif_save_single_copy))
                }
            }

            Text(
                stringResource(R.string.audit_privacy_quick),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            } // Quick JPEG workflow
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (advancedPage == null) {
                OutlinedButton(
                    onClick = { showAdvancedExif = !showAdvancedExif },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.audit_privacy_advanced) +
                        if (showAdvancedExif) " −" else " +")
                }
            }
            if (advancedPage == true || (advancedPage == null && showAdvancedExif)) {
            OutlinedButton(
                onClick = { sourcePicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (sourceTree == null) stringResource(R.string.exif_choose_photo_folder)
                    else stringResource(R.string.exif_change_photo_folder),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = recursive,
                        role = Role.Checkbox,
                        onValueChange = { recursive = it },
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = recursive,
                    onCheckedChange = null,
                )
                Text(stringResource(R.string.exif_include_subfolders))
            }

            ExifGroup.entries.forEach { group ->
                val selected = group in groups
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = selected,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                groups = if (checked) groups + group else groups - group
                                scan = null
                                export = null
                            },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                    )
                    Text(exifGroupLabel(group))
                }
            }

            Button(
                onClick = {
                    val source = sourceTree ?: return@Button
                    busy = true
                    status = null
                    scope.launch {
                        runCatching {
                            cleaner.scan(
                                trees = listOf(source),
                                recursive = recursive,
                                groups = groups,
                            )
                        }.onSuccess {
                            scan = it
                            status = ExifStatus(R.string.exif_scan_status, listOf(it.candidates.size))
                        }.onFailure {
                            status = ExifStatus(R.string.exif_scan_failed)
                        }
                        busy = false
                    }
                },
                enabled = sourceTree != null && groups.isNotEmpty() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.exif_scan))
            }

            scan?.let { result ->
                Text(
                    stringResource(R.string.exif_found_summary, result.candidates.size, result.skipped, result.failures),
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedButton(
                    onClick = { destinationPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (destinationTree == null) stringResource(R.string.exif_choose_export_folder)
                        else stringResource(R.string.exif_change_export_folder),
                    )
                }

                Button(
                    onClick = {
                        val destination = destinationTree ?: return@Button
                        busy = true
                        scope.launch {
                            runCatching {
                                cleaner.export(
                                    scan = result,
                                    destination = destination,
                                    onProgress = { done, total ->
                                        status = ExifStatus(R.string.exif_export_progress, listOf(done, total))
                                    },
                                    onIncomplete = { name ->
                                        status = ExifStatus(R.string.exif_copy_verification_failed, listOf(name))
                                    },
                                )
                            }.onSuccess {
                                export = it
                                status = ExifStatus(R.string.exif_export_status, listOf(it.created, it.failed))
                            }.onFailure {
                                status = ExifStatus(R.string.exif_export_failed)
                            }
                            busy = false
                        }
                    },
                    enabled = destinationTree != null &&
                        result.candidates.isNotEmpty() &&
                        !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exif_export_clean_copies))
                }
            }

            } // End advanced folder/batch tools.

            export?.let { result ->
                Text(
                    stringResource(R.string.exif_export_summary, result.created, result.failed),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            status?.let { message ->
                Text(
                    stringResource(message.stringRes, *message.args.toTypedArray()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


internal data class ExifStatus(
    val stringRes: Int,
    val args: List<Any> = emptyList(),
)

@Composable
private fun exifGroupLabel(group: ExifGroup): String =
    stringResource(
        when (group) {
            ExifGroup.LOCATION -> R.string.exif_group_location
            ExifGroup.DATES -> R.string.exif_group_dates
            ExifGroup.DEVICE -> R.string.exif_group_device
            ExifGroup.COMMENTS -> R.string.exif_group_comments
        },
    )
