package com.riccardopinato.omnicleaner.ui

import android.content.Context
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.cleaner.AppResidueItem
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.ThumbnailCacheItem
import com.riccardopinato.omnicleaner.cleaner.ThumbnailFileCleanup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class MaintenanceState(
    val running: Boolean = false,
    val operation: String? = null,
    val thumbnails: List<ThumbnailCacheItem> = emptyList(),
    val residues: List<AppResidueItem> = emptyList(),
    val residueVisibilityWarning: Boolean = false,
    val thumbnailSearchQuery: String = "",
    val residueSearchQuery: String = "",
    val message: String? = null,
) {
    val visibleThumbnails: List<ThumbnailCacheItem>
        get() {
            val q = thumbnailSearchQuery.trim().lowercase()
            if (q.isBlank()) return thumbnails
            return thumbnails.filter {
                it.name.lowercase().contains(q) || it.path.lowercase().contains(q)
            }
        }

    val visibleResidues: List<AppResidueItem>
        get() {
            val q = residueSearchQuery.trim().lowercase()
            if (q.isBlank()) return residues
            return residues.filter {
                it.name.lowercase().contains(q) || it.path.lowercase().contains(q)
            }
        }
}



private class MaintenanceController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val scanner: FileScanner = FileScanner(),
    private val thumbnailCleanup: ThumbnailFileCleanup = ThumbnailFileCleanup(),
) {
    var state by mutableStateOf(MaintenanceState())
        private set

    private var job: Job? = null

    fun scanThumbnails() = run("Cache miniature") {
        state = state.copy(
            thumbnails = scanner.scanThumbnailCache(StorageAccess.primaryRoot()),
        )
    }

    fun scanResidues() = run("Residui app") {
        val result = scanner.scanAppResidues(context, StorageAccess.primaryRoot())
        state = state.copy(
            residues = result.items,
            residueVisibilityWarning = result.packageVisibilityMayBeRestricted,
        )
    }

    fun cleanThumbnail(item: ThumbnailCacheItem) = run("Pulizia cache miniature") {
        val result = thumbnailCleanup.clean(item.inventory)
        val refreshed = scanner.scanThumbnailCache(StorageAccess.primaryRoot())
        state = state.copy(
            thumbnails = refreshed,
            message = "${result.removed} file rimossi; ${result.rejected} rifiutati perché cambiati o non verificabili.",
        )
        if (result.removed > 0) CleanupEventBus.publishCompleted()
    }

    fun updateThumbnailSearchQuery(query: String) {
        state = state.copy(thumbnailSearchQuery = query)
    }

    fun updateResidueSearchQuery(query: String) {
        state = state.copy(residueSearchQuery = query)
    }

    fun clearMessage() {
        state = state.copy(message = null)
    }

    private fun run(label: String, block: suspend () -> Unit) {
        if (!StorageAccess.hasBroadAccess(context)) {
            state = state.copy(message = "Serve l’accesso alla memoria prima di eseguire questa analisi.")
            return
        }
        job?.cancel()
        job = scope.launch {
            state = state.copy(running = true, operation = label, message = null)
            runCatching { block() }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        state = state.copy(message = error.message ?: error.javaClass.simpleName)
                    }
                }
            state = state.copy(running = false, operation = null)
        }
    }
}

@Composable
fun MaintenanceSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context.applicationContext, scope) {
        MaintenanceController(context.applicationContext, scope)
    }
    val state = controller.state
    var pendingThumbnail by remember { mutableStateOf<ThumbnailCacheItem?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Pulizia avanzata", style = MaterialTheme.typography.titleLarge)
            Text(
                "Modalità Residui app e Cache miniature recuperate dall’APK originale.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = controller::scanThumbnails, enabled = !state.running) {
                    Text("Cache miniature")
                }
                OutlinedButton(onClick = controller::scanResidues, enabled = !state.running) {
                    Text("Residui app")
                }
            }

            OutlinedTextField(
                value = state.thumbnailSearchQuery,
                onValueChange = controller::updateThumbnailSearchQuery,
                label = { Text("Cerca cache miniature") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.residueSearchQuery,
                onValueChange = controller::updateResidueSearchQuery,
                label = { Text("Cerca residui app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.running) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(state.operation.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }

            state.visibleThumbnails.take(8).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.displayPath, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${item.formattedSize()} · ${item.fileCount} file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { pendingThumbnail = item }, enabled = !state.running) {
                        Text("Pulisci dopo verifica")
                    }
                }
            }

            if (state.visibleResidues.isNotEmpty()) {
                Text("Candidati residui app", style = MaterialTheme.typography.titleMedium)
                if (state.residueVisibilityWarning) {
                    Text(
                        "Android può limitare la visibilità dei pacchetti installati: questi elementi sono candidati da revisionare, non prove automatiche di app disinstallate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.visibleResidues.take(10).forEach { item ->
                    Text(
                        "${item.name} · ${item.formattedSize()} · ${item.fileCount} file",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            state.message?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = controller::clearMessage) {
                    Text("Chiudi")
                }
            }
        }
    }

    pendingThumbnail?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingThumbnail = null },
            title = { Text("Conferma pulizia cache") },
            text = {
                Text(
                    "Verranno rimossi solo i file identici all’inventario appena scansionato " +
                        "(dimensione, data modifica e SHA-256). I file cambiati verranno rifiutati.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingThumbnail = null
                        controller.cleanThumbnail(item)
                    },
                ) {
                    Text("Pulisci")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingThumbnail = null }) {
                    Text("Annulla")
                }
            },
        )
    }
}
