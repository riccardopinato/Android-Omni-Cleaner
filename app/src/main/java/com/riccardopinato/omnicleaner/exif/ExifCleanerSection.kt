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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.launch

@Composable
fun ExifCleanerSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cleaner = remember(context.applicationContext) {
        FolderExifCleaner(context.applicationContext)
    }

    var sourceTree by remember { mutableStateOf<Uri?>(null) }
    var destinationTree by remember { mutableStateOf<Uri?>(null) }
    var recursive by remember { mutableStateOf(true) }
    var groups by remember { mutableStateOf(ExifGroup.entries.toSet()) }
    var scan by remember { mutableStateOf<ExifScan?>(null) }
    var export by remember { mutableStateOf<ExifExport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<ExifStatus?>(null) }

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
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.module_exif_privacy), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.exif_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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


private data class ExifStatus(
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
