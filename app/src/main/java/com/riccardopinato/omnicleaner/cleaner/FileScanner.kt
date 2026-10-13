package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.coroutineContext

class FileScanner(
    private val inventoryScanner: FileInventoryScanner = FileInventoryScanner(),
    private val thumbnailCleanup: ThumbnailFileCleanup = ThumbnailFileCleanup(),
    private val residueScanner: AppResidueScanner = AppResidueScanner(),
) {
    suspend fun collectFileInventory(
        root: File,
        forceRefresh: Boolean = false,
    ): FileInventory =
        StorageInventoryRepository.snapshot(root, forceRefresh = forceRefresh).inventory

    suspend fun findBigFiles(
        root: File,
        minimumBytes: Long = BigFileSizeFilter.SIZE_100MB.minimumBytes,
        limit: Int = 500,
    ): List<BigFileItem> =
        findBigFiles(
            inventory = StorageInventoryRepository.snapshot(root).inventory,
            minimumBytes = minimumBytes,
            limit = limit,
        )

    fun findBigFiles(
        inventory: FileInventory,
        minimumBytes: Long = BigFileSizeFilter.SIZE_100MB.minimumBytes,
        limit: Int = 500,
    ): List<BigFileItem> = inventory.files
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

    suspend fun findDuplicates(
        root: File,
        extensionFilter: String? = null,
        onInventoryProgress: ((checked: Int, currentPath: String) -> Unit)? = null,
        onDuplicateProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<DuplicateGroup> {
        val inventory = StorageInventoryRepository.snapshot(
            root = root,
            onProgress = onInventoryProgress,
        ).inventory
        return findDuplicates(
            inventory = inventory,
            extensionFilter = extensionFilter,
            onProgress = onDuplicateProgress,
        )
    }

    suspend fun findDuplicates(
        inventory: FileInventory,
        extensionFilter: String? = null,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<DuplicateGroup> {
        val normalizedExtension = extensionFilter
            ?.trim()
            ?.removePrefix(".")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }

        val candidates = if (normalizedExtension == null) {
            inventory.files
        } else {
            inventory.files.filter { item ->
                item.file.extension.lowercase(Locale.ROOT) == normalizedExtension
            }
        }

        return inventoryScanner.duplicatesProgressive(candidates, onProgress)
    }

    suspend fun scanFolders(
        root: File,
        smallFolderThresholdBytes: Long = 10L * 1024L * 1024L,
    ): List<FolderItem> = withContext(Dispatchers.Default) {
        val inventory = StorageInventoryRepository.snapshot(root).inventory
        val rootPath = canonicalPath(root).trimEnd(File.separatorChar)
        val rootPrefix = rootPath + File.separator

        val indexedByTopLevel = inventory.files.groupBy { item ->
            val path = canonicalPath(item.file)
            path.removePrefix(rootPrefix).substringBefore(File.separatorChar)
        }

        val children = runCatching { root.listFiles() }.getOrNull()
            ?.filter { it.isDirectory && !FileInventoryPolicy.shouldIgnore(root, it) }
            .orEmpty()

        children.mapNotNull { directory ->
            coroutineContext.ensureActive()
            val key = canonicalPath(directory)
                .removePrefix(rootPrefix)
                .substringBefore(File.separatorChar)
            val files = indexedByTopLevel[key].orEmpty()

            // If traversal reported unreadable directories, do not label an
            // otherwise empty top-level folder as completely empty from partial
            // evidence. Deletion has its own final emptiness/path validation.
            if (files.isEmpty() && inventory.unreadable > 0) {
                return@mapNotNull null
            }

            val total = files.sumOf { it.size }
            val status = when {
                files.isEmpty() -> FolderStatus.COMPLETELY_EMPTY
                total <= smallFolderThresholdBytes -> FolderStatus.BELOW_THRESHOLD
                else -> return@mapNotNull null
            }

            val directChildren = runCatching { directory.listFiles() }.getOrNull()
            val subfolders = directChildren?.count { it.isDirectory } ?: 0
            val path = directory.absolutePath

            FolderItem(
                file = directory,
                name = directory.name.ifBlank { path },
                path = path,
                displayPath = path,
                sizeBytes = total,
                status = status,
                fileCount = files.size,
                subfolderCount = subfolders,
            )
        }.sortedWith(
            compareByDescending<FolderItem> { it.status == FolderStatus.COMPLETELY_EMPTY }
                .thenBy { it.sizeBytes },
        )
    }

    suspend fun scanAppResidues(context: Context, root: File): AppResidueScan =
        residueScanner.scan(context, root)

    suspend fun scanThumbnailCache(root: File): List<ThumbnailCacheItem> = withContext(Dispatchers.IO) {
        val matches = ArrayList<ThumbnailCacheItem>()

        suspend fun inspect(directory: File) {
            if (!directory.exists() || !isSafeThumbnailDirectory(directory)) return
            val inventory = thumbnailCleanup.inventory(directory)
            val path = directory.absolutePath
            matches += ThumbnailCacheItem(
                file = directory,
                name = directory.name.ifBlank { ".thumbnails" },
                path = path,
                displayPath = path,
                sizeBytes = inventory.bytes,
                fileCount = inventory.files.size,
                warning = "",
                inventory = inventory,
            )
        }

        inspect(File(root, "DCIM/.thumbnails"))
        inspect(File(root, "Pictures/.thumbnails"))

        val androidMedia = File(root, "Android/media")
        val appFolders = runCatching { androidMedia.listFiles() }.getOrNull().orEmpty()
        for (appFolder in appFolders) {
            coroutineContext.ensureActive()
            if (!appFolder.isDirectory) continue
            inspect(File(appFolder, ".thumbnails"))
        }

        matches.sortedByDescending { it.sizeBytes }
    }

    fun isSafeThumbnailDirectory(directory: File): Boolean {
        val normalized = directory.absolutePath.replace('\\', '/').lowercase(Locale.ROOT)
        return normalized.endsWith("/dcim/.thumbnails") ||
            normalized.endsWith("/pictures/.thumbnails") ||
            (normalized.contains("/android/media/") && normalized.endsWith("/.thumbnails"))
    }

    private fun canonicalPath(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }

    private companion object {
        val IMAGE_OR_VIDEO_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg",
            "mp4", "mkv", "mov", "webm", "3gp", "avi",
        )
    }
}
