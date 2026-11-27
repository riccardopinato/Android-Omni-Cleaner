package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FileCleanupPreparation
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.StorageAccess
import com.riccardopinato.omnicleaner.cleaner.StorageCleanupEngine
import com.riccardopinato.omnicleaner.cleaner.MediaStoreHelper
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
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
    private val thumbnailCleanup: ThumbnailFileCleanup = ThumbnailFileCleanup(
        com.riccardopinato.omnicleaner.cleaner.SafeTrashManager(context),
    ),
) {
    private var uiContext: Context = context

    fun updateUiContext(localizedContext: Context) {
        uiContext = localizedContext
        state = state.copy(message = null, error = null)
    }

    var state by androidx.compose.runtime.mutableStateOf(ProBatchState())
        private set

    private var job: Job? = null
    private var cleanupChangedAnything: Boolean = false
    private var pendingMediaUris: List<Uri> = emptyList()
    private var pendingMediaPreparation: FileCleanupPreparation? = null
    private var pendingLocalReview: ProBatchState? = null

    fun scan(selectedTypes: Set<ProScanType>) {
        if (state.resolving || state.awaitingSystemTrash) return
        if (!StorageAccess.hasBroadAccess(context)) {
            state = state.copy(error = uiContext.getString(R.string.pro_error_access_required))
            return
        }
        if (selectedTypes.isEmpty()) {
            state = state.copy(error = uiContext.getString(R.string.pro_error_select_category))
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
                                selectedFolderPaths = emptySet(),
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
                                selectedDuplicatePaths = emptySet(),
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
                                selectedThumbnailPaths = emptySet(),
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
            } catch (_: Throwable) {
                state = state.copy(
                    running = false,
                    error = uiContext.getString(R.string.pro_scan_failed),
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

    /**
     * A mixed Pro batch must not change ANY local file before the Android
     * media prompt has been approved and independently verified.
     */
    fun resolve() {
        if (state.resolving || state.awaitingSystemTrash) return
        val reviewed = state
        if (reviewed.selectedFolderPaths.isEmpty() &&
            reviewed.selectedDuplicatePaths.isEmpty() &&
            reviewed.selectedLargeFilePaths.isEmpty() &&
            reviewed.selectedThumbnailPaths.isEmpty()
        ) return

        job?.cancel()
        job = scope.launch {
            state = state.copy(resolving = true, message = null, error = null)
            cleanupChangedAnything = false
            try {
                val duplicateFiles = cleanupEngine.protectedDuplicateSnapshots(
                    reviewed.duplicateGroups, reviewed.selectedDuplicatePaths,
                )
                val largeFiles = cleanupEngine.snapshotsForBigFiles(
                    reviewed.largeFiles, reviewed.selectedLargeFilePaths,
                )
                val preparation = cleanupEngine.prepareSnapshots(duplicateFiles + largeFiles)
                if (preparation.mediaUris.isNotEmpty()) {
                    val request = cleanupEngine.createSystemTrashRequest(preparation)
                    if (request == null) {
                        state = state.copy(resolving = false,
                            error = uiContext.getString(R.string.cleanup_system_trash_unavailable))
                        return@launch
                    }
                    pendingMediaUris = preparation.mediaUris
                    pendingMediaPreparation = preparation
                    pendingLocalReview = reviewed
                    state = state.copy(
                        resolving = false,
                        awaitingSystemTrash = true,
                        pendingSystemTrashRequest = request,
                        message = uiContext.getString(R.string.pro_confirm_body),
                    )
                    // Absolutely no local mutation occurred before OS consent.
                } else {
                    val message = applyLocalReviewedPlan(reviewed, preparation)
                    state = state.copy(resolving = false, message = message)
                    finishResolve()
                }
            } catch (cancelled: CancellationException) {
                state = state.copy(resolving = false)
                throw cancelled
            } catch (_: Throwable) {
                state = state.copy(resolving = false,
                    error = uiContext.getString(R.string.pro_scan_failed))
            }
        }
    }

    private suspend fun applyLocalReviewedPlan(
        reviewed: ProBatchState,
        preparation: FileCleanupPreparation,
    ): String {
        val folders = reviewed.emptyFolders.filter {
            it.path in reviewed.selectedFolderPaths
        }.map { it.file }
        val folderResult = cleanupEngine.deleteEmptyFolders(folders)
        if (folderResult.removed > 0) cleanupChangedAnything = true

        var thumbMoved = 0
        var thumbRejected = 0
        var thumbBytes = 0L
        for (item in reviewed.thumbnails.filter { it.path in reviewed.selectedThumbnailPaths }) {
            val result = thumbnailCleanup.clean(item.inventory)
            thumbMoved += result.removed
            thumbRejected += result.rejected
            thumbBytes += result.removedBytes
        }
        if (thumbMoved > 0) {
            cleanupChangedAnything = true
            StorageInventoryRepository.invalidate()
        }
        val moved = cleanupEngine.movePreparedSnapshotsToSafeTrash(
            preparation.safeTrashCandidates,
        )
        if (moved.moved > 0) cleanupChangedAnything = true
        val lines = mutableListOf<String>()
        lines += uiContext.getString(R.string.pro_resolve_empty_folders, folderResult.removed)
        lines += uiContext.getString(R.string.pro_resolve_thumbnails, thumbMoved)
        if (thumbBytes > 0L) {
            lines += uiContext.getString(R.string.pro_resolve_thumbnail_bytes, thumbBytes)
        }
        lines += uiContext.getString(R.string.pro_resolve_safe_trash, moved.moved)
        if (thumbRejected > 0) {
            lines += uiContext.getString(R.string.pro_resolve_thumbnail_rejected, thumbRejected)
        }
        if (moved.rejected > 0) {
            lines += uiContext.getString(R.string.pro_resolve_blocked, moved.rejected)
        }
        if (preparation.staleFiles.isNotEmpty()) {
            lines += uiContext.getString(R.string.pro_resolve_stale, preparation.staleFiles.size)
        }
        if (preparation.blockedFiles.isNotEmpty()) {
            lines += uiContext.getString(R.string.pro_resolve_blocked, preparation.blockedFiles.size)
        }
        if (reviewed.residues.isNotEmpty()) {
            lines += uiContext.getString(R.string.pro_resolve_residues, reviewed.residues.size)
        }
        return lines.joinToString(" · ")
    }

    fun markSystemTrashLaunched() {
        if (!state.awaitingSystemTrash) return
        state = state.copy(pendingSystemTrashRequest = null)
    }

    fun onSystemTrashResult(success: Boolean) {
        if (!state.awaitingSystemTrash) return
        val attempted = pendingMediaUris
        val preparation = pendingMediaPreparation
        val reviewed = pendingLocalReview
        pendingMediaUris = emptyList()
        pendingMediaPreparation = null
        pendingLocalReview = null
        if (!success || attempted.isEmpty() || preparation == null || reviewed == null) {
            state = state.copy(
                awaitingSystemTrash = false, pendingSystemTrashRequest = null,
                message = uiContext.getString(R.string.pro_system_trash_cancelled),
            )
            if (preparation != null) {
                scope.launch { cleanupEngine.recordMediaTrashOutcome(preparation, null) }
            }
            return // No local file has been changed.
        }
        scope.launch {
            try {
                val verified = MediaStoreHelper.verifyTrashedUris(
                    context.contentResolver, attempted,
                )
                cleanupEngine.recordMediaTrashOutcome(preparation, verified)
                val successSummary = uiContext.getString(
                    R.string.cleanup_media_verified_summary,
                    verified.confirmedUris.size, verified.unverifiedUris.size,
                )
                if (verified.confirmedUris.isNotEmpty()) cleanupChangedAnything = true
                // If Android applied only a subset, retain the reviewed local
                // selection and require a fresh user decision. Never pretend
                // that the mixed operation was atomic.
                val localSummary = if (verified.unverifiedUris.isEmpty()) {
                    applyLocalReviewedPlan(reviewed, preparation)
                } else uiContext.getString(R.string.cleanup_system_trash_unavailable)
                state = state.copy(
                    awaitingSystemTrash = false, pendingSystemTrashRequest = null,
                    message = successSummary + " · " + localSummary,
                )
                finishResolve()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(
                    awaitingSystemTrash = false, pendingSystemTrashRequest = null,
                    error = uiContext.getString(R.string.cleanup_system_trash_unavailable),
                )
            }
        }
    }

    fun clearMessage() {
        state = state.copy(message = null, error = null)
    }

    fun cancel() {
        job?.cancel()
        job = null
        pendingMediaUris = emptyList()
        pendingMediaPreparation = null
        pendingLocalReview = null
        state = state.copy(
            running = false,
            resolving = false,
            awaitingSystemTrash = false,
            pendingSystemTrashRequest = null,
        )
    }

    private fun finishResolve() {
        // Only independently observed local file/folder mutations count.
        if (cleanupChangedAnything) CleanupEventBus.publishCompleted()
    }

    private fun Set<String>.toggle(path: String, checked: Boolean): Set<String> =
        if (checked) this + path else this - path
}
