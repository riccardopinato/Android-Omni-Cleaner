package com.riccardopinato.omnicleaner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.riccardopinato.omnicleaner.cleaner.StorageCapabilityHealth
import com.riccardopinato.omnicleaner.model.BigFileSizeFilter
import com.riccardopinato.omnicleaner.model.FolderSubTab
import com.riccardopinato.omnicleaner.model.GalleryMediaFilter
import com.riccardopinato.omnicleaner.model.GallerySortOrder
import com.riccardopinato.omnicleaner.model.ThresholdUnit
import com.riccardopinato.omnicleaner.model.formatBytes
import com.riccardopinato.omnicleaner.viewmodel.CleanTab
import com.riccardopinato.omnicleaner.viewmodel.OmniCleanerViewModel
import com.riccardopinato.omnicleaner.viewmodel.OmniUiState

@Composable
fun OmniCleanerApp(viewModel: OmniCleanerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var section by rememberSaveable { mutableIntStateOf(0) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.checkPermission() }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.checkPermission()
        viewModel.loadGalleryMedia(force = true)
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onMediaStoreTrashCompleted(result.resultCode == Activity.RESULT_OK)
    }

    val requestDeepAccess = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                android.net.Uri.parse("package:" + context.packageName),
            )
            allFilesLauncher.launch(intent)
        } else {
            mediaPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
            )
        }
    }

    val requestGalleryAccess = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ),
            )
        } else {
            mediaPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    Scaffold { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Android Omni Cleaner", style = MaterialTheme.typography.headlineMedium) }
            item { StorageHeader(state, viewModel) }

            state.lastMessage?.let { message ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(message, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (section == 0) Button(onClick = { section = 0 }) { Text("Pulizia") }
                    else OutlinedButton(onClick = { section = 0 }) { Text("Pulizia") }

                    if (section == 1) Button(onClick = { section = 1 }) { Text("Galleria") }
                    else OutlinedButton(onClick = { section = 1 }) { Text("Galleria") }
                }
            }

            if (section == 0) {
                item { CapabilityCard(state, requestDeepAccess) }
                item { CleanTabBar(state, viewModel) }
                when (state.currentTab) {
                    CleanTab.EMPTY_FOLDERS -> folderContent(state, viewModel)
                    CleanTab.DUPLICATES -> duplicateContent(state, viewModel)
                    CleanTab.BIG_FILES -> bigFileContent(state, viewModel)
                    CleanTab.UNLOCK_PRO -> proContent(state)
                }
            } else {
                galleryContent(
                    state = state,
                    viewModel = viewModel,
                    requestPermission = requestGalleryAccess,
                    requestTrash = {
                        val pending = viewModel.galleryTrashRequest()
                        if (pending != null) {
                            trashLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StorageHeader(state: OmniUiState, viewModel: OmniCleanerViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Spazio", style = MaterialTheme.typography.titleLarge)
            Text(
                "Usato " + formatBytes(state.storageInfo.usedBytes) +
                    " / " + formatBytes(state.storageInfo.totalBytes),
            )
            LinearProgressIndicator(
                progress = { state.storageInfo.usedPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Cestino sicuro: " + formatBytes(state.safeTrashBytes))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::refreshStorageInfo) { Text("Aggiorna") }
                OutlinedButton(onClick = viewModel::scanSpaceOverview) { Text("Analizza") }
                if (state.safeTrashBytes > 0) {
                    OutlinedButton(onClick = viewModel::emptySafeTrash) { Text("Svuota cestino") }
                }
            }
            state.spaceOverview?.let { overview ->
                Text(
                    overview.files.toString() + " file indicizzati · " +
                        formatBytes(overview.logicalBytes),
                )
                overview.byCategory
                    .filterValues { it > 0L }
                    .forEach { entry ->
                        Text(entry.key + ": " + formatBytes(entry.value))
                    }
            }
        }
    }
}

@Composable
private fun CapabilityCard(state: OmniUiState, onRequest: () -> Unit) {
    if (state.capabilityHealth == StorageCapabilityHealth.HEALTHY) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Accesso a tutti i file richiesto", style = MaterialTheme.typography.titleMedium)
            Text("Le scansioni profonde restano bloccate finché Android non concede questa capacità.")
            Button(onClick = onRequest) { Text("Apri impostazioni") }
        }
    }
}

@Composable
private fun CleanTabBar(state: OmniUiState, viewModel: OmniCleanerViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CleanTab.entries.forEach { tab ->
            if (state.currentTab == tab) {
                Button(onClick = { viewModel.setTab(tab) }) { Text(tab.title) }
            } else {
                OutlinedButton(onClick = { viewModel.setTab(tab) }) { Text(tab.title) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.folderContent(
    state: OmniUiState,
    viewModel: OmniCleanerViewModel,
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FolderSubTab.entries.forEach { tab ->
                OutlinedButton(onClick = { viewModel.setFolderSubTab(tab) }) {
                    Text(
                        when (tab) {
                            FolderSubTab.EMPTY_FOLDERS -> "Cartelle"
                            FolderSubTab.APP_RESIDUES -> "Residui"
                            FolderSubTab.THUMBNAIL_CACHE -> "Miniature"
                        },
                    )
                }
            }
        }
    }

    when (state.folderSubTab) {
        FolderSubTab.EMPTY_FOLDERS -> {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cartelle vuote o sotto soglia", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.thresholdValue,
                        onValueChange = viewModel::updateThresholdValue,
                        label = { Text("Soglia") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThresholdUnit.entries.forEach { unit ->
                            OutlinedButton(onClick = { viewModel.updateThresholdUnit(unit) }) {
                                Text(unit.label)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.folderSearchQuery,
                        onValueChange = viewModel::updateFolderSearchQuery,
                        label = { Text("Cerca cartella") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = viewModel::startFolderScan) { Text("Scansiona cartelle") }
                    if (state.folderScanProgress.isScanning) {
                        Text(state.folderScanProgress.currentPath.takeLast(80))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(state.filteredFolders, key = { it.path }) { folder ->
                SelectionCard(
                    checked = folder.isSelected,
                    title = folder.name,
                    subtitle = folder.formattedSize + " · " + folder.fileCount +
                        " file · " + folder.path,
                    onToggle = { viewModel.toggleFolderSelection(folder.path) },
                )
            }
            if (state.folders.any { it.isSelected }) {
                item {
                    Button(
                        enabled = !state.isDeleting,
                        onClick = viewModel::cleanSelectedFolders,
                    ) { Text("Sposta selezionate nel cestino sicuro") }
                }
            }
        }

        FolderSubTab.APP_RESIDUES -> {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Residui app", style = MaterialTheme.typography.titleMedium)
                    Text("Candidati da verificare: nessuna cancellazione automatica.")
                    Button(onClick = viewModel::startAppResiduesScan) { Text("Analizza residui") }
                    if (state.isScanningResidues) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(state.appResidues, key = { it.path }) { item ->
                InfoCard(
                    item.name,
                    item.formattedSize + " · " + item.fileCount + " file · " + item.path,
                )
            }
        }

        FolderSubTab.THUMBNAIL_CACHE -> {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cache miniature", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = viewModel::startThumbnailCacheScan) { Text("Analizza miniature") }
                    if (state.isScanningThumbnailCache) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(state.thumbnailCaches, key = { it.path }) { item ->
                SelectionCard(
                    checked = item.isSelected,
                    title = item.name,
                    subtitle = item.formattedSize + " · " + item.fileCount + " file · " + item.path,
                    onToggle = { viewModel.toggleThumbnailCacheSelection(item.path) },
                )
            }
            if (state.thumbnailCaches.any { it.isSelected }) {
                item {
                    Button(
                        enabled = !state.isDeleting,
                        onClick = viewModel::cleanSelectedThumbnails,
                    ) { Text("Elimina miniature verificate") }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.duplicateContent(
    state: OmniUiState,
    viewModel: OmniCleanerViewModel,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Duplicati", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.duplicateSearchQuery,
                onValueChange = viewModel::updateDuplicateSearchQuery,
                label = { Text("Filtra duplicati per nome file...") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::startDuplicateScan) { Text("Scansiona") }
                OutlinedButton(onClick = viewModel::smartSelectAllDuplicates) { Text("Selezione sicura") }
                OutlinedButton(onClick = viewModel::smartSelectKeepOldest) { Text("Tieni più vecchio") }
                OutlinedButton(onClick = viewModel::smartSelectKeepNewest) { Text("Tieni più nuovo") }
            }
            if (state.duplicateScanProgress.isScanning) {
                Text(state.duplicateScanProgress.phaseDescription)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    state.filteredDuplicateGroups.forEach { group ->
        item(key = "group-" + group.hash) {
            Text(
                group.files.size.toString() + " copie · spreco potenziale " +
                    formatBytes(group.totalWasteBytes),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        items(group.files, key = { it.path }) { file ->
            SelectionCard(
                checked = file.isSelected,
                title = file.name,
                subtitle = file.formattedSize + " · " + file.path,
                onToggle = { viewModel.toggleDuplicateFileSelection(file.path) },
            )
        }
    }

    if (state.duplicateGroups.any { group -> group.files.any { it.isSelected } }) {
        item {
            Button(
                enabled = !state.isDeleting,
                onClick = viewModel::cleanSelectedDuplicates,
            ) { Text("Sposta duplicati selezionati nel cestino") }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bigFileContent(
    state: OmniUiState,
    viewModel: OmniCleanerViewModel,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("File grandi", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BigFileSizeFilter.entries.forEach { filter ->
                    OutlinedButton(onClick = { viewModel.updateBigFileSizeFilter(filter) }) {
                        Text(filter.label)
                    }
                }
            }
            OutlinedTextField(
                value = state.bigFileSearchQuery,
                onValueChange = viewModel::updateBigFileSearchQuery,
                label = { Text("Cerca file") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::startBigFilesScan) { Text("Scansiona file grandi") }
            if (state.bigFileScanProgress.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    items(state.filteredBigFiles, key = { it.path }) { file ->
        SelectionCard(
            checked = file.isSelected,
            title = file.name,
            subtitle = formatBytes(file.sizeBytes) + " · " + file.fileTypeDescription +
                " · " + file.path,
            onToggle = { viewModel.toggleBigFileSelection(file.path) },
        )
    }

    if (state.bigFiles.any { it.isSelected }) {
        item {
            Button(
                enabled = !state.isDeleting,
                onClick = viewModel::cleanSelectedBigFiles,
            ) { Text("Sposta file selezionati nel cestino") }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.proContent(state: OmniUiState) {
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("PRO", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Entitlement predisposto. Billing e Ads verranno collegati dopo la parity " +
                        "senza riusare le configurazioni test del donor.",
                )
                Text("Stato tecnico: " + if (state.isPremium) "PRO" else "FREE")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.galleryContent(
    state: OmniUiState,
    viewModel: OmniCleanerViewModel,
    requestPermission: () -> Unit,
    requestTrash: () -> Unit,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gallery Cleaner", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalleryMediaFilter.entries.forEach { filter ->
                    OutlinedButton(onClick = { viewModel.updateGalleryFilter(filter) }) {
                        Text(filter.label)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GallerySortOrder.entries.forEach { sort ->
                    OutlinedButton(onClick = { viewModel.updateGallerySortOrder(sort) }) {
                        Text(sort.label)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.loadGalleryMedia(force = true) }) {
                    Text("Carica galleria")
                }
                OutlinedButton(onClick = requestPermission) { Text("Permesso media") }
            }
            if (state.galleryIsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    val current = state.galleryItems.getOrNull(state.galleryCurrentIndex)
    if (current != null) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(current.name, style = MaterialTheme.typography.titleMedium)
                    Text(current.formattedSize + " · " + current.formattedDate)
                    Text(if (current.isVideo) "Video " + current.formattedDuration else "Foto")
                    if (current.album.isNotBlank()) Text(current.album)
                    Text(
                        (state.galleryCurrentIndex + 1).toString() +
                            " / " + state.galleryItems.size,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::swipeKeepCurrent) { Text("Tieni") }
                        Button(onClick = viewModel::swipeTrashCurrent) { Text("Cestino") }
                        OutlinedButton(
                            enabled = state.galleryHistory.isNotEmpty(),
                            onClick = viewModel::undoLastGalleryAction,
                        ) { Text("Annulla") }
                    }
                }
            }
        }
    } else if (state.hasLoadedGalleryOnce && !state.galleryIsLoading) {
        item { Text("Nessun altro elemento da revisionare.") }
    }

    if (state.galleryTrashedItems.isNotEmpty()) {
        item {
            Button(onClick = requestTrash) {
                Text(
                    "Conferma " + state.galleryTrashedItems.size +
                        " elementi nel cestino Android",
                )
            }
        }
    }
}

@Composable
private fun SelectionCard(
    checked: Boolean,
    title: String,
    subtitle: String,
    onToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
