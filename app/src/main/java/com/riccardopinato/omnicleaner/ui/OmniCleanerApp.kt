package com.riccardopinato.omnicleaner.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.BigFileSizeFilter
import com.riccardopinato.omnicleaner.cleaner.DeepStorageAccessStatus
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.ThresholdUnit
import com.riccardopinato.omnicleaner.exif.ExifCleanerSection
import com.riccardopinato.omnicleaner.gallery.GalleryCleanerSection
import com.riccardopinato.omnicleaner.monetization.AdManager
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import com.riccardopinato.omnicleaner.monetization.PremiumEntitlementStore
import com.riccardopinato.omnicleaner.monetization.findActivity
import java.util.Locale

@Composable
fun OmniCleanerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context.applicationContext, scope) {
        CleanerController(context.applicationContext, scope)
    }
    val state = controller.state
    val activity = remember(context) { context.findActivity() }
    var showDeepAccessDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity != null) {
            CleanupEventBus.events.collect {
                AdManager.onCleanupCompleted(
                    activity = activity,
                    isPremium = PremiumEntitlementStore.state.value.isPremium,
                )
            }
        }
    }

    val storageSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshAccessAndStorage()
    }
    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        controller.refreshAccessAndStorage()
    }

    fun requestStorageAccess() {
        when (state.storageCapability.status) {
            DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS ->
                storageSettingsLauncher.launch(StorageAccess.settingsIntent(context))

            DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION ->
                legacyStorageLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ),
                )

            else -> Unit
        }
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                StorageCard(
                    state = state,
                    onGrantAccess = { showDeepAccessDisclosure = true },
                    onRefresh = controller::refreshAccessAndStorage,
                )
            }

            if (state.hasBroadAccess) {
                item {
                    ActionCard(
                        scanning = state.scanning,
                        currentOperation = state.currentOperation,
                        onOverview = controller::scanOverview,
                        onFolders = controller::scanFolders,
                        onBigFiles = { controller.scanBigFiles() },
                        onDuplicates = controller::scanDuplicates,
                        onCancel = controller::cancelScan,
                    )
                }
            }

            state.error?.let { error ->
                item {
                    MessageCard(
                        title = stringResource(R.string.scan_error),
                        body = error,
                    )
                }
            }

            if (state.hasBroadAccess) {
                item {
                    SearchAndFilterCard(
                        state = state,
                        onFolderQuery = controller::updateFolderSearchQuery,
                        onBigFileQuery = controller::updateBigFileSearchQuery,
                        onDuplicateQuery = controller::updateDuplicateSearchQuery,
                        onBigFileSizeFilter = controller::updateBigFileSizeFilter,
                        onThresholdValue = controller::updateThresholdValue,
                        onThresholdUnit = controller::updateThresholdUnit,
                    )
                }
            }

            state.overview?.let { overview ->
                item {
                    ResultHeader(
                        title = stringResource(R.string.module_storage_overview),
                        subtitle = stringResource(
                            R.string.overview_summary,
                            overview.files,
                            formatBytes(overview.logicalBytes),
                        ),
                    )
                }
                items(overview.byCategory.take(12), key = { it.path }) { category ->
                    ResultRow(category.path, formatBytes(category.bytes))
                }
            }

            if (state.visibleFolders.isNotEmpty()) {
                item {
                    val empty = state.visibleFolders.count { it.status == com.riccardopinato.omnicleaner.cleaner.FolderStatus.COMPLETELY_EMPTY }
                    ResultHeader(
                        title = stringResource(R.string.module_folders),
                        subtitle = stringResource(R.string.folder_summary, state.visibleFolders.size, empty),
                    )
                }
                items(state.visibleFolders.take(30), key = { it.path }) { folder ->
                    ResultRow(
                        title = folder.name,
                        value = "${formatBytes(folder.sizeBytes)} · ${folder.fileCount}",
                    )
                }
            }

            if (state.visibleBigFiles.isNotEmpty()) {
                item {
                    ResultHeader(
                        title = stringResource(R.string.module_large_files),
                        subtitle = stringResource(R.string.big_files_summary, state.visibleBigFiles.size),
                    )
                }
                items(state.visibleBigFiles.take(40), key = { it.path }) { item ->
                    ResultRow(item.name, formatBytes(item.sizeBytes))
                }
            }

            if (state.visibleDuplicates.isNotEmpty()) {
                item {
                    val waste = state.visibleDuplicates.sumOf { it.totalWasteBytes }
                    ResultHeader(
                        title = stringResource(R.string.module_duplicates),
                        subtitle = stringResource(
                            R.string.duplicates_summary,
                            state.visibleDuplicates.size,
                            formatBytes(waste),
                        ),
                    )
                }
                items(state.visibleDuplicates.take(30), key = { it.hash }) { group ->
                    ResultRow(
                        title = group.files.firstOrNull()?.name ?: group.hash.take(12),
                        value = "${group.files.size} × ${formatBytes(group.sizeBytes)}",
                    )
                }
            }

            item {
                StorageCleanupSection(
                    folders = state.visibleFolders,
                    bigFiles = state.visibleBigFiles,
                    duplicateGroups = state.visibleDuplicates,
                    onCleanupFinished = {
                        controller.refreshAccessAndStorage()
                        controller.scanOverview()
                    },
                )
            }

            item {
                GalleryCleanerSection()
            }

            item {
                MaintenanceSection()
            }

            item {
                ExifCleanerSection()
            }

            item {
                com.riccardopinato.omnicleaner.monetization.MonetizationSection()
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showDeepAccessDisclosure) {
        AlertDialog(
            onDismissRequest = { showDeepAccessDisclosure = false },
            title = { Text(stringResource(R.string.deep_access_dialog_title)) },
            text = { Text(stringResource(R.string.deep_access_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeepAccessDisclosure = false
                        requestStorageAccess()
                    },
                ) {
                    Text(stringResource(R.string.deep_access_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeepAccessDisclosure = false }) {
                    Text(stringResource(R.string.deep_access_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun SearchAndFilterCard(
    state: CleanerUiState,
    onFolderQuery: (String) -> Unit,
    onBigFileQuery: (String) -> Unit,
    onDuplicateQuery: (String) -> Unit,
    onBigFileSizeFilter: (BigFileSizeFilter) -> Unit,
    onThresholdValue: (String) -> Unit,
    onThresholdUnit: (ThresholdUnit) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Filtri recuperati", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.folderSearchQuery,
                onValueChange = onFolderQuery,
                label = { Text("Cerca cartelle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.bigFileSearchQuery,
                onValueChange = onBigFileQuery,
                label = { Text("Cerca file grandi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.duplicateSearchQuery,
                onValueChange = onDuplicateQuery,
                label = { Text("Cerca duplicati") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Soglia cartelle piccole", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.thresholdValue,
                onValueChange = onThresholdValue,
                label = { Text("Valore soglia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThresholdUnit.entries.forEach { unit ->
                    if (unit == state.thresholdUnit) {
                        Button(onClick = { onThresholdUnit(unit) }) { Text(unit.label) }
                    } else {
                        OutlinedButton(onClick = { onThresholdUnit(unit) }) { Text(unit.label) }
                    }
                }
            }
            Text("Soglia file grandi", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BigFileSizeFilter.entries.forEach { filter ->
                    if (filter == state.bigFileSizeFilter) {
                        Button(onClick = { onBigFileSizeFilter(filter) }) {
                            Text(filter.label)
                        }
                    } else {
                        OutlinedButton(onClick = { onBigFileSizeFilter(filter) }) {
                            Text(filter.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(
    state: CleanerUiState,
    onGrantAccess: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.storage), style = MaterialTheme.typography.titleLarge)
            state.storageInfo?.let { info ->
                Text(
                    stringResource(
                        R.string.storage_usage,
                        formatBytes(info.usedBytes),
                        formatBytes(info.totalBytes),
                    ),
                )
                LinearProgressIndicator(
                    progress = { info.usedPercent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                if (state.storageCapability.standardCleaningAvailable) {
                    stringResource(R.string.standard_cleaning_ready)
                } else {
                    stringResource(R.string.standard_cleaning_unavailable)
                },
                color = if (state.storageCapability.standardCleaningAvailable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                when (state.storageCapability.status) {
                    DeepStorageAccessStatus.READY -> stringResource(R.string.access_ready)
                    DeepStorageAccessStatus.NEEDS_ALL_FILES_ACCESS -> stringResource(R.string.deep_access_optional)
                    DeepStorageAccessStatus.NEEDS_LEGACY_PERMISSION -> stringResource(R.string.deep_access_optional)
                    DeepStorageAccessStatus.SCOPED_ONLY_ANDROID_10 -> stringResource(R.string.deep_access_android10)
                    DeepStorageAccessStatus.STORAGE_UNAVAILABLE -> stringResource(R.string.deep_access_unavailable)
                },
                color = if (state.hasBroadAccess) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.hasBroadAccess) {
                Text(
                    stringResource(R.string.access_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.storageCapability.canRequestDeepAccess) {
                    Button(onClick = onGrantAccess) {
                        Text(stringResource(R.string.grant_deep_access))
                    }
                }
                OutlinedButton(onClick = onRefresh) {
                    Text(stringResource(R.string.refresh))
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    scanning: Boolean,
    currentOperation: String?,
    onOverview: () -> Unit,
    onFolders: () -> Unit,
    onBigFiles: () -> Unit,
    onDuplicates: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.scan_tools), style = MaterialTheme.typography.titleLarge)
            if (scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(currentOperation.orEmpty())
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                Button(onClick = onOverview, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_overview))
                }
                Button(onClick = onFolders, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_folders))
                }
                Button(onClick = onBigFiles, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_big_files))
                }
                Button(onClick = onDuplicates, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.scan_duplicates))
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultRow(title: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, maxLines = 1)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MessageCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
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
