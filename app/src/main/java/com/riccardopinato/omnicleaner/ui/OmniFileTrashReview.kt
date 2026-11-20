package com.riccardopinato.omnicleaner.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.cleaner.CleanupFileSnapshot
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Reusable, conservative selection -> review -> Android Trash / SafeTrash flow.
 * A checkbox never mutates a file. The user approves the exact list first.
 * MediaStore media must use OS Trash confirmation on supported Android versions.
 * Non-media enters reversible app SafeTrash only after snapshot validation.
 */
@Composable
internal fun OmniFileTrashReview(
    selected: List<CleanupFileSnapshot>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = remember(context.applicationContext) {
        StorageCleanupEngine(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<FileCleanupPreparation?>(null) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { response ->
        val prepared = pending
        pending = null
        if (response.resultCode == Activity.RESULT_OK && prepared != null) {
            scope.launch {
                try {
                    val result = engine.movePreparedSnapshotsToSafeTrash(prepared.safeTrashCandidates)
                    message = "File nel cestino Android: ${prepared.mediaUris.size} · Cestino reversibile: ${result.moved} · Rifiutati: ${result.rejected}"
                    CleanupEventBus.publishCompleted()
                    onFinished()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = "Spostamento incompleto. I file non confermati restano invariati."
                } finally {
                    running = false
                }
            }
        } else {
            message = "Operazione annullata. Nessun file rimosso."
            running = false
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { confirm = true },
            enabled = selected.isNotEmpty() && !running,
            modifier = Modifier.fillMaxWidth().testTag("omni_file_trash_selected"),
        ) { Text("Sposta nel cestino (${selected.size})") }
        if (running) CircularProgressIndicator()
        message?.let { Text(it) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Conferma i file selezionati") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Solo i file verificati saranno spostati. Le foto e i video richiedono l'autorizzazione Android.")
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(selected, key = { it.file.absolutePath }) { candidate ->
                            Text(candidate.file.name + " · " + candidate.file.parent.orEmpty())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        running = true
                        scope.launch {
                            try {
                                val prepared = engine.prepareSnapshots(selected)
                                if (prepared.mediaUris.isNotEmpty()) {
                                    val request = engine.createSystemTrashRequest(prepared.mediaUris)
                                    if (request != null) {
                                        pending = prepared
                                        launcher.launch(
                                            IntentSenderRequest.Builder(request.intentSender).build(),
                                        )
                                    } else {
                                        running = false
                                        message = "Conferma del cestino Android non disponibile: file intatti."
                                    }
                                } else {
                                    val moved = engine.movePreparedSnapshotsToSafeTrash(prepared.safeTrashCandidates)
                                    message = "Spostati: ${moved.moved} · Non idonei: ${prepared.blockedFiles.size} · Modificati: ${prepared.staleFiles.size} · Rifiutati: ${moved.rejected}"
                                    if (moved.moved > 0) {
                                        CleanupEventBus.publishCompleted()
                                        onFinished()
                                    }
                                    running = false
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                running = false
                                message = "Errore nella preparazione. Nessun altro file viene rimosso."
                            }
                        }
                    },
                ) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Annulla") }
            },
        )
    }
}
