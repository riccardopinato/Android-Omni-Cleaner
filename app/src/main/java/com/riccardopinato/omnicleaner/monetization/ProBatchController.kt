package com.riccardopinato.omnicleaner.monetization

import android.app.PendingIntent
import android.content.Context
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.cleaner.ThumbnailFileCleanup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProBatchController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val scanner: FileScanner = FileScanner(),
    private val cleanupEngine: StorageCleanupEngine = StorageCleanupEngine(context),
    private val thumbnailCleanup: ThumbnailFileCleanup = ThumbnailFileCleanup(),
) {
    var state by androidx.compose.runtime.mutableStateOf(ProBatchState())
        private set

    private var job: Job? = null
    private var cleanupChangedAnything: Boolean = false

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
            state = ProBatchState(running = true)
            val completed = linkedSetOf<ProScanType>()
            val results = arrayListOf<ProBatchResult>()

            try {
                for (type in ProScanType.entries.filter { it in selectedTypes }) {
                    when (type) {
                        ProScanType.EMPTY_FOLDERS -> {
                            val items = scanner.scanFolders(StorageAccess.primaryRoot())
                                .filter { it.status == FolderStatus.COMPLETELY_EMPTY }
                            results += ProBatchResult(type, items.size, items.sumOf { it.sizeBytes })
                            state = state.copy(
                                emptyFolders = items,
                                selectedFolderPaths = items.mapTo(linkedSetOf()) { it.path },
                            )
                        }

                        ProScanType.DUPLICATES -> {
                            val groups = scanner.findDuplicates(StorageAccess.primaryRoot())
                            results += ProBatchResult(
                                type,
                                groups.sumOf { it.totalDuplicatesCount },
                                groups.sumOf { it.totalWasteBytes },
                            )
                            state = state.copy(
                                duplicateGroups = groups,
                                selectedDuplicatePaths = cleanupEngine.selectDuplicatesExceptOldest(groups),
                            )
                        }

                        ProScanType.LARGE_FILES -> {
                            val items = scanner.findBigFiles(StorageAccess.primaryRoot())
                            results += ProBatchResult(type, items.size, items.sumOf { it.sizeBytes })
                            state = state.copy(
                                largeFiles = items,
                                selectedLargeFilePaths = emptySet(),
                            )
                        }

                        ProScanType.THUMBNAIL_CACHE -> {
                            val items = scanner.scanThumbnailCache(StorageAccess.primaryRoot())
                            results += ProBatchResult(
                                type,
                                items.sumOf { it.fileCount },
                                items.sumOf { it.sizeBytes },
                            )
                            state = state.copy(
                                thumbnails = items,
                                selectedThumbnailPaths = items.mapTo(linkedSetOf()) { it.path },
                            )
                        }

                        ProScanType.APP_RESIDUES -> {
                            val scan = scanner.scanAppResidues(context, StorageAccess.primaryRoot())
                            results += ProBatchResult(
                                type,
                                scan.items.size,
                                scan.items.sumOf { it.sizeBytes },
                            )
                            state = state.copy(residues = scan.items)
                        }
                    }

                    completed += type
                    state = state.copy(
                        running = true,
                        completedTypes = completed.toSet(),
                        results = results.toList(),
                        totalRecoverableBytes = results.sumOf { it.candidateBytes },
                    )
                }

                state = state.copy(
                    running = false,
                    hasCompletedScan = true,
                    reviewVisible = true,
                    message = null,
                    error = null,
                )
            } catch (error: CancellationException) {
                state = state.copy(running = false)
            } catch (error: Throwable) {
                state = state.copy(
                    running = false,
                    error = error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    fun toggleFolder(path: String, checked: Boolean) {
        state = state.copy(selectedFolderPaths = state.selectedFolderPaths.toggle(path, checked))
    }

    fun toggleDuplicate(path: String, checked: Boolean) {
        state = state.copy(selectedDuplicatePaths = state.selectedDuplicatePaths.toggle(path, checked))
    }

    fun toggleLargeFile(path: String, checked: Boolean) {
        state = state.copy(selectedLargeFilePaths = state.selectedLargeFilePaths.toggle(path, checked))
    }

    fun toggleThumbnail(path: String, checked: Boolean) {
        state = state.copy(selectedThumbnailPaths = state.selectedThumbnailPaths.toggle(path, checked))
    }

    fun selectDuplicateKeepOldest() {
        state = state.copy(
            selectedDuplicatePaths = cleanupEngine.selectDuplicatesExceptOldest(state.duplicateGroups),
        )
    }

    fun selectDuplicateKeepNewest() {
        state = state.copy(
            selectedDuplicatePaths = cleanupEngine.selectDuplicatesExceptNewest(state.duplicateGroups),
        )
    }

    fun resolve(onSystemTrashRequired: (PendingIntent) -> Unit) {
        if (state.resolving || state.awaitingSystemTrash) return

        job?.cancel()
        job = scope.launch {
            state = state.copy(resolving = true, message = null, error = null)
            cleanupChangedAnything = false

            try {
                val folders = state.emptyFolders
                    .filter { it.path in state.selectedFolderPaths }
                    .map { it.file }
                val folderResult = cleanupEngine.deleteEmptyFolders(folders)
                if (folderResult.removed > 0) cleanupChangedAnything = true

                var thumbnailRemoved = 0
                var thumbnailRejected = 0
                for (item in state.thumbnails.filter { it.path in state.selectedThumbnailPaths }) {
                    val result = thumbnailCleanup.clean(item.inventory)
                    thumbnailRemoved += result.removed
                    thumbnailRejected += result.rejected
                }
                if (thumbnailRemoved > 0) cleanupChangedAnything = true

                val duplicateFiles = cleanupEngine.protectedDuplicateFiles(
                    state.duplicateGroups,
                    state.selectedDuplicatePaths,
                )
                val largeFiles = state.largeFiles
                    .filter { it.path in state.selectedLargeFilePaths }
                    .map { it.file }

                val preparation = cleanupEngine.prepareFiles(duplicateFiles + largeFiles)
                val safeTrash = cleanupEngine.movePreparedNonMediaToSafeTrash(preparation.safeTrashFiles)
                if (safeTrash.moved > 0) cleanupChangedAnything = true

                val mediaRequest = cleanupEngine.createSystemTrashRequest(preparation.mediaUris)
                val residueMessage = if (state.residues.isNotEmpty()) {
                    " · ${state.residues.size} residui lasciati in review per sicurezza package visibility"
                } else {
                    ""
                }
                val blockedMessage = if (preparation.blockedFiles.isNotEmpty()) {
                    " · ${preparation.blockedFiles.size} file non eliminati perché non reversibili"
                } else {
                    ""
                }

                state = state.copy(
                    resolving = false,
                    awaitingSystemTrash = mediaRequest != null,
                    message = buildString {
                        append(folderResult.removed)
                        append(" cartelle vuote · ")
                        append(thumbnailRemoved)
                        append(" thumbnail · ")
                        append(safeTrash.moved)
                        append(" file in Safe Trash")
                        if (thumbnailRejected > 0) append(" · $thumbnailRejected thumbnail rifiutate")
                        append(blockedMessage)
                        append(residueMessage)
                    },
                )

                if (mediaRequest != null) {
                    onSystemTrashRequired(mediaRequest)
                } else {
                    finishResolve(systemTrashConfirmed = false)
                }
            } catch (error: CancellationException) {
                state = state.copy(resolving = false)
            } catch (error: Throwable) {
                state = state.copy(
                    resolving = false,
                    awaitingSystemTrash = false,
                    error = error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    fun onSystemTrashResult(success: Boolean) {
        if (!state.awaitingSystemTrash) return
        if (success) cleanupChangedAnything = true
        state = state.copy(
            awaitingSystemTrash = false,
            message = buildString {
                append(state.message.orEmpty())
                if (success) append(" · cestino Android confermato")
                else append(" · cestino Android annullato")
            },
        )
        finishResolve(systemTrashConfirmed = success)
    }

    fun clearMessage() {
        state = state.copy(message = null, error = null)
    }

    fun cancel() {
        job?.cancel()
        job = null
        state = state.copy(running = false, resolving = false)
    }

    private fun finishResolve(systemTrashConfirmed: Boolean) {
        if (cleanupChangedAnything || systemTrashConfirmed) {
            CleanupEventBus.publishCompleted()
        }
    }

    private fun Set<String>.toggle(path: String, checked: Boolean): Set<String> =
        if (checked) this + path else this - path
}
