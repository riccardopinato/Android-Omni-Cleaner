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
import com.riccardopinato.omnicleaner.cleaner.PhysicalPerformanceBenchmark
import com.riccardopinato.omnicleaner.cleaner.PhysicalPerformanceBenchmarkResult
import com.riccardopinato.omnicleaner.cleaner.SpaceOverview
import com.riccardopinato.omnicleaner.cleaner.SpaceOverviewScanner
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageCapabilityHealth
import com.riccardopinato.omnicleaner.cleaner.StorageInfo
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import com.riccardopinato.omnicleaner.cleaner.ThresholdUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val COMMON_FILE_EXTENSION_FILTERS: List<String> = listOf(
    "apk", "pdf", "zip", "rar", "7z",
    "jpg", "jpeg", "png", "webp",
    "mp4", "mkv", "mov",
    "mp3", "flac",
    "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "csv",
)

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
    val fileExtensionFilter: String? = null,
    val duplicateScanExtension: String? = null,
    val bigFilesScanExtension: String? = null,
    val duplicateProgressProcessed: Int = 0,
    val duplicateProgressTotal: Int = 0,
    val indexedFilesChecked: Int = 0,
    val bigFileSizeFilter: BigFileSizeFilter = BigFileSizeFilter.SIZE_100MB,
    val performanceBenchmark: PhysicalPerformanceBenchmarkResult? = null,
    val thresholdValue: String = "10",
    val thresholdUnit: ThresholdUnit = ThresholdUnit.MB,
    val error: String? = null,
) {
    val visibleFolders: List<FolderItem>
        get() = folders.filterByQuery(folderSearchQuery) { listOf(it.name, it.path) }

    val visibleBigFiles: List<BigFileItem>
        get() = bigFiles
            .filterByExtension(fileExtensionFilter) { it.extension }
            .filterByQuery(bigFileSearchQuery) { listOf(it.name, it.path, it.extension) }

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
            val extensionFiltered = duplicates.filter { group ->
                fileExtensionFilter == null ||
                    group.files.any { it.extension.equals(fileExtensionFilter, ignoreCase = true) }
            }
            if (normalized.isBlank()) return extensionFiltered
            return extensionFiltered.filter { group ->
                group.files.any { file ->
                    file.name.lowercase().contains(normalized) ||
                        file.path.lowercase().contains(normalized) ||
                        file.extension.lowercase().contains(normalized)
                }
            }
        }
}

