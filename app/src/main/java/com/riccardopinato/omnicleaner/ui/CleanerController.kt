package com.riccardopinato.omnicleaner.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.BigFileItem
import com.riccardopinato.omnicleaner.cleaner.BigFileSizeFilter
import com.riccardopinato.omnicleaner.cleaner.DuplicateGroup
import com.riccardopinato.omnicleaner.cleaner.DeepStorageAccessStatus
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FolderItem
import com.riccardopinato.omnicleaner.cleaner.SpaceOverview
import com.riccardopinato.omnicleaner.cleaner.SpaceOverviewScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageCapabilityHealth
import com.riccardopinato.omnicleaner.cleaner.StorageInfo
import com.riccardopinato.omnicleaner.cleaner.ThresholdUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class CleanerUiState(
    val hasBroadAccess: Boolean = false,
    val storageCapability: StorageCapabilityHealth = StorageCapabilityHealth(
        status = DeepStorageAccessStatus.STORAGE_UNAVAILABLE,
        standardCleaningAvailable = false,
        deepScanAvailable = false,
        canRequestDeepAccess = false,
    ),
    val storageInfo: StorageInfo? = null,
    val scanning: Boolean = false,
    val currentOperation: String? = null,
    val overview: SpaceOverview? = null,
    val folders: List<FolderItem> = emptyList(),
    val bigFiles: List<BigFileItem> = emptyList(),
    val duplicates: List<DuplicateGroup> = emptyList(),
    val foldersScanned: Boolean = false,
    val bigFilesScanned: Boolean = false,
    val duplicatesScanned: Boolean = false,
    val folderSearchQuery: String = "",
    val bigFileSearchQuery: String = "",
    val duplicateSearchQuery: String = "",
    val bigFileSizeFilter: BigFileSizeFilter = BigFileSizeFilter.SIZE_100MB,
    val thresholdValue: String = "10",
    val thresholdUnit: ThresholdUnit = ThresholdUnit.MB,
    val error: String? = null,
) {
    val visibleFolders: List<FolderItem>
        get() = folders.filterByQuery(folderSearchQuery) { listOf(it.name, it.path) }

    val visibleBigFiles: List<BigFileItem>
        get() = bigFiles.filterByQuery(bigFileSearchQuery) { listOf(it.name, it.path, it.extension) }

    val folderThresholdBytes: Long
        get() = (thresholdValue.toLongOrNull() ?: 0L)
            .coerceAtLeast(0L)
            .let { value ->
                runCatching { Math.multiplyExact(value, thresholdUnit.multiplier) }
                    .getOrDefault(Long.MAX_VALUE)
            }

    val visibleDuplicates: List<DuplicateGroup>
        get() {
            val normalized = duplicateSearchQuery.trim().lowercase()
            if (normalized.isBlank()) return duplicates
            return duplicates.filter { group ->
                group.files.any { file ->
                    file.name.lowercase().contains(normalized) ||
                        file.path.lowercase().contains(normalized) ||
                        file.extension.lowercase().contains(normalized)
                }
            }
        }
}

private inline fun <T> List<T>.filterByQuery(
    query: String,
    fields: (T) -> List<String>,
): List<T> {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return this
    return filter { item ->
        fields(item).any { it.lowercase().contains(normalized) }
    }
}



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
        val capability = StorageAccess.capabilityHealth(context)
        val deepScanAvailable = capability.deepScanAvailable
        state = state.copy(
            hasBroadAccess = deepScanAvailable,
            storageCapability = capability,
            storageInfo = runCatching { StorageAccess.storageInfo() }.getOrNull(),
            overview = if (deepScanAvailable) state.overview else null,
            folders = if (deepScanAvailable) state.folders else emptyList(),
            bigFiles = if (deepScanAvailable) state.bigFiles else emptyList(),
            duplicates = if (deepScanAvailable) state.duplicates else emptyList(),
            foldersScanned = if (deepScanAvailable) state.foldersScanned else false,
            bigFilesScanned = if (deepScanAvailable) state.bigFilesScanned else false,
            duplicatesScanned = if (deepScanAvailable) state.duplicatesScanned else false,
            error = null,
        )
    }

    fun scanOverview() = runScan(context.getString(R.string.module_storage_overview)) {
        val overview = overviewScanner.scan(StorageAccess.primaryRoot())
        state = state.copy(overview = overview)
    }

    fun scanFolders() = runScan(context.getString(R.string.module_folders)) {
        val folders = fileScanner.scanFolders(
            root = StorageAccess.primaryRoot(),
            smallFolderThresholdBytes = state.folderThresholdBytes,
        )
        state = state.copy(folders = folders, foldersScanned = true)
    }

    fun updateThresholdValue(value: String) {
        val normalized = value.filter(Char::isDigit).take(9)
        state = state.copy(thresholdValue = normalized)
    }

    fun updateThresholdUnit(unit: ThresholdUnit) {
        state = state.copy(thresholdUnit = unit)
    }

    fun scanBigFiles(
        minimumBytes: Long = state.bigFileSizeFilter.minimumBytes,
    ) = runScan(context.getString(R.string.module_large_files)) {
        val files = fileScanner.findBigFiles(StorageAccess.primaryRoot(), minimumBytes)
        state = state.copy(bigFiles = files, bigFilesScanned = true)
    }

    fun updateFolderSearchQuery(query: String) {
        state = state.copy(folderSearchQuery = query)
    }

    fun updateBigFileSearchQuery(query: String) {
        state = state.copy(bigFileSearchQuery = query)
    }

    fun updateDuplicateSearchQuery(query: String) {
        state = state.copy(duplicateSearchQuery = query)
    }

    fun updateBigFileSizeFilter(filter: BigFileSizeFilter) {
        state = state.copy(bigFileSizeFilter = filter)
        if (state.hasBroadAccess) scanBigFiles(filter.minimumBytes)
    }

    fun scanDuplicates() = runScan(context.getString(R.string.module_duplicates)) {
        val groups = fileScanner.findDuplicates(StorageAccess.primaryRoot())
        state = state.copy(duplicates = groups, duplicatesScanned = true)
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        state = state.copy(scanning = false, currentOperation = null)
    }

    private fun runScan(label: String, block: suspend () -> Unit) {
        if (!state.hasBroadAccess) {
            state = state.copy(error = context.getString(R.string.deep_cleaning_access_required))
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
