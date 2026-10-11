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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.AppResidueItem
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.ThumbnailCacheItem
import com.riccardopinato.omnicleaner.cleaner.ThumbnailFileCleanup
import com.riccardopinato.omnicleaner.monetization.CleanupEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

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
    private var operationGeneration = 0L

    fun scanThumbnails(label: String, accessRequired: String) = run(label, accessRequired) {
        state = state.copy(
            thumbnails = scanner.scanThumbnailCache(StorageAccess.primaryRoot()),
        )
    }

    fun scanResidues(label: String, accessRequired: String) = run(label, accessRequired) {
        val result = scanner.scanAppResidues(context, StorageAccess.primaryRoot())
        state = state.copy(
            residues = result.items,
            residueVisibilityWarning = result.packageVisibilityMayBeRestricted,
        )
    }

    fun cleanThumbnail(
        item: ThumbnailCacheItem,
        label: String,
        accessRequired: String,
        resultMessage: (removed: Int, bytes: Long, rejected: Int) -> String,
    ) = run(label, accessRequired) {
        val result = thumbnailCleanup.clean(item.inventory)
        val refreshed = scanner.scanThumbnailCache(StorageAccess.primaryRoot())
        state = state.copy(
            thumbnails = refreshed,
            message = resultMessage(result.removed, result.removedBytes, result.rejected),
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

    private fun run(label: String, accessRequired: String, block: suspend () -> Unit) {
        if (!StorageAccess.hasBroadAccess(context)) {
            state = state.copy(message = accessRequired)
            return
        }
        job?.cancel()
        val generation = ++operationGeneration
        job = scope.launch {
            state = state.copy(running = true, operation = label, message = null)
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                state = state.copy(message = error.message ?: error.javaClass.simpleName)
            } finally {
                if (generation == operationGeneration) {
                    state = state.copy(running = false, operation = null)
                }
            }
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

    val thumbnailLabel = stringResource(R.string.maintenance_thumbnail_cache)
    val residueLabel = stringResource(R.string.maintenance_app_residues)
    val cleaningThumbnailLabel = stringResource(R.string.maintenance_cleaning_thumbnails)
    val accessRequired = stringResource(R.string.maintenance_access_required)
    val cleanupResultTemplate = stringResource(R.string.maintenance_cleanup_result)
    val cleanupResultMessage: (Int, Long, Int) -> String = { removed, bytes, rejected ->
        String.format(Locale.getDefault(), cleanupResultTemplate, removed, bytes, rejected)
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
            Text(stringResource(R.string.maintenance_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.maintenance_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { controller.scanThumbnails(thumbnailLabel, accessRequired) },
                    enabled = !state.running,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(thumbnailLabel)
                }
                OutlinedButton(
                    onClick = { controller.scanResidues(residueLabel, accessRequired) },
                    enabled = !state.running,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(residueLabel)
                }
            }

            OutlinedTextField(
                value = state.thumbnailSearchQuery,
                onValueChange = controller::updateThumbnailSearchQuery,
                label = { Text(stringResource(R.string.maintenance_search_thumbnails)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.residueSearchQuery,
                onValueChange = controller::updateResidueSearchQuery,
                label = { Text(stringResource(R.string.maintenance_search_residues)) },
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
                        Text(stringResource(R.string.maintenance_clean_verified))
                    }
                }
            }

            if (state.visibleResidues.isNotEmpty()) {
                Text(stringResource(R.string.maintenance_residue_candidates), style = MaterialTheme.typography.titleMedium)
                if (state.residueVisibilityWarning) {
                    Text(
                        stringResource(R.string.maintenance_residue_warning),
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
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }

    pendingThumbnail?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingThumbnail = null },
            title = { Text(stringResource(R.string.maintenance_confirm_cache_title)) },
            text = {
                Text(stringResource(R.string.maintenance_confirm_cache_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingThumbnail = null
                        controller.cleanThumbnail(item, cleaningThumbnailLabel, accessRequired, cleanupResultMessage)
                    },
                ) {
                    Text(stringResource(R.string.action_clean))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingThumbnail = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
