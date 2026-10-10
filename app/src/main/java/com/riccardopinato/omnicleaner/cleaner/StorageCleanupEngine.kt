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
    val safeTrashFiles: List<File>,
    val blockedFiles: List<File>,
)

data class SafeTrashExecutionResult(
    val moved: Int,
    val rejected: Int,
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

    suspend fun prepareFiles(
        files: List<File>,
    ): FileCleanupPreparation = withContext(Dispatchers.IO) {
        val media = ArrayList<Uri>()
        val safeTrash = ArrayList<File>()
        val blocked = ArrayList<File>()

        for (file in CleanupSelection.uniqueEntries(files)) {
            coroutineContext.ensureActive()

            if (!file.exists() || !file.isFile || PathSafety.isDangerousPath(file) || PathSafety.isSymlink(file)) {
                blocked += file
                continue
            }

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
                safeTrash += file
            } else {
                // Large non-media files stay blocked until a reversible external-storage
                // trash lane exists. Never downgrade to permanent File.delete().
                blocked += file
            }
        }

        FileCleanupPreparation(
            mediaUris = media.distinct(),
            safeTrashFiles = safeTrash,
            blockedFiles = blocked,
        )
    }

    suspend fun movePreparedNonMediaToSafeTrash(
        files: List<File>,
    ): SafeTrashExecutionResult = withContext(Dispatchers.IO) {
        var moved = 0
        var rejected = 0

        for (file in CleanupSelection.uniqueEntries(files)) {
            coroutineContext.ensureActive()
            if (safeTrashManager.moveToTrash(file) != null) moved++ else rejected++
        }

        SafeTrashExecutionResult(moved, rejected)
    }

    fun createSystemTrashRequest(mediaUris: List<Uri>): PendingIntent? =
        MediaStoreHelper.trashMediaFiles(context.contentResolver, mediaUris)

    fun protectedDuplicateFiles(
        groups: List<DuplicateGroup>,
        selectedPaths: Set<String>,
    ): List<File> {
        val selected = ArrayList<File>()

        for (group in groups) {
            val selectedInGroup = group.files.filter { it.path in selectedPaths }
            if (selectedInGroup.isEmpty()) continue

            val allSelected = selectedInGroup.size == group.files.size
            val keeper = if (allSelected) {
                group.files.minWithOrNull(
                    compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path },
                )
            } else {
                null
            }

            selected += selectedInGroup
                .filterNot { keeper != null && it.path == keeper.path }
                .map { it.file }
        }

        return CleanupSelection.uniqueEntries(selected)
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
}
