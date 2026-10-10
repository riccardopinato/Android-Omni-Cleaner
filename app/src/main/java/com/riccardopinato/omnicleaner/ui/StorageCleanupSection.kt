package com.riccardopinato.omnicleaner.ui

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.cleaner.BigFileItem
import com.riccardopinato.omnicleaner.cleaner.DuplicateGroup
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.FolderItem
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.ManualMaintenance
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.launch

private enum class CleanupDialog {
    EMPTY_FOLDERS,
    FILES,
}

@Composable
fun StorageCleanupSection(
    folders: List<FolderItem>,
    bigFiles: List<BigFileItem>,
    duplicateGroups: List<DuplicateGroup>,
    onCleanupFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember(context.applicationContext) {
        StorageCleanupEngine(context.applicationContext)
    }

    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedBigFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDuplicates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dialog by remember { mutableStateOf<CleanupDialog?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingPreparation by remember { mutableStateOf<FileCleanupPreparation?>(null) }

    val systemTrashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val preparation = pendingPreparation
        pendingPreparation = null

        if (result.resultCode != Activity.RESULT_OK || preparation == null) {
            message = "Operazione nel cestino Android annullata."
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val safeResult = engine.movePreparedNonMediaToSafeTrash(preparation.safeTrashFiles)
            message = buildString {
                append("Cestino Android confermato")
                if (safeResult.moved > 0) append(" · " + safeResult.moved + " file spostati nel Safe Trash")
                if (safeResult.rejected > 0) append(" · " + safeResult.rejected + " file rifiutati")
                if (preparation.blockedFiles.isNotEmpty()) {
                    append(" · " + preparation.blockedFiles.size + " file lasciati invariati perché non eliminabili in modo reversibile")
                }
            }
            selectedBigFiles = emptySet()
            selectedDuplicates = emptySet()
            CleanupEventBus.publishCompleted()
            onCleanupFinished()
        }
    }

    fun executeSelectedFiles() {
        val big = bigFiles.filter { it.path in selectedBigFiles }.map { it.file }
        val duplicates = engine.protectedDuplicateFiles(duplicateGroups, selectedDuplicates)
        val requested = big + duplicates
        if (requested.isEmpty()) {
            message = "Nessun file selezionato."
            return
        }

        scope.launch {
            val preparation = engine.prepareFiles(requested)
            if (preparation.mediaUris.isNotEmpty()) {
                val request = engine.createSystemTrashRequest(preparation.mediaUris)
                if (request == null) {
                    message = "Il cestino di sistema non è disponibile per i media selezionati."
                    return@launch
                }
                pendingPreparation = preparation
                systemTrashLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                return@launch
            }

            val safeResult = engine.movePreparedNonMediaToSafeTrash(preparation.safeTrashFiles)
            message = buildString {
                append(safeResult.moved)
                append(" file spostati nel Safe Trash")
                if (safeResult.rejected > 0) append(" · " + safeResult.rejected + " rifiutati")
                if (preparation.blockedFiles.isNotEmpty()) {
                    append(" · " + preparation.blockedFiles.size + " lasciati invariati perché non eliminabili in modo reversibile")
                }
            }
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
                Text("Revisione e pulizia", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Ogni azione distruttiva richiede selezione e conferma. I media usano il cestino Android; i file non eliminabili in modo reversibile restano invariati.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (emptyFolders.isNotEmpty()) {
                    Text("Cartelle realmente vuote", style = MaterialTheme.typography.titleMedium)
                    emptyFolders.take(20).forEach { folder ->
                        SelectionRow(
                            checked = folder.path in selectedFolders,
                            label = folder.name,
                            detail = folder.displayPath,
                            onCheckedChange = { checked ->
                                selectedFolders = selectedFolders.withToggle(folder.path, checked)
                            },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedFolders = emptyFolders.mapTo(linkedSetOf()) { it.path }
                            },
                        ) {
                            Text("Seleziona vuote")
                        }
                        Button(
                            onClick = { dialog = CleanupDialog.EMPTY_FOLDERS },
                            enabled = selectedFolders.isNotEmpty(),
                        ) {
                            Text("Elimina selezionate")
                        }
                    }
                }

                if (bigFiles.isNotEmpty()) {
                    Text("File grandi", style = MaterialTheme.typography.titleMedium)
                    bigFiles.take(20).forEach { file ->
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

                if (duplicateGroups.isNotEmpty()) {
                    Text("Duplicati", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedDuplicates = engine.selectDuplicatesExceptOldest(duplicateGroups)
                            },
                        ) {
                            Text("Keep oldest")
                        }
                        OutlinedButton(
                            onClick = {
                                selectedDuplicates = engine.selectDuplicatesExceptNewest(duplicateGroups)
                            },
                        ) {
                            Text("Keep newest")
                        }
                    }

                    duplicateGroups.take(12).forEach { group ->
                        Text(
                            group.files.size.toString() + " copie · " +
                                group.files.firstOrNull()?.formattedSize().orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        group.files.take(6).forEach { file ->
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
                }

                if (selectedBigFiles.isNotEmpty() || selectedDuplicates.isNotEmpty()) {
                    Button(
                        onClick = { dialog = CleanupDialog.FILES },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sposta selezione nel cestino")
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
    }

    ManualMaintenanceCard()

    when (dialog) {
        CleanupDialog.EMPTY_FOLDERS -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Eliminare le cartelle vuote?") },
            text = {
                Text(
                    "Ogni cartella viene ricontrollata: deve essere ancora vuota, interna alla memoria utente e fuori dai percorsi protetti.",
                )
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
                            message = result.removed.toString() + " cartelle eliminate · " +
                                result.rejected + " rifiutate"
                            selectedFolders = emptySet()
                            if (result.removed > 0) CleanupEventBus.publishCompleted()
                            onCleanupFinished()
                        }
                    },
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Annulla") }
            },
        )

        CleanupDialog.FILES -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Confermare la pulizia?") },
            text = {
                Text(
                    "I duplicati mantengono almeno una copia. I media passano dal cestino Android. I non-media vanno nel Safe Trash solo quando l’operazione è verificabile; nessun file grande viene eliminato permanentemente come fallback.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialog = null
                        executeSelectedFiles()
                    },
                ) { Text("Continua") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Annulla") }
            },
        )

        null -> Unit
    }
}

@Composable
private fun ManualMaintenanceCard() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Manutenzione manuale", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val activity = context as? Activity
                        message = when {
                            activity == null -> "Pulizia clipboard non disponibile in questo contesto."
                            ManualMaintenance.clearClipboard(activity) -> "Clipboard pulita."
                            else -> "Porta l’app in primo piano e riprova."
                        }
                    },
                ) {
                    Text("Pulisci clipboard")
                }
                OutlinedButton(
                    onClick = {
                        message = if (ManualMaintenance.openAppManagement(context)) {
                            "Gestione app aperta."
                        } else {
                            "Impossibile aprire la gestione app."
                        }
                    },
                ) {
                    Text("Gestione app")
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun Set<String>.withToggle(value: String, checked: Boolean): Set<String> =
    if (checked) this + value else this - value
