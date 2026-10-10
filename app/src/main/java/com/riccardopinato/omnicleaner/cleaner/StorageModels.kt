package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.util.Locale

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercent: Float get() = if (totalBytes <= 0L) 0f else usedBytes.toFloat() / totalBytes.toFloat()
}

data class IndexedFile(val file: File, val size: Long, val modified: Long)

data class FileInventory(
    val files: List<IndexedFile>,
    val unreadable: Int,
    val excludedDirectories: Int,
)

enum class FolderStatus { COMPLETELY_EMPTY, BELOW_THRESHOLD }
enum class FolderSubTab { EMPTY_FOLDERS, APP_RESIDUES, THUMBNAIL_CACHE }

enum class ThresholdUnit(val label: String, val multiplier: Long) {
    BYTES("Bytes", 1L),
    KB("KB", 1024L),
    MB("MB", 1024L * 1024L),
}

data class FolderItem(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val status: FolderStatus,
    val isSelected: Boolean = false,
    val fileCount: Int,
    val subfolderCount: Int,
) {
    fun formattedSize(): String = formatModelBytes(sizeBytes)
}

data class AppResidueItem(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val isSelected: Boolean = false,
) {
    fun formattedSize(): String = formatModelBytes(sizeBytes)
}

data class ThumbnailFileStamp(
    val file: File,
    val bytes: Long,
    val modified: Long,
    val sha256: String,
)

data class ThumbnailSnapshot(
    val directory: File,
    val files: List<ThumbnailFileStamp>,
    val subfolders: Int,
) {
    val bytes: Long get() = files.sumOf { it.bytes }
}

data class ThumbnailCleanupResult(
    val removed: Int,
    val rejected: Int,
    val removedBytes: Long = 0L,
)

data class ThumbnailCacheItem(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val isSelected: Boolean = false,
    val warning: String,
    val inventory: ThumbnailSnapshot,
) {
    fun formattedSize(): String = formatModelBytes(sizeBytes)
}

data class BigFileItem(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val isImageOrVideo: Boolean,
    val isSelected: Boolean = false,
)

data class DuplicateFile(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val isSelected: Boolean = false,
) {
    fun formattedSize(): String = formatModelBytes(sizeBytes)
}

data class DuplicateGroup(
    val hash: String,
    val sizeBytes: Long,
    val files: List<DuplicateFile>,
) {
    val totalDuplicatesCount: Int get() = (files.size - 1).coerceAtLeast(0)
    val totalWasteBytes: Long get() = sizeBytes * totalDuplicatesCount
    val selectedFilesCount: Int get() = files.count { it.isSelected }
}

data class SpaceEntry(val path: String, val bytes: Long)

data class SpaceOverview(
    val logicalBytes: Long,
    val files: Int,
    val unreadable: Int,
    val excludedDirectories: Int,
    val byCategory: List<SpaceEntry>,
    val largest: List<IndexedFile>,
)

private fun formatModelBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
