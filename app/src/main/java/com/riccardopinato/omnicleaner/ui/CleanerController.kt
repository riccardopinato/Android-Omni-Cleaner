package com.riccardopinato.omnicleaner.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riccardopinato.omnicleaner.cleaner.BigFileItem
import com.riccardopinato.omnicleaner.cleaner.DuplicateGroup
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FolderItem
import com.riccardopinato.omnicleaner.cleaner.SpaceOverview
import com.riccardopinato.omnicleaner.cleaner.SpaceOverviewScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class CleanerUiState(
    val hasBroadAccess: Boolean = false,
    val storageInfo: StorageInfo? = null,
    val scanning: Boolean = false,
    val currentOperation: String? = null,
    val overview: SpaceOverview? = null,
    val folders: List<FolderItem> = emptyList(),
    val bigFiles: List<BigFileItem> = emptyList(),
    val duplicates: List<DuplicateGroup> = emptyList(),
    val error: String? = null,
)

internal class CleanerController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val fileScanner: FileScanner = FileScanner(),
    private val overviewScanner: SpaceOverviewScanner = SpaceOverviewScanner(),
) {
    var state by mutableStateOf(CleanerUiState())
        private set

    private var scanJob: Job? = null

    init {
        refreshAccessAndStorage()
    }

    fun refreshAccessAndStorage() {
        state = state.copy(
            hasBroadAccess = StorageAccess.hasBroadAccess(context),
            storageInfo = runCatching { StorageAccess.storageInfo() }.getOrNull(),
            error = null,
        )
    }

    fun scanOverview() = runScan("Storage overview") {
        val overview = overviewScanner.scan(StorageAccess.primaryRoot())
        state = state.copy(overview = overview)
    }

    fun scanFolders() = runScan("Folders") {
        val folders = fileScanner.scanFolders(StorageAccess.primaryRoot())
        state = state.copy(folders = folders)
    }

    fun scanBigFiles(minimumBytes: Long = 100L * 1024L * 1024L) = runScan("Large files") {
        val files = fileScanner.findBigFiles(StorageAccess.primaryRoot(), minimumBytes)
        state = state.copy(bigFiles = files)
    }

    fun scanDuplicates() = runScan("Duplicates") {
        val groups = fileScanner.findDuplicates(StorageAccess.primaryRoot())
        state = state.copy(duplicates = groups)
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        state = state.copy(scanning = false, currentOperation = null)
    }

    private fun runScan(label: String, block: suspend () -> Unit) {
        if (!state.hasBroadAccess) {
            state = state.copy(error = "Storage access is required before scanning.")
            return
        }
        scanJob?.cancel()
        scanJob = scope.launch {
            state = state.copy(scanning = true, currentOperation = label, error = null)
            runCatching { block() }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        state = state.copy(error = error.message ?: error.javaClass.simpleName)
                    }
                }
            state = state.copy(scanning = false, currentOperation = null)
        }
    }
}
