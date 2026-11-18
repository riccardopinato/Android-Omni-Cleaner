package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.util.Locale

/**
 * Read-only view over the existing shared inventory.
 * No new filesystem traversal, permission or deletion authority is introduced.
 */
internal data class StorageFolderShare(
    val name: String,
    val fileCount: Int,
    val bytes: Long,
)

internal data class StorageReviewSuggestion(
    val category: SmartCategory,
    val fileCount: Int,
    val bytes: Long,
)

internal data class StorageFileSearchHit(
    val name: String,
    val relativePath: String,
    val extension: String,
    val bytes: Long,
)

internal data class StorageFileSearchPage(
    val matchCount: Int,
    val files: List<StorageFileSearchHit>,
)

internal class StorageIntelligenceReport(
    val indexedBytes: Long,
    val indexedFileCount: Int,
    val unreadable: Int,
    val excludedDirectories: Int,
    val folders: List<StorageFolderShare>,
    val suggestions: List<StorageReviewSuggestion>,
    private val indexed: List<IndexedFile>,
    private val rootPrefix: String,
) {
    /** Search is read-only, bounded in the UI, and never yields deletion commands. */
    fun search(
        text: String = "",
        folder: String? = null,
        minimumBytes: Long = 0L,
        limit: Int = 20,
    ): StorageFileSearchPage {
        val needle = text.trim().lowercase(Locale.ROOT)
        val minSize = minimumBytes.coerceAtLeast(0L)
        val maxVisible = limit.coerceIn(0, 200)
        val matches = ArrayList<StorageFileSearchHit>(maxVisible)
        var total = 0

        // indexed is sorted descending once when the report is constructed.
        for (entry in indexed) {
            val path = entry.file.absolutePath.replace('\\', '/')
            if (!path.startsWith(rootPrefix)) continue
            val relative = path.removePrefix(rootPrefix)
            val topLevel = if ('/' in relative) relative.substringBefore('/') else ""
            if (folder != null && topLevel != folder) continue
            val size = entry.size.coerceAtLeast(0L)
            if (size < minSize) continue
            if (needle.isNotEmpty() &&
                !relative.lowercase(Locale.ROOT).contains(needle)
            ) continue

            total++
            if (matches.size < maxVisible) {
                matches += StorageFileSearchHit(
                    name = entry.file.name,
                    relativePath = relative,
                    extension = entry.file.extension.lowercase(Locale.ROOT),
                    bytes = size,
                )
            }
        }

        return StorageFileSearchPage(matchCount = total, files = matches)
    }
}

internal class StorageIntelligenceAnalyzer {
    fun fromInventory(
        root: File,
        inventory: FileInventory,
        nowMillis: Long = System.currentTimeMillis(),
    ): StorageIntelligenceReport {
        val rootPath = root.canonicalPath.replace('\\', '/').trimEnd('/')
        val rootPrefix = "$rootPath/"
        val folderBytes = linkedMapOf<String, Long>()
        val folderCounts = linkedMapOf<String, Int>()
        val indexed = ArrayList<IndexedFile>(inventory.files.size)
        var totalBytes = 0L

        for (entry in inventory.files) {
            val absolute = entry.file.absolutePath.replace('\\', '/')
            if (!absolute.startsWith(rootPrefix)) continue
            val relative = absolute.removePrefix(rootPrefix)
            if (relative.isBlank()) continue
            val folder = if ('/' in relative) relative.substringBefore('/') else ""
            val size = entry.size.coerceAtLeast(0L)
            indexed += entry
            totalBytes += size
            folderBytes[folder] = (folderBytes[folder] ?: 0L) + size
            folderCounts[folder] = (folderCounts[folder] ?: 0) + 1
        }

        val shares = folderBytes.map { (name, bytes) ->
            StorageFolderShare(name, folderCounts[name] ?: 0, bytes)
        }.sortedWith(
            compareByDescending<StorageFolderShare> { it.bytes }.thenBy { it.name },
        )

        // Reuse the existing conservative Smart Categories policy. Categories
        // overlap: their sizes must NOT be summed as hypothetical savings.
        val suggestionsScan = SmartCategoryScanner().classify(
            root = root,
            files = indexed,
            nowMillis = nowMillis,
            unreadable = inventory.unreadable,
            excludedDirectories = inventory.excludedDirectories,
        )
        val suggestions = SmartCategory.entries.mapNotNull { category ->
            val candidates = suggestionsScan.itemsFor(category)
            if (candidates.isEmpty()) null else StorageReviewSuggestion(
                category = category,
                fileCount = candidates.size,
                bytes = candidates.sumOf { it.sizeBytes },
            )
        }
        indexed.sortWith(
            compareByDescending<IndexedFile> { it.size }.thenBy { it.file.absolutePath },
        )

        return StorageIntelligenceReport(
            indexedBytes = totalBytes,
            indexedFileCount = indexed.size,
            unreadable = inventory.unreadable,
            excludedDirectories = inventory.excludedDirectories,
            folders = shares,
            suggestions = suggestions,
            indexed = indexed,
            rootPrefix = rootPrefix,
        )
    }
}
