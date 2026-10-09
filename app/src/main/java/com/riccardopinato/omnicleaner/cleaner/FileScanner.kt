package com.riccardopinato.omnicleaner.cleaner

import com.riccardopinato.omnicleaner.model.AppResidueItem
import com.riccardopinato.omnicleaner.model.BigFileItem
import com.riccardopinato.omnicleaner.model.DuplicateGroup
import com.riccardopinato.omnicleaner.model.FolderItem
import com.riccardopinato.omnicleaner.model.FolderStatus
import com.riccardopinato.omnicleaner.model.ThumbnailCacheItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ScannedFolderResult(
    val file: File,
    val totalSizeBytes: Long,
    val fileCount: Int,
    val subfolderCount: Int,
    val isCompletelyEmpty: Boolean,
    val thumbnailSnapshot: ThumbnailSnapshot? = null,
)

object FileScanner {
    private val thumbnailFolderNames = setOf(".thumbnails", "thumbnails", ".thumbnail", "thumbs")
    private val ignoredDirectories = setOf("Android/obb", "Android/data")

    suspend fun collectFileInventory(
        root: File,
        minBytes: Long = 0L,
        onProgress: (Int) -> Unit = {},
    ): List<IndexedFile> = FileInventoryScanner.collect(root, minBytes, onProgress)

    suspend fun computeFolderStats(
        root: File,
        onProgress: (Int, File) -> Unit = { _, _ -> },
    ): ScannedFolderResult = withContext(Dispatchers.IO) {
        var total = 0L
        var fileCount = 0
        var subfolderCount = 0
        var visited = 0
        val queue = ArrayDeque<File>()
        val seen = HashSet<String>()
        queue += root

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = queue.removeLast()
            if (current.isDirectory) {
                val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
                if (!seen.add(canonical)) continue
                if (current != root) subfolderCount++
                current.listFiles()?.forEach(queue::add)
            } else if (current.isFile) {
                fileCount++
                total += current.length()
            }
            visited++
            if (visited % 64 == 0) onProgress(visited, current)
        }

