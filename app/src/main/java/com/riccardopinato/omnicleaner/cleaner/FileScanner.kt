package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.coroutineContext

class FileScanner(
    private val inventoryScanner: FileInventoryScanner = FileInventoryScanner(),
) {
    suspend fun collectFileInventory(root: File): FileInventory = inventoryScanner.collect(root)

    suspend fun findBigFiles(
        root: File,
        minimumBytes: Long = BigFileSizeFilter.SIZE_100MB.minimumBytes,
        limit: Int = 500,
    ): List<BigFileItem> = inventoryScanner.collect(root).files
        .asSequence()
        .filter { it.size >= minimumBytes }
        .sortedByDescending { it.size }
        .take(limit)
        .map { item ->
            val path = item.file.absolutePath
            val extension = item.file.extension.lowercase(Locale.ROOT)
            BigFileItem(
                file = item.file,
                name = item.file.name,
                path = path,
                displayPath = path,
                sizeBytes = item.size,
                lastModified = item.modified,
                extension = extension,
                isImageOrVideo = extension in IMAGE_OR_VIDEO_EXTENSIONS,
            )
        }
        .toList()

    suspend fun findDuplicates(root: File): List<DuplicateGroup> = inventoryScanner.duplicates(root)

    suspend fun scanFolders(
        root: File,
        smallFolderThresholdBytes: Long = 10L * 1024L * 1024L,
    ): List<FolderItem> = withContext(Dispatchers.IO) {
        val children = runCatching { root.listFiles() }.getOrNull()
            ?.filter { it.isDirectory && !FileInventoryPolicy.shouldIgnore(root, it) }
            .orEmpty()

        children.mapNotNull { directory ->
            coroutineContext.ensureActive()
            collectFolderCandidate(directory, smallFolderThresholdBytes)
        }.sortedWith(
            compareByDescending<FolderItem> { it.status == FolderStatus.COMPLETELY_EMPTY }
                .thenBy { it.sizeBytes },
        )
    }

    suspend fun scanThumbnailCache(root: File): List<ThumbnailCacheItem> = withContext(Dispatchers.IO) {
        val stack = ArrayDeque<File>()
        val matches = ArrayList<ThumbnailCacheItem>()
        val visited = HashSet<String>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = stack.removeLast()
            val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
            if (!visited.add(canonical)) continue

            val children = runCatching { current.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                if (!child.isDirectory) continue

                if (isSafeThumbnailDirectory(child)) {
                    val inventory = thumbnailInventory(child)
                    val path = child.absolutePath
                    matches += ThumbnailCacheItem(
                        file = child,
                        name = child.name.ifBlank { ".thumbnails" },
                        path = path,
                        displayPath = path,
                        sizeBytes = inventory.bytes,
                        fileCount = inventory.files.size,
                        warning = "",
                        inventory = inventory,
                    )
                    continue
                }

                val normalized = child.absolutePath.replace('\\', '/').lowercase(Locale.ROOT)
                val shouldDescend = child == root ||
                    child.parentFile == root ||
                    normalized.endsWith("/dcim") ||
                    normalized.endsWith("/pictures") ||
                    normalized.endsWith("/android") ||
                    normalized.endsWith("/android/media") ||
                    normalized.contains("/android/media/")

                if (shouldDescend && !FileInventoryPolicy.shouldIgnore(root, child)) {
                    stack.add(child)
                }
            }
        }

        matches.sortedByDescending { it.sizeBytes }
    }

    fun isSafeThumbnailDirectory(directory: File): Boolean {
        val normalized = directory.absolutePath.replace('\\', '/').lowercase(Locale.ROOT)
        return normalized.endsWith("/dcim/.thumbnails") ||
            normalized.endsWith("/pictures/.thumbnails") ||
            (normalized.contains("/android/media/") && normalized.endsWith("/.thumbnails"))
    }

    private suspend fun collectFolderCandidate(
        folder: File,
        thresholdBytes: Long,
    ): FolderItem? {
        if (!folder.exists() || !folder.isDirectory) return null

        val inventory = inventoryScanner.collect(folder)
        if (inventory.unreadable > 0 && inventory.files.isEmpty()) return null

        val subfolders = runCatching { folder.listFiles()?.count { it.isDirectory } }
            .getOrNull() ?: 0
        val total = inventory.files.sumOf { it.size }

        val status = when {
            inventory.files.isEmpty() -> FolderStatus.COMPLETELY_EMPTY
            total <= thresholdBytes -> FolderStatus.BELOW_THRESHOLD
            else -> return null
        }

        val path = folder.absolutePath
        return FolderItem(
            file = folder,
            name = folder.name.ifBlank { path },
            path = path,
            displayPath = path,
            sizeBytes = total,
            status = status,
            fileCount = inventory.files.size,
            subfolderCount = subfolders,
        )
    }

    private fun thumbnailInventory(directory: File): ThumbnailSnapshot {
        val files = ArrayList<File>()
        var subfolders = 0
        val stack = ArrayDeque<File>()
        val visited = HashSet<String>()
        stack.add(directory)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
            if (!visited.add(canonical)) continue

            val children = runCatching { current.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                when {
                    child.isDirectory && !PathSafety.isSymlink(child) -> {
                        subfolders++
                        stack.add(child)
                    }
                    child.isFile -> files += child
                }
            }
        }

        return ThumbnailSnapshot(
            directory = directory,
            files = files,
            subfolders = subfolders,
        )
    }

    private companion object {
        val IMAGE_OR_VIDEO_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg",
            "mp4", "mkv", "mov", "webm", "3gp", "avi",
        )
    }
}