private inline fun <T> List<T>.filterByExtension(
    extension: String?,
    extensionOf: (T) -> String,
): List<T> {
    val normalized = extension?.trim()?.removePrefix(".")?.lowercase()
    if (normalized.isNullOrBlank()) return this
    return filter { item -> extensionOf(item).lowercase() == normalized }
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
    private var uiContext: Context = context

    fun updateUiContext(localizedContext: Context) {
        uiContext = localizedContext
        state = state.copy(currentOperation = null, error = null)
    }

    var state by mutableStateOf(CleanerUiState())
        private set

    private var scanJob: Job? = null
    private var scanGeneration = 0L

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

    fun scanOverview() = runScan(uiContext.getString(R.string.module_storage_overview)) {
        val overview = overviewScanner.scan(StorageAccess.primaryRoot())
        state = state.copy(overview = overview)
    }

    fun refreshPersistentIndex() =
        runScan(uiContext.getString(R.string.refresh_storage_index)) {
            val root = StorageAccess.primaryRoot()
            StorageInventoryRepository.snapshot(
                root = root,
                forceRefresh = true,
                onProgress = { checked, _ ->
                    if (checked % 512 == 0) {
                        scope.launch {
                            if (state.scanning) {
                                state = state.copy(indexedFilesChecked = checked)
                            }
                        }
                    }
                },
            )
            val overview = overviewScanner.scan(root)
            state = state.copy(overview = overview)
        }

    fun runPhysicalPerformanceBenchmark() =
        runScan(uiContext.getString(R.string.performance_benchmark_running)) {
            state = state.copy(performanceBenchmark = null)
            val benchmark = PhysicalPerformanceBenchmark(context)
            val result = benchmark.run(
                root = StorageAccess.primaryRoot(),
            )
            state = state.copy(performanceBenchmark = result)
        }

    fun scanFolders() = runScan(uiContext.getString(R.string.module_folders)) {
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
    ) = runScan(uiContext.getString(R.string.module_large_files)) {
        val selectedExtension = state.fileExtensionFilter
        val files = fileScanner.findBigFiles(
            root = StorageAccess.primaryRoot(),
            minimumBytes = minimumBytes,
            extensionFilter = selectedExtension,
        )
        state = state.copy(
            bigFiles = files,
            bigFilesScanned = true,
            bigFilesScanExtension = selectedExtension,
        )
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

    fun updateFileExtensionFilter(extension: String?) {
        val normalized = extension
            ?.trim()
            ?.removePrefix(".")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        val duplicateScopeChanged =
            state.duplicatesScanned &&
                state.duplicateScanExtension != normalized
        val bigFileScopeChanged =
            state.bigFilesScanned &&
                state.bigFilesScanExtension != normalized

        state = state.copy(
            fileExtensionFilter = normalized,
            duplicates = if (duplicateScopeChanged) emptyList() else state.duplicates,
            duplicatesScanned = if (duplicateScopeChanged) false else state.duplicatesScanned,
            bigFiles = if (bigFileScopeChanged) emptyList() else state.bigFiles,
            bigFilesScanned = if (bigFileScopeChanged) false else state.bigFilesScanned,
            duplicateProgressProcessed = 0,
            duplicateProgressTotal = 0,
        )
    }

    fun updateBigFileSizeFilter(filter: BigFileSizeFilter) {
        state = state.copy(bigFileSizeFilter = filter)
        if (state.hasBroadAccess) scanBigFiles(filter.minimumBytes)
    }

    fun scanDuplicates() = runScan(uiContext.getString(R.string.module_duplicates)) {
        val selectedExtension = state.fileExtensionFilter
        state = state.copy(
            duplicateProgressProcessed = 0,
            duplicateProgressTotal = 0,
            indexedFilesChecked = 0,
            duplicatesScanned = false,
            duplicateScanExtension = selectedExtension,
        )

        val groups = fileScanner.findDuplicates(
            root = StorageAccess.primaryRoot(),
            extensionFilter = selectedExtension,
            onInventoryProgress = { checked, _ ->
                if (checked % 512 == 0) {
                    scope.launch {
                        if (state.scanning) {
                            state = state.copy(indexedFilesChecked = checked)
                        }
                    }
                }
            },
            onDuplicateProgress = { processed, total ->
                scope.launch {
                    if (state.scanning) {
                        state = state.copy(
                            duplicateProgressProcessed = processed,
                            duplicateProgressTotal = total,
                        )
                    }
                }
            },
        )

        state = state.copy(
            duplicates = groups,
            duplicatesScanned = true,
            duplicateProgressProcessed = state.duplicateProgressTotal,
        )
    }

    fun cancelScan() {
        scanGeneration++
        scanJob?.cancel()
        scanJob = null
        state = state.copy(scanning = false, currentOperation = null)
    }

    private fun runScan(label: String, block: suspend () -> Unit) {
        if (!state.hasBroadAccess) {
            state = state.copy(error = uiContext.getString(R.string.deep_cleaning_access_required))
            return
        }
        scanJob?.cancel()
        val generation = ++scanGeneration
        scanJob = scope.launch {
            state = state.copy(
                scanning = true,
                currentOperation = label,
                duplicateProgressProcessed = 0,
                duplicateProgressTotal = 0,
                indexedFilesChecked = 0,
                error = null,
            )
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                state = state.copy(error = uiContext.getString(R.string.scan_error))
            } finally {
                if (generation == scanGeneration) {
                    state = state.copy(scanning = false, currentOperation = null)
                }
            }
        }
    }
}
