package com.riccardopinato.omnicleaner.cleaner

import java.io.File

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercent: Float get() = if (totalBytes <= 0L) 0f else usedBytes.toFloat() / totalBytes.toFloat()
}

data class IndexedFile(
    val file: File,
    val size: Long,
    val modified: Long,
)

data class FileInventory(
    val files: List<IndexedFile>,
    val unreadable: Int,
    val excludedDirectories: Int,
)

enum class FolderStatus { EMPTY, SMALL, NORMAL, UNREADABLE }

data class FolderItem(
    val file: File,
    val sizeBytes: Long,
    val fileCount: Int,
    val subfolderCount: Int,
    val status: FolderStatus,
    val selected: Boolean = false,
) {
    val name: String get() = file.name.ifBlank { file.absolutePath }
    val path: String get() = file.absolutePath
    val displayPath: String get() = file.absolutePath
}

data class BigFileItem(
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long,
    val selected: Boolean = false,
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
}

data class DuplicateFile(
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long,
    val selected: Boolean = false,
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    val extension: String get() = file.extension.lowercase()
}

data class DuplicateGroup(
    val hash: String,
    val files: List<DuplicateFile>,
) {
    val sizeBytes: Long get() = files.firstOrNull()?.sizeBytes ?: 0L
    val totalDuplicatesCount: Int get() = (files.size - 1).coerceAtLeast(0)
    val totalWasteBytes: Long get() = sizeBytes * totalDuplicatesCount
    val selectedFilesCount: Int get() = files.count { it.selected }
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
