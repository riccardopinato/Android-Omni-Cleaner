package com.riccardopinato.omnicleaner.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus

@Composable
fun GalleryCleanerSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context.applicationContext, scope) {
        GalleryController(context.applicationContext, scope)
    }

    var state by remember { mutableStateOf(controller.state) }
    var showTrashConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val dispose = controller.observe { state = it }
        onDispose(dispose)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        controller.refreshAccess()
    }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val success = result.resultCode == Activity.RESULT_OK
        controller.onTrashResult(success)
        if (success) CleanupEventBus.publishCompleted()
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
            Text("Gallery Cleaner", style = MaterialTheme.typography.titleLarge)
            Text(
                "Keep or mark media for Android system trash. Nothing is removed without confirmation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.hasAccess) {
                Button(
                    onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow photos and videos")
                }
            } else {
                if (state.accessHealth.level == GalleryAccessLevel.PARTIAL) {
                    Text(
                        "Accesso limitato: Gallery Cleaner vede solo i contenuti autorizzati da Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(GalleryAccess.requiredPermissions()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Modifica selezione foto e video")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GalleryMediaFilter.entries.forEach { filter ->
                        if (state.filter == filter) {
                            Button(
                                onClick = { controller.setFilter(filter) },
                                enabled = filter.isAvailableFor(state.accessHealth),
                            ) {
                                Text(filter.label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { controller.setFilter(filter) },
                                enabled = filter.isAvailableFor(state.accessHealth),
                            ) {
                                Text(filter.label)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = controller::cycleSortOrder,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ordine: ${state.sortOrder.label}")
                }

                Button(
                    onClick = { controller.load() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.items.isEmpty()) "Carica galleria" else "Ricarica galleria")
                }
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.currentItem?.let { current ->
                GalleryMediaPreview(
                    item = current,
                    onKeep = controller::keepCurrent,
                    onTrash = controller::trashCurrent,
                )
                Text(current.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(current.formattedSize)
                        append(" · ")
                        append(current.formattedDate)
                        current.album?.takeIf { it.isNotBlank() }?.let {
                            append(" · ")
                            append(it)
                        }
                        if (current.isVideo && current.formattedDuration.isNotBlank()) {
                            append(" · ")
                            append(current.formattedDuration)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = controller::keepCurrent) {
                        Text("Tieni")
                    }
                    Button(onClick = controller::trashCurrent) {
                        Text("Cestino")
                    }
                    if (state.history.isNotEmpty()) {
                        OutlinedButton(onClick = controller::undoLastAction) {
                            Text("Annulla")
                        }
                    }
                }
            }

            if (state.loadingNextPage) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Caricamento pagina successiva…", style = MaterialTheme.typography.bodySmall)
            }

            if (state.hasMore && !state.loadingNextPage) {
                OutlinedButton(
                    onClick = controller::loadNextPage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Carica altri elementi")
                }
            }

            if (state.items.isNotEmpty() && state.currentItem == null && !state.hasMore) {
                Text("Revisione completata per tutti gli elementi caricati.")
            }

            if (state.selectedForTrash.isNotEmpty()) {
                Text(
                    "${state.selectedForTrash.size} elementi selezionati per il cestino",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { showTrashConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sposta nel cestino di Android")
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = controller::clearError) {
                    Text("Chiudi")
                }
            }
        }
    }

    if (showTrashConfirmation) {
        AlertDialog(
            onDismissRequest = { showTrashConfirmation = false },
            title = { Text("Conferma cestino") },
            text = {
                Text(
                    "Android mostrerà una seconda conferma di sistema. " +
                        "Gli elementi verranno spostati nel cestino multimediale, non eliminati direttamente.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTrashConfirmation = false
                        val pendingIntent = controller.createTrashRequest() ?: return@TextButton
                        trashLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                        )
                    },
                ) {
                    Text("Continua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirmation = false }) {
                    Text("Annulla")
                }
            },
        )
    }
}


private fun GalleryMediaFilter.isAvailableFor(health: GalleryAccessHealth): Boolean = when (this) {
    GalleryMediaFilter.ALL -> health.canReadImages && health.canReadVideos
    GalleryMediaFilter.IMAGES -> health.canReadImages
    GalleryMediaFilter.VIDEOS -> health.canReadVideos
}
