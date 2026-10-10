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
import androidx.compose.ui.unit.dp
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
    var message by remember { mutableStateOf<String?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val canRestore = StorageAccess.hasBroadAccess(context)

    suspend fun reload() {
        val snapshot = withContext(Dispatchers.IO) {
            manager.listEntries() to manager.getTrashSize()
        }
        entries = snapshot.first
        trashBytes = snapshot.second
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
            Text("Safe Trash", style = MaterialTheme.typography.titleLarge)
            Text(
                "I file qui dentro occupano ancora spazio. Puoi ripristinarli senza sovrascrivere file esistenti oppure eliminarli definitivamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "${entries.size} elementi · ${formatSafeTrashBytes(trashBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (entries.isEmpty()) {
                    Text("Safe Trash vuoto.", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (!canRestore) {
                        Text(
                            "Per ripristinare nel percorso originale riattiva Pulizia Profonda. Puoi comunque svuotare il cestino privato.",
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
                                                "Ripristinato: ${entry.originalName}"
                                            SafeTrashRestoreStatus.DESTINATION_EXISTS ->
                                                "Ripristino rifiutato: esiste già un file nel percorso originale."
                                            SafeTrashRestoreStatus.PAYLOAD_INVALID ->
                                                "Ripristino rifiutato: il payload non supera la verifica d’integrità."
                                            SafeTrashRestoreStatus.UNSAFE_DESTINATION ->
                                                "Ripristino rifiutato: destinazione non sicura."
                                            SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE ->
                                                "Destinazione originale non disponibile."
                                            SafeTrashRestoreStatus.INSUFFICIENT_SPACE ->
                                                "Spazio insufficiente per il ripristino."
                                            SafeTrashRestoreStatus.COPY_FAILED ->
                                                "Ripristino non completato: la copia verificata è fallita."
                                            SafeTrashRestoreStatus.ENTRY_NOT_FOUND ->
                                                "Elemento Safe Trash non più disponibile."
                                        }
                                        reload()
                                    }
                                },
                            ) {
                                Text("Ripristina")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !loading,
                            onClick = {
                                loading = true
                                scope.launch { reload() }
                            },
                        ) {
                            Text("Aggiorna")
                        }
                        Button(
                            enabled = !loading && trashBytes > 0L,
                            onClick = { confirmEmpty = true },
                        ) {
                            Text("Svuota definitivamente")
                        }
                    }
                }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { message = null }) {
                    Text("Chiudi")
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Svuotare Safe Trash?") },
            text = {
                Text(
                    "Questa azione elimina definitivamente le copie private. Solo dopo lo svuotamento questi byte possono essere considerati realmente liberati.",
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
                                "Safe Trash svuotato · ${formatSafeTrashBytes(reclaimed)} liberati definitivamente."
                            } else {
                                "Svuotamento parziale · ${formatSafeTrashBytes(reclaimed)} liberati; alcuni elementi sono stati lasciati per sicurezza."
                            }
                            reload()
                            if (reclaimed > 0L) CleanupEventBus.publishCompleted()
                        }
                    },
                ) {
                    Text("Elimina definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text("Annulla")
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
