package com.riccardopinato.omnicleaner.cleaner

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
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
    val mediaBindings: List<Pair<Uri, CleanupFileSnapshot>> = emptyList(),
    var mediaTransactionId: String? = null,
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
    private val journal by lazy {
        SafeOperationsJournal(File(context.filesDir, "omni_safe_operations"))
    }
    private val coordinator by lazy { DeletionCoordinator(safeTrashManager, journal) }

    /** Read-only crash recovery. The caller must never replay a pending move. */
    fun incompleteOperations(): List<SafeOperationRecord> = journal.needsReview()

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

        if (removed > 0) {
            StorageInventoryRepository.invalidate()
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
        val mediaBindings = ArrayList<Pair<Uri, CleanupFileSnapshot>>()
        val seenMedia = HashSet<Uri>()
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
                val digestBound = CleanupSnapshotValidator.withCurrentDigest(snapshot)
                if (digestBound == null) {
                    stale += file
                    continue
                }
                val uri = runCatching { MediaStoreHelper.getMediaUri(context, file) }.getOrNull()
                if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (uri in seenMedia) continue
                    if (seenMedia.size == MediaTrashBatchPolicy.MAX_URIS_PER_REQUEST) {
                        // Do not silently submit >2,000 Android URIs; the
                        // remainder is reported as blocked and stays untouched.
                        blocked += file
                    } else {
                        seenMedia += uri
                        media += uri
                        mediaBindings += uri to digestBound
                    }
                } else {
                    blocked += file
                }
                continue
            }

            if (file.length() <= SafeTrashManager.MAX_REVERSIBLE_TRASH_BYTES) {
                // A supplied SHA was already revalidated above (including
                // the keeper for duplicates). Do not read both 50-MiB APKs
                // a further time just to re-bind that same digest.
                val digestBound = if (snapshot.expectedSha256 != null) {
                    snapshot
                } else {
                    CleanupSnapshotValidator.withCurrentDigest(snapshot)
                }
                if (digestBound != null) {
                    safeTrash += digestBound
                } else {
                    stale += file
                }
            } else {
                // No irreversible fallback: over-limit non-media files stay untouched.
                blocked += file
            }
        }

        FileCleanupPreparation(
            mediaUris = media.distinct(),
            safeTrashCandidates = safeTrash,
            blockedFiles = blocked.distinctBy(::canonicalKey),
            staleFiles = stale.distinctBy(::canonicalKey),
            mediaBindings = mediaBindings,
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
        coroutineContext.ensureActive()
        val result = coordinator.moveApproved(snapshots)
        if (result.moved > 0) StorageInventoryRepository.invalidate()
        result
    }

    /**
     * Media snapshots are revalidated immediately before the system intent is
     * created. An inaccessible or changed source blocks the entire batch.
     */
    fun createSystemTrashRequest(preparation: FileCleanupPreparation): PendingIntent? {
        val bindings = preparation.mediaBindings
        if (bindings.size != preparation.mediaUris.size ||
            bindings.map { it.first } != preparation.mediaUris ||
            bindings.any { (uri, snapshot) ->
                !CleanupSnapshotValidator.validate(snapshot).valid ||
                    runCatching { MediaStoreHelper.getMediaUri(context, snapshot.file) }.getOrNull() != uri ||
                    !MediaStoreHelper.isCurrentMediaMatch(
                        context.contentResolver,
                        uri,
                        snapshot.expectedBytes,
                        snapshot.file.name,
                    )
            }
        ) return null

        // Write intent before asking the OS to mutate MediaStore. On crash the
        // persisted URI set can be re-read, never replayed as a Trash request.
        val detail = preparation.mediaBindings.joinToString("\n") { (uri, item) ->
            listOf(uri.toString(), item.expectedBytes.toString(),
                item.file.name, item.expectedSha256.orEmpty(),
            ).joinToString("|")
        }
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(detail.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val prepared = journal.begin("MEDIA_TRASH", fingerprint, detail) ?: return null
        val approved = journal.advance(prepared, SafeOperationPhase.USER_APPROVED) ?: return null
        val request = MediaStoreHelper.trashMediaFiles(
            context.contentResolver, preparation.mediaUris,
        )
        if (request == null) {
            journal.advance(approved, SafeOperationPhase.REJECTED)
            return null
        }
        preparation.mediaTransactionId = approved.id
        return request
    }

    /** Callback is only evidence. Actual MediaStore state determines success. */
    suspend fun recordMediaTrashOutcome(
        preparation: FileCleanupPreparation,
        verification: MediaTrashVerification?,
    ) = withContext(Dispatchers.IO) {
        val record = preparation.mediaTransactionId?.let(journal::latest)
            ?: return@withContext
        if (record.type != "MEDIA_TRASH") return@withContext
        val success = verification != null &&
            preparation.mediaUris.toSet() == verification.confirmedUris
        journal.advance(
            record,
            if (success) SafeOperationPhase.COMMITTED
            else SafeOperationPhase.UNKNOWN_NEEDS_REVIEW,
        )
    }

    /** Read-only crash recovery; never relaunch Android's destructive prompt. */
    suspend fun reconcilePendingMedia(): Int = withContext(Dispatchers.IO) {
        var unresolved = 0
        journal.needsReview().filter { it.type == "MEDIA_TRASH" }.forEach { record ->
            val raw = record.detail.lineSequence().filter { it.isNotBlank() }.toList()
            val uris = raw.mapNotNull { line ->
                line.substringBefore('|').takeIf { it.startsWith("content://") }
                    ?.let(Uri::parse)
            }
            if (uris.isEmpty() || uris.size != raw.size) {
                unresolved++
                return@forEach
            }
            val confirmed = MediaStoreHelper.verifyTrashedUris(
                context.contentResolver, uris,
            )
            if (confirmed.unverifiedUris.isEmpty()) {
                journal.advance(record, SafeOperationPhase.COMMITTED)
            } else unresolved++
        }
        unresolved
    }

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
