package com.riccardopinato.omnicleaner.monetization

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
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
        Text("Review Pro", style = MaterialTheme.typography.titleMedium)
        Text(
            "Candidati rilevati: ${formatBytes(state.totalRecoverableBytes)}. " +
                "La stima non equivale a spazio già liberato.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.emptyFolders.isNotEmpty()) {
            Text("Cartelle realmente vuote", style = MaterialTheme.typography.labelLarge)
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
            Text("Duplicati", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = controller::selectDuplicateKeepOldest) {
                    Text("Keep oldest")
                }
                OutlinedButton(onClick = controller::selectDuplicateKeepNewest) {
                    Text("Keep newest")
                }
            }
            state.duplicateGroups.take(6).forEach { group ->
                Text(
                    "${group.files.size} copie · ${formatBytes(group.sizeBytes)} ciascuna",
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
            Text("File grandi — selezione manuale", style = MaterialTheme.typography.labelLarge)
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
            Text("Cache e miniature verificate", style = MaterialTheme.typography.labelLarge)
            state.thumbnails.take(8).forEach { item ->
                ReviewCheckRow(
                    checked = item.path in state.selectedThumbnailPaths,
                    title = item.name,
                    detail = "${item.fileCount} file · ${formatBytes(item.sizeBytes)}",
                    onChecked = { controller.toggleThumbnail(item.path, it) },
                )
            }
        }

        if (state.residues.isNotEmpty()) {
            Text("Residui accessibili — solo review", style = MaterialTheme.typography.labelLarge)
            Text(
                "Android può limitare la visibilità dei pacchetti installati. " +
                    "Per evitare falsi positivi questi candidati non vengono rimossi automaticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            state.residues.take(8).forEach { item ->
                Text(
                    "${item.name} · ${formatBytes(item.sizeBytes)} · ${item.displayPath}",
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
            Text("Risolvi selezione Pro")
        }

        state.message?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = controller::clearMessage) {
                Text("Chiudi")
            }
        }

        state.error?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showResolveConfirmation) {
        AlertDialog(
            onDismissRequest = { showResolveConfirmation = false },
            title = { Text("Confermare la sequenza Pro?") },
            text = {
                Text(
                    "Cartelle vuote e thumbnail verranno ricontrollate prima della rimozione. " +
                        "I duplicati manterranno almeno una copia. I media selezionati passeranno " +
                        "dal cestino Android con una seconda conferma di sistema. I residui app " +
                        "resteranno invariati.",
                )
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
                    Text("Continua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveConfirmation = false }) {
                    Text("Annulla")
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
        Column {
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
