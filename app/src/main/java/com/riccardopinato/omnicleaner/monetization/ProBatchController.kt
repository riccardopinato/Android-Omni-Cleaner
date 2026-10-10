package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProBatchController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val scanner: FileScanner = FileScanner(),
) {
    var state by mutableStateOf(ProBatchState())
        private set
    private var job: Job? = null

    fun scan(selectedTypes: Set<ProScanType>) {
        if (!StorageAccess.hasBroadAccess(context)) {
            state = state.copy(error = "Serve l’accesso alla memoria per la scansione Pro.")
            return
        }
        if (selectedTypes.isEmpty()) {
            state = state.copy(error = "Seleziona almeno una categoria.")
            return
        }
        job?.cancel()
        job = scope.launch {
            val results = arrayListOf<ProBatchResult>()
            val completed = linkedSetOf<ProScanType>()
            state = ProBatchState(running = true)
            runCatching {
                for (type in selectedTypes) {
                    results += scanType(type)
                    completed += type
                    state = ProBatchState(
                        running = true,
                        completedTypes = completed.toSet(),
                        results = results.toList(),
                    )
                }
            }.onFailure { error ->
                if (error !is kotlinx.coroutines.CancellationException) {
                    state = state.copy(error = error.message ?: error.javaClass.simpleName)
                }
            }
            state = state.copy(running = false)
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        state = state.copy(running = false)
    }

    private suspend fun scanType(type: ProScanType): ProBatchResult {
        val root = StorageAccess.primaryRoot()
        return when (type) {
            ProScanType.EMPTY_FOLDERS -> {
                val items = scanner.scanFolders(root).filter {
                    it.status == FolderStatus.COMPLETELY_EMPTY
                }
                ProBatchResult(type, items.size, items.sumOf { it.sizeBytes })
            }
            ProScanType.DUPLICATES -> {
                val groups = scanner.findDuplicates(root)
                ProBatchResult(
                    type,
                    groups.sumOf { it.totalDuplicatesCount },
                    groups.sumOf { it.totalWasteBytes },
                )
            }
            ProScanType.LARGE_FILES -> {
                val items = scanner.findBigFiles(root)
                ProBatchResult(type, items.size, items.sumOf { it.sizeBytes })
            }
            ProScanType.THUMBNAIL_CACHE -> {
                val items = scanner.scanThumbnailCache(root)
                ProBatchResult(type, items.sumOf { it.fileCount }, items.sumOf { it.sizeBytes })
            }
            ProScanType.APP_RESIDUES -> {
                val scan = scanner.scanAppResidues(context, root)
                ProBatchResult(type, scan.items.size, scan.items.sumOf { it.sizeBytes })
            }
        }
    }
}
