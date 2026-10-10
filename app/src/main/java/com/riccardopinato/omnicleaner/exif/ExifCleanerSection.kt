package com.riccardopinato.omnicleaner.exif

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var status by remember { mutableStateOf<String?>(null) }

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
            Text("EXIF Privacy", style = MaterialTheme.typography.titleLarge)
            Text(
                "Analizza JPEG e crea copie pulite. Gli originali non vengono modificati.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { sourcePicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (sourceTree == null) "Scegli cartella foto" else "Cambia cartella foto")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(
                    checked = recursive,
                    onCheckedChange = { recursive = it },
                )
                Text("Includi sottocartelle")
            }

            ExifGroup.entries.forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = group in groups,
                        onCheckedChange = { checked ->
                            groups = if (checked) groups + group else groups - group
                            scan = null
                            export = null
                        },
                    )
                    Text(group.title)
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
                            status = "${it.candidates.size} JPEG con metadati selezionati"
                        }.onFailure {
                            status = it.message ?: "Analisi EXIF non riuscita"
                        }
                        busy = false
                    }
                },
                enabled = sourceTree != null && groups.isNotEmpty() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Analizza EXIF")
            }

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            scan?.let { result ->
                Text(
                    "Trovati: ${result.candidates.size} · saltati: ${result.skipped} · errori: ${result.failures}",
                    style = MaterialTheme.typography.bodySmall,
                )

                OutlinedButton(
                    onClick = { destinationPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (destinationTree == null) "Scegli cartella di esportazione" else "Cambia destinazione")
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
                                        status = "Esportazione ${done}/${total}"
                                    },
                                    onIncomplete = { name ->
                                        status = "Verifica copia fallita: $name"
                                    },
                                )
                            }.onSuccess {
                                export = it
                                status = "Create ${it.created} copie pulite · errori ${it.failed}"
                            }.onFailure {
                                status = it.message ?: "Esportazione EXIF non riuscita"
                            }
                            busy = false
                        }
                    },
                    enabled = destinationTree != null &&
                        result.candidates.isNotEmpty() &&
                        !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Esporta copie pulite")
                }
            }

            export?.let { result ->
                Text(
                    "Copie create: ${result.created} · fallite: ${result.failed}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
