package com.riccardopinato.omnicleaner.cleaner

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class EmptyFolderCleanupResult(
    val removed: Int,
    val rejected: Int,
)

data class FileCleanupPreparation(
    val mediaUris: List<Uri>,
    val safeTrashCandidates: List<CleanupFileSnapshot>,
    val blockedFiles: List<File>,
    val staleFiles: List<File>,
) {
    val safeTrashFiles: List<File>
        get() = safeTrashCandidates.map { it.file }
}

data class SafeTrashExecutionResult(
    val moved: Int,
    val rejected: Int,
    val movedBytes: Long,
)

class StorageCleanupEngine(
    private val context: Context,
    private val safeTrashManager: SafeTrashManager = SafeTrashManager(context),
) {
    suspend fun deleteEmptyFolders(
        folders: List<File>,
        allowedRoot: File = StorageAccess.primaryRoot(),
    ): EmptyFolderCleanupResult = withContext(Dispatchers.IO) {
        var removed = 0
        var rejected = 0

        for (folder in CleanupSelection.uniqueEntries(folders)) {
            coroutineContext.ensureActive()
            val safe = PathSafety.isSafeFolderToDelete(folder, allowedRoot)
            val stillEmpty = runCatching {
                folder.isDirectory && folder.listFiles()?.isEmpty() == true
            }.getOrDefault(false)

            if (!safe || !stillEmpty) {
                rejected++
                continue
            }

            if (folder.delete()) removed++ else rejected++
        }

        EmptyFolderCleanupResult(removed, rejected)
    }

    fun snapshotsForBigFiles(
        items: List<BigFileItem>,
        selectedPaths: Set<String>,
    ): List<CleanupFileSnapshot> =
        items
            .asSequence()
            .filter { it.path in selectedPaths }
            .map {
                CleanupFileSnapshot(
                    file = it.file,
                    expectedBytes = it.sizeBytes,
                    expectedModified = it.lastModified,
                    kind = CleanupCandidateKind.LARGE_FILE,
                )
            }
            .distinctBy { canonicalKey(it.file) }
            .toList()

    fun protectedDuplicateSnapshots(
        groups: List<DuplicateGroup>,
        selectedPaths: Set<String>,
    ): List<CleanupFileSnapshot> {
        val selected = ArrayList<CleanupFileSnapshot>()

        for (group in groups) {
            val selectedInGroup = group.files.filter { it.path in selectedPaths }
            if (selectedInGroup.isEmpty()) continue

            val comparator = compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path }
            val keeper = if (selectedInGroup.size == group.files.size) {
                group.files.minWithOrNull(comparator)
            } else {
                group.files
                    .filterNot { it.path in selectedPaths }
                    .minWithOrNull(comparator)
            } ?: continue

            val peer = CleanupPeerSnapshot(
                file = keeper.file,
                expectedBytes = keeper.sizeBytes,
                expectedModified = keeper.lastModified,
                expectedSha256 = group.hash,
            )

            selected += selectedInGroup
                .asSequence()
                .filterNot { it.path == keeper.path }
                .map {
                    CleanupFileSnapshot(
                        file = it.file,
                        expectedBytes = it.sizeBytes,
                        expectedModified = it.lastModified,
                        expectedSha256 = group.hash,
                        kind = CleanupCandidateKind.DUPLICATE,
                        requiredDuplicatePeer = peer,
                    )
                }
                .toList()
        }

        return selected.distinctBy { canonicalKey(it.file) }
    }

    fun protectedDuplicateFiles(
        groups: List<DuplicateGroup>,
        selectedPaths: Set<String>,
    ): List<File> =
        protectedDuplicateSnapshots(groups, selectedPaths).map { it.file }

    suspend fun prepareFiles(
        files: List<File>,
    ): FileCleanupPreparation =
        prepareSnapshots(
            files.mapNotNull { file ->
                if (!file.exists() || !file.isFile) return@mapNotNull null
                CleanupFileSnapshot(
                    file = file,
                    expectedBytes = file.length().coerceAtLeast(0L),
                    expectedModified = file.lastModified().coerceAtLeast(0L),
                )
            },
        )

    suspend fun prepareSnapshots(
        snapshots: List<CleanupFileSnapshot>,
    ): FileCleanupPreparation = withContext(Dispatchers.IO) {
        val media = ArrayList<Uri>()
        val safeTrash = ArrayList<CleanupFileSnapshot>()
        val blocked = ArrayList<File>()
        val stale = ArrayList<File>()

        for (snapshot in strongestCleanupSnapshots(snapshots)) {
            coroutineContext.ensureActive()

            val validation = CleanupSnapshotValidator.validate(snapshot)
            if (!validation.valid) {
                when (validation.rejection) {
                    CleanupSnapshotRejection.CHANGED_SINCE_SCAN,
                    CleanupSnapshotRejection.DIGEST_MISMATCH,
                    CleanupSnapshotRejection.DUPLICATE_KEEPER_INVALID,
                    -> stale += snapshot.file

                    else -> blocked += snapshot.file
                }
                continue
            }

            val file = snapshot.file
            if (MediaStoreHelper.isMediaFile(file)) {
                val uri = runCatching { MediaStoreHelper.getMediaUri(context, file) }.getOrNull()
                if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    media += uri
                } else {
                    blocked += file
                }
                continue
            }

            if (file.length() <= SafeTrashManager.MAX_SMALL_FILE_TRASH_BYTES) {
                safeTrash += snapshot
            } else {
                // No irreversible fallback: large non-media files stay untouched.
                blocked += file
            }
        }

        FileCleanupPreparation(
            mediaUris = media.distinct(),
            safeTrashCandidates = safeTrash,
            blockedFiles = blocked.distinctBy(::canonicalKey),
            staleFiles = stale.distinctBy(::canonicalKey),
        )
    }

    suspend fun movePreparedNonMediaToSafeTrash(
        files: List<File>,
    ): SafeTrashExecutionResult =
        movePreparedSnapshotsToSafeTrash(
            files.mapNotNull { file ->
                if (!file.exists() || !file.isFile) return@mapNotNull null
                CleanupFileSnapshot(
                    file = file,
                    expectedBytes = file.length().coerceAtLeast(0L),
                    expectedModified = file.lastModified().coerceAtLeast(0L),
                )
            },
        )

    suspend fun movePreparedSnapshotsToSafeTrash(
        snapshots: List<CleanupFileSnapshot>,
    ): SafeTrashExecutionResult = withContext(Dispatchers.IO) {
        var moved = 0
        var rejected = 0
        var movedBytes = 0L

        for (snapshot in strongestCleanupSnapshots(snapshots)) {
            coroutineContext.ensureActive()

            // Revalidate again immediately before mutation. This also protects
            // the delay while an Android system-trash confirmation is open.
            if (!CleanupSnapshotValidator.validate(snapshot).valid) {
                rejected++
                continue
            }

            if (safeTrashManager.moveToTrash(snapshot) != null) {
                moved++
                movedBytes += snapshot.expectedBytes.coerceAtLeast(0L)
            } else {
                rejected++
            }
        }

        SafeTrashExecutionResult(moved, rejected, movedBytes)
    }

    fun createSystemTrashRequest(mediaUris: List<Uri>): PendingIntent? =
        MediaStoreHelper.trashMediaFiles(context.contentResolver, mediaUris)

    fun selectDuplicatesExceptOldest(groups: List<DuplicateGroup>): Set<String> =
        selectDuplicatesExcept(groups, keepNewest = false)

    fun selectDuplicatesExceptNewest(groups: List<DuplicateGroup>): Set<String> =
        selectDuplicatesExcept(groups, keepNewest = true)

    private fun selectDuplicatesExcept(
        groups: List<DuplicateGroup>,
        keepNewest: Boolean,
    ): Set<String> = buildSet {
        for (group in groups) {
            val comparator = compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path }
            val keeper = if (keepNewest) {
                group.files.maxWithOrNull(comparator)
            } else {
                group.files.minWithOrNull(comparator)
            } ?: continue
            group.files
                .asSequence()
                .filterNot { it.path == keeper.path }
                .mapTo(this) { it.path }
        }
    }

    private fun canonicalKey(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
}
