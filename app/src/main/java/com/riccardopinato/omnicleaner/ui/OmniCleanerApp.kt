package com.riccardopinato.omnicleaner.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.exif.ExifCleanerSection
import com.riccardopinato.omnicleaner.gallery.GalleryCleanerSection
import java.util.Locale

@Composable
fun OmniCleanerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context.applicationContext, scope) {
        CleanerController(context.applicationContext, scope)
    }
    val state = controller.state

    val storageSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshAccessAndStorage()
    }
    val legacyReadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        controller.refreshAccessAndStorage()
    }

    fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storageSettingsLauncher.launch(StorageAccess.settingsIntent(context))
        } else {
            legacyReadLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                    onGrantAccess = ::requestStorageAccess,
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

            if (state.folders.isNotEmpty()) {
                item {
                    val empty = state.folders.count { it.status == com.riccardopinato.omnicleaner.cleaner.FolderStatus.COMPLETELY_EMPTY }
                    ResultHeader(
                        title = stringResource(R.string.module_folders),
                        subtitle = stringResource(R.string.folder_summary, state.folders.size, empty),
                    )
                }
                items(state.folders.take(30), key = { it.path }) { folder ->
                    ResultRow(
                        title = folder.name,
                        value = "${formatBytes(folder.sizeBytes)} · ${folder.fileCount}",
                    )
                }
            }

            if (state.bigFiles.isNotEmpty()) {
                item {
                    ResultHeader(
                        title = stringResource(R.string.module_large_files),
                        subtitle = stringResource(R.string.big_files_summary, state.bigFiles.size),
                    )
                }
                items(state.bigFiles.take(40), key = { it.path }) { item ->
                    ResultRow(item.name, formatBytes(item.sizeBytes))
                }
            }

            if (state.duplicates.isNotEmpty()) {
                item {
                    val waste = state.duplicates.sumOf { it.totalWasteBytes }
                    ResultHeader(
                        title = stringResource(R.string.module_duplicates),
                        subtitle = stringResource(
                            R.string.duplicates_summary,
                            state.duplicates.size,
                            formatBytes(waste),
                        ),
                    )
                }
                items(state.duplicates.take(30), key = { it.hash }) { group ->
                    ResultRow(
                        title = group.files.firstOrNull()?.name ?: group.hash.take(12),
                        value = "${group.files.size} × ${formatBytes(group.sizeBytes)}",
                    )
                }
            }

            item {
                GalleryCleanerSection()
            }

            item {
                ExifCleanerSection()
            }

            item { Spacer(Modifier.height(20.dp)) }
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
                if (state.hasBroadAccess) stringResource(R.string.access_ready)
                else stringResource(R.string.access_required),
                color = if (state.hasBroadAccess) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.hasBroadAccess) {
                    Button(onClick = onGrantAccess) {
                        Text(stringResource(R.string.grant_access))
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
