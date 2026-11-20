package com.riccardopinato.omnicleaner.ui

import android.app.Activity
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import com.riccardopinato.omnicleaner.cleaner.OmniDiscoveredMedia
import com.riccardopinato.omnicleaner.cleaner.OmniScanCoverageResult
import com.riccardopinato.omnicleaner.cleaner.OmniSourceCoverageScanner
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extra sources panel for Storage Intelligence.
 * A checked file always remains untouched until an explicit review dialog.
 * MediaStore uses Android's OS Trash consent; SAF needs a separate explicit
 * irreversible-deletion acknowledgment after re-reading the document metadata.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OmniCoverageSection() {
    val context = LocalContext.current
    val scanner = remember { OmniSourceCoverageScanner() }
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<OmniScanCoverageResult?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Map<String, OmniDiscoveredMedia>>(emptyMap()) }
    var quickLook by remember { mutableStateOf<OmniDiscoveredMedia?>(null) }
    var visible by remember { mutableStateOf(50) }
    var nameQuery by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var acceptIrreversible by remember { mutableStateOf(false) }
    var pendingMediaCount by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { answer ->
        if (answer.resultCode == Activity.RESULT_OK) {
            message = "Cestino Android confermato per $pendingMediaCount elementi."
            selected = emptyMap()
            StorageInventoryRepository.invalidate()
            report = null
        } else {
            message = "Richiesta annullata: nessun altro file eliminato."
        }
        pendingMediaCount = 0
    }

    fun scan() {
        if (scanning) return
        scope.launch {
            scanning = true
            message = null
            selected = emptyMap()
            visible = 50
            try {
                report = scanner.discover(context.applicationContext, nameQuery = nameQuery)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "La verifica delle sorgenti non è riuscita."
            } finally {
                scanning = false
            }
        }
    }

    fun select(item: OmniDiscoveredMedia, checked: Boolean) {
        selected = if (checked) selected + (item.uri.toString() to item)
            else selected - item.uri.toString()
    }

    Card(Modifier.fillMaxWidth().testTag("omni_coverage_sources")) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Copertura completa delle sorgenti", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cerca anche nei media Android, nelle cartelle SAF autorizzate e nelle memorie SD/USB accessibili. " +
                    "Le sorgenti protette vengono dichiarate, non considerate vuote.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = nameQuery,
                onValueChange = { nameQuery = it },
                label = { Text("Cerca per nome in tutte le sorgenti") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("omni_coverage_query"),
            )
            Button(
                onClick = ::scan,
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth().testTag("omni_coverage_start"),
            ) { Text("Controlla tutte le sorgenti") }
            if (scanning) CircularProgressIndicator()
            report?.let { result ->
                for (source in result.sources) {
                    Text(
                        "${source.label} · ${source.status} · ${source.scannedCount} voci",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(source.detail, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Le voci da origini diverse potrebbero riferirsi allo stesso file. " +
                        "Non sommare i conteggi come spazio recuperabile.",
                    style = MaterialTheme.typography.labelSmall,
                )
                for (item in result.visibleResources.take(visible)) {
                    val checked = item.uri.toString() in selected
                    val kind = omniQuickLookKind(item.name, item.mimeType)
                    Row(
                        Modifier.fillMaxWidth()
                            .combinedClickable(
                                role = Role.Checkbox,
                                onClick = { select(item, !checked) },
                                onLongClick = { if (kind != null) quickLook = item },
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { value -> select(item, value) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.name, maxLines = 2)
                            Text(
                                "${item.sizeBytes} byte · ${item.sourceId.substringBefore(':')}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (result.visibleResources.size > visible) {
                    TextButton(onClick = {
                        visible = (visible + 100).coerceAtMost(result.visibleResources.size)
                    }) { Text("Mostra altri file") }
                }
                if (selected.isNotEmpty()) {
                    Button(
                        onClick = { acceptIrreversible = false; confirm = true },
                        modifier = Modifier.fillMaxWidth().testTag("omni_coverage_trash_selected"),
                    ) { Text("Rivedi ${selected.size} file selezionati") }
                }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    quickLook?.let { item ->
        val kind = omniQuickLookKind(item.name, item.mimeType)
        if (kind != null) {
            OmniMediaQuickLook(
                target = OmniQuickLookTarget(item.uri, item.name, kind),
                onDismiss = { quickLook = null },
                selected = item.uri.toString() in selected,
                onSelectedChange = { checked -> select(item, checked) },
            )
        }
    }
    if (confirm) {
        val chosen = selected.values.toList()
        val media = chosen.filter { it.sourceId.startsWith("media:") }
        val saf = chosen.filter { it.sourceId.startsWith("saf:") }
        val unsupported = chosen.filter { !it.sourceId.startsWith("media:") && !it.sourceId.startsWith("saf:") }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Revisione eliminazione") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "MediaStore: ${media.size} (cestino Android). SAF: ${saf.size} (eliminazione definitiva). " +
                            "Non gestiti: ${unsupported.size}."
                    )
                    LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(chosen, key = { it.uri.toString() }) { file ->
                            Text(file.name + " · " + file.sourceId.substringBefore(':'))
                        }
                    }
                    if (saf.isNotEmpty()) {
                        Row {
                            Checkbox(
                                checked = acceptIrreversible,
                                onCheckedChange = { acceptIrreversible = it },
                            )
                            Text("Confermo che i file SAF saranno eliminati definitivamente, senza possibilità di ripristino.")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = chosen.isNotEmpty() && (saf.isEmpty() || acceptIrreversible),
                    onClick = {
                        confirm = false
                        scope.launch {
                            // SAF and MediaStore never share an opaque blanket-delete
                            // operation; each backend retains its own safety contract.
                            var safDeleted = 0
                            var safRejected = 0
                            if (saf.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    for (item in saf) {
                                        try {
                                            val doc = DocumentFile.fromSingleUri(context, item.uri)
                                            val unchanged = doc != null && doc.isFile &&
                                                doc.name == item.name &&
                                                doc.length() == item.sizeBytes &&
                                                doc.lastModified() == item.modifiedMillis &&
                                                doc.canWrite()
                                            if (unchanged &&
                                                DocumentsContract.deleteDocument(
                                                    context.contentResolver, item.uri,
                                                )
                                            ) safDeleted++ else safRejected++
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            safRejected++
                                        }
                                    }
                                }
                            }
                            if (media.isNotEmpty()) {
                                val request = MediaStoreHelper.trashMediaFiles(
                                    context.contentResolver,
                                    media.take(2000).map { it.uri },
                                )
                                if (request != null) {
                                    pendingMediaCount = media.take(2000).size
                                    launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                } else {
                                    message = "Cestino Android non disponibile; file media invariati."
                                }
                            }
                            if (safDeleted > 0) {
                                message = "SAF eliminati: $safDeleted · Rifiutati: $safRejected. " +
                                    "Altri file non modificati."
                                StorageInventoryRepository.invalidate()
                                report = null
                                selected = emptyMap()
                            } else if (safRejected > 0) {
                                message = "SAF non eliminati: $safRejected. Verifica permessi e versione dei file."
                            }
                            if (unsupported.isNotEmpty() && message == null) {
                                message = "${unsupported.size} file locali richiedono la revisione dal modulo Pulizia."
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