        onProgress(visited, root)
        ScannedFolderResult(
            file = root,
            totalSizeBytes = total,
            fileCount = fileCount,
            subfolderCount = subfolderCount,
            isCompletelyEmpty = fileCount == 0,
        )
    }

    suspend fun scanFolders(
        root: File,
        thresholdBytes: Long,
        onProgress: (Int, File) -> Unit = { _, _ -> },
    ): List<FolderItem> = withContext(Dispatchers.IO) {
        if (!root.isDirectory) return@withContext emptyList()

        val directories = ArrayList<File>()
        val queue = ArrayDeque<File>()
        val seen = HashSet<String>()
        queue += root

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = queue.removeLast()
            if (!current.isDirectory) continue
            val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
            if (!seen.add(canonical)) continue

            if (current != root && PathSafety.isSafeFolderToDelete(current)) directories += current
            current.listFiles()
                ?.asSequence()
                ?.filter { it.isDirectory && !shouldIgnore(it) }
                ?.forEach(queue::add)
        }

        val candidates = ArrayList<FolderItem>()
        directories.forEachIndexed { index, directory ->
            coroutineContext.ensureActive()
            onProgress(index + 1, directory)
            val stats = computeFolderStats(directory)
            if (stats.isCompletelyEmpty || stats.totalSizeBytes <= thresholdBytes) {
                val status = if (stats.isCompletelyEmpty) {
                    FolderStatus.COMPLETELY_EMPTY
                } else {
                    FolderStatus.BELOW_THRESHOLD
                }
                candidates += FolderItem(
                    file = directory,
                    sizeBytes = stats.totalSizeBytes,
                    status = status,
                    fileCount = stats.fileCount,
                    subfolderCount = stats.subfolderCount,
                )
            }
        }

        // If a parent folder is itself a candidate, do not also present descendants.
        candidates
            .sortedBy { it.path.count { ch -> ch == File.separatorChar } }
            .fold(ArrayList<FolderItem>()) { selected, item ->
                val nested = selected.any { parent ->
                    item.path.startsWith(parent.path.trimEnd(File.separatorChar) + File.separator)
                }
                if (!nested) selected += item
                selected
            }
            .sortedWith(compareBy<FolderItem> { it.status != FolderStatus.COMPLETELY_EMPTY }.thenBy { it.sizeBytes })
    }

    suspend fun findBigFiles(
        root: File,
        minBytes: Long,
        inventory: List<IndexedFile>? = null,
        onProgress: (Int) -> Unit = {},
    ): List<BigFileItem> {
        val source = inventory ?: collectFileInventory(root, minBytes, onProgress)
        return source
            .asSequence()
            .filter { it.size >= minBytes && it.file.exists() }
            .map {
                BigFileItem(
                    file = it.file,
                    sizeBytes = it.size,
                    lastModified = it.modified,
                )
            }
            .sortedByDescending { it.sizeBytes }
            .toList()
    }

    suspend fun findDuplicates(
        root: File,
        minBytes: Long,
        inventory: List<IndexedFile>? = null,
        onProgress: (Int) -> Unit = {},
    ): List<DuplicateGroup> {
        val source = inventory ?: collectFileInventory(root, minBytes)
        return FileInventoryScanner.duplicates(source, minBytes, onProgress)
    }

    suspend fun isFolderWithinThreshold(folder: File, thresholdBytes: Long): Boolean {
        if (!folder.isDirectory) return false
        return computeFolderStats(folder).totalSizeBytes <= thresholdBytes
    }

    fun isSafeThumbnailDirectory(root: File, candidate: File): Boolean {
        if (!candidate.isDirectory) return false
        val name = candidate.name.lowercase()
        if (name !in thumbnailFolderNames) return false
        return runCatching {
            val rootPath = root.canonicalPath.trimEnd(File.separatorChar) + File.separator
            candidate.canonicalPath.startsWith(rootPath)
        }.getOrDefault(false)
    }

    suspend fun scanThumbnailCache(
        root: File,
        onProgress: (Int, File) -> Unit = { _, _ -> },
    ): List<ThumbnailCacheItem> = withContext(Dispatchers.IO) {
        val candidates = linkedSetOf<File>()
        listOf(
            File(root, "DCIM/.thumbnails"),
            File(root, "Pictures/.thumbnails"),
        ).filterTo(candidates) { it.isDirectory }

        val androidMedia = File(root, "Android/media")
        androidMedia.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.flatMap { appDir ->
                sequenceOf(
                    File(appDir, ".thumbnails"),
                    File(appDir, "thumbnails"),
                )
            }
            ?.filter { it.isDirectory }
            ?.forEach(candidates::add)

        // Recover additional explicit thumbnail directories without scanning protected roots.
        root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && !shouldIgnore(it) }
            ?.forEach { top ->
                top.listFiles()
                    ?.filter { isSafeThumbnailDirectory(root, it) }
                    ?.forEach(candidates::add)
            }

        val result = ArrayList<ThumbnailCacheItem>()
        candidates.forEachIndexed { index, directory ->
            coroutineContext.ensureActive()
            onProgress(index + 1, directory)
            val snapshot = ThumbnailFileCleanup.inventory(directory)
            if (snapshot.files.isNotEmpty()) {
                result += ThumbnailCacheItem(
                    file = directory,
                    sizeBytes = snapshot.bytes,
                    fileCount = snapshot.files.size,
                    warning = "Miniature ricreabili dalle app: verifica prima di eliminare.",
                    inventory = snapshot,
                )
            }
        }
        result.sortedByDescending { it.sizeBytes }
    }

    suspend fun scanAppResidues(
        root: File,
        installedPackages: Set<String>,
        onProgress: (Int) -> Unit = {},
        onVisited: (Int, File) -> Unit = { _, _ -> },
    ): List<AppResidueItem> = withContext(Dispatchers.IO) {
        val bases = listOf(
            File(root, "Android/data"),
            File(root, "Android/media"),
            File(root, "Android/obb"),
        ).filter { it.isDirectory && it.canRead() }

        val candidates = bases.flatMap { base ->
            base.listFiles()
                ?.filter { child ->
                    child.isDirectory &&
                        child.name.contains('.') &&
                        child.name !in installedPackages
                }
                .orEmpty()
        }.distinctBy { it.absolutePath }

        val result = ArrayList<AppResidueItem>()
        candidates.forEachIndexed { index, directory ->
            coroutineContext.ensureActive()
            onProgress(index + 1)
            onVisited(index + 1, directory)
            val stats = computeFolderStats(directory)
            result += AppResidueItem(
                file = directory,
                sizeBytes = stats.totalSizeBytes,
                fileCount = stats.fileCount,
            )
        }
        result.sortedByDescending { it.sizeBytes }
    }

    private fun shouldIgnore(file: File): Boolean {
        val normalized = file.absolutePath.replace('\\', '/')
        if (ignoredDirectories.any { normalized.endsWith(it) }) return true
        return file.name in PathSafety.knownDevArtifactNames
    }
}
