package com.riccardopinato.omnicleaner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.riccardopinato.omnicleaner.cleaner.SafeTrashEntry
import com.riccardopinato.omnicleaner.cleaner.SafeTrashManager
import com.riccardopinato.omnicleaner.cleaner.SafeTrashRestoreStatus
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SafeTrashSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context.applicationContext) {
        SafeTrashManager(context.applicationContext)
    }

    var entries by remember { mutableStateOf<List<SafeTrashEntry>>(emptyList()) }
    var trashBytes by remember { mutableStateOf(0L) }
    var loading by remember { mutableStateOf(true) }
    var persistentAvailable by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<SafeTrashMessage?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val canRestore = StorageAccess.hasBroadAccess(context)

    suspend fun reload() {
        val snapshot = withContext(Dispatchers.IO) {
            Triple(
                manager.listEntries(),
                manager.getTrashSize(),
                manager.isPersistentTrashAvailable(),
            )
        }
        entries = snapshot.first
        trashBytes = snapshot.second
        persistentAvailable = snapshot.third
        loading = false
    }

    LaunchedEffect(manager) {
        reload()
        CleanupEventBus.events.collectLatest {
            reload()
        }
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
            Text(stringResource(R.string.module_safe_trash), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.safe_trash_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.safe_trash_persistent_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!loading && !persistentAvailable) {
                Text(
                    stringResource(R.string.safe_trash_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    stringResource(R.string.safe_trash_summary, entries.size, formatSafeTrashBytes(trashBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (entries.isEmpty()) {
                    Text(stringResource(R.string.safe_trash_empty), style = MaterialTheme.typography.bodySmall)
                } else {
                    if (!canRestore) {
                        Text(
                            stringResource(R.string.safe_trash_restore_access),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    entries.take(12).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.originalName, maxLines = 1)
                                Text(
                                    "${formatSafeTrashBytes(entry.sizeBytes)} · ${entry.originalPath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            OutlinedButton(
                                enabled = canRestore && !loading,
                                onClick = {
                                    loading = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            manager.restore(entry.id)
                                        }
                                        message = when (result.status) {
                                            SafeTrashRestoreStatus.RESTORED ->
                                                SafeTrashMessage(R.string.safe_trash_restored, listOf(entry.originalName))
                                            SafeTrashRestoreStatus.DESTINATION_EXISTS ->
                                                SafeTrashMessage(R.string.safe_trash_restore_destination_exists)
                                            SafeTrashRestoreStatus.PAYLOAD_INVALID ->
                                                SafeTrashMessage(R.string.safe_trash_restore_payload_invalid)
                                            SafeTrashRestoreStatus.UNSAFE_DESTINATION ->
                                                SafeTrashMessage(R.string.safe_trash_restore_unsafe_destination)
                                            SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE ->
                                                SafeTrashMessage(R.string.safe_trash_restore_destination_unavailable)
                                            SafeTrashRestoreStatus.INSUFFICIENT_SPACE ->
                                                SafeTrashMessage(R.string.safe_trash_restore_insufficient_space)
                                            SafeTrashRestoreStatus.COPY_FAILED ->
                                                SafeTrashMessage(R.string.safe_trash_restore_copy_failed)
                                            SafeTrashRestoreStatus.ENTRY_NOT_FOUND ->
                                                SafeTrashMessage(R.string.safe_trash_restore_entry_missing)
                                        }
                                        reload()
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.action_restore))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !loading,
                            onClick = {
                                loading = true
                                scope.launch { reload() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.refresh))
                        }
                        Button(
                            enabled = !loading && trashBytes > 0L,
                            onClick = { confirmEmpty = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.safe_trash_empty_permanently))
                        }
                    }
                }
            }

            message?.let { value ->
                Text(
                    stringResource(value.stringRes, *value.args.toTypedArray()),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { message = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(stringResource(R.string.safe_trash_empty_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.safe_trash_empty_confirm_body),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEmpty = false
                        loading = true
                        scope.launch {
                            val before = withContext(Dispatchers.IO) { manager.getTrashSize() }
                            val completed = withContext(Dispatchers.IO) { manager.emptyTrash() }
                            val after = withContext(Dispatchers.IO) { manager.getTrashSize() }
                            val reclaimed = (before - after).coerceAtLeast(0L)
                            message = if (completed) {
                                SafeTrashMessage(
                                    R.string.safe_trash_emptied,
                                    listOf(formatSafeTrashBytes(reclaimed)),
                                )
                            } else {
                                SafeTrashMessage(
                                    R.string.safe_trash_partial_empty,
                                    listOf(formatSafeTrashBytes(reclaimed)),
                                )
                            }
                            reload()
                            if (reclaimed > 0L) CleanupEventBus.publishCompleted()
                        }
                    },
                ) {
                    Text(stringResource(R.string.safe_trash_delete_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun formatSafeTrashBytes(bytes: Long): String {
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


private data class SafeTrashMessage(
    val stringRes: Int,
    val args: List<Any> = emptyList(),
)
