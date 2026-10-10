package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class FileScanner(
    private val inventoryScanner: FileInventoryScanner = FileInventoryScanner(),
) {
    suspend fun collectFileInventory(root: File): FileInventory = inventoryScanner.collect(root)

    suspend fun findBigFiles(
        root: File,
        minimumBytes: Long = 100L * 1024L * 1024L,
        limit: Int = 500,
    ): List<BigFileItem> = inventoryScanner.collect(root).files
        .asSequence()
        .filter { it.size >= minimumBytes }
        .sortedByDescending { it.size }
        .take(limit)
        .map { BigFileItem(it.file, it.size, it.modified) }
        .toList()

    suspend fun findDuplicates(root: File): List<DuplicateGroup> = inventoryScanner.duplicates(root)

    suspend fun scanFolders(
        root: File,
        smallFolderThresholdBytes: Long = 10L * 1024L * 1024L,
    ): List<FolderItem> = withContext(Dispatchers.IO) {
        val children = runCatching { root.listFiles() }.getOrNull()
            ?.filter { it.isDirectory }
            .orEmpty()

        children.map { directory ->
            coroutineContext.ensureActive()
            computeFolderStats(directory, smallFolderThresholdBytes)
        }.sortedWith(
            compareByDescending<FolderItem> { it.status == FolderStatus.EMPTY }
                .thenBy { it.sizeBytes },
        )
    }

    suspend fun computeFolderStats(
        folder: File,
        smallFolderThresholdBytes: Long = 10L * 1024L * 1024L,
    ): FolderItem = withContext(Dispatchers.IO) {
        if (!folder.exists() || !folder.isDirectory) {
            return@withContext FolderItem(folder, 0L, 0, 0, FolderStatus.UNREADABLE)
        }

        val inventory = inventoryScanner.collect(folder)
        val directSubfolders = runCatching { folder.listFiles()?.count { it.isDirectory } }.getOrNull() ?: 0
        val total = inventory.files.sumOf { it.size }
        val status = when {
            inventory.unreadable > 0 && inventory.files.isEmpty() -> FolderStatus.UNREADABLE
            inventory.files.isEmpty() && directSubfolders == 0 -> FolderStatus.EMPTY
            total <= smallFolderThresholdBytes -> FolderStatus.SMALL
            else -> FolderStatus.NORMAL
        }
        FolderItem(folder, total, inventory.files.size, directSubfolders, status)
    }

    suspend fun scanThumbnailCache(root: File): List<FolderItem> = withContext(Dispatchers.IO) {
        val expected = setOf(".thumbnails", "thumbnails", "cache")
        val stack = ArrayDeque<File>()
        val matches = ArrayList<FolderItem>()
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
                if (child.name.lowercase() in expected || isSafeThumbnailDirectory(child)) {
                    matches += computeFolderStats(child)
                } else if (current == root || current.parentFile == root) {
                    stack.add(child)
                }
            }
        }
        matches.sortedByDescending { it.sizeBytes }
    }

    fun isSafeThumbnailDirectory(directory: File): Boolean {
        val normalized = directory.absolutePath.replace('\', '/').lowercase()
        return normalized.endsWith("/dcim/.thumbnails") ||
            normalized.endsWith("/pictures/.thumbnails") ||
            (normalized.contains("/android/media/") && normalized.endsWith("/.thumbnails"))
    }
}
