package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SpaceOverviewScanner {
    suspend fun scan(
        root: File,
        largestLimit: Int = 25,
    ): SpaceOverview = withContext(Dispatchers.Default) {
        fromInventory(
            root = root,
            inventory = StorageInventoryRepository.snapshot(root).inventory,
            largestLimit = largestLimit,
        )
    }

    fun fromInventory(
        root: File,
        inventory: FileInventory,
        largestLimit: Int = 25,
    ): SpaceOverview {
        val rootPath = runCatching { root.canonicalPath }
            .getOrElse { root.absolutePath }
            .trimEnd('/') + "/"
        val categories = linkedMapOf<String, Long>()

        for (item in inventory.files) {
            val path = runCatching { item.file.canonicalPath }
                .getOrElse { item.file.absolutePath }
            val relative = path.removePrefix(rootPath)
            val category = relative.substringBefore('/').ifBlank { "Other" }
            categories[category] = (categories[category] ?: 0L) + item.size
        }

        return SpaceOverview(
            logicalBytes = inventory.files.sumOf { it.size },
            files = inventory.files.size,
            unreadable = inventory.unreadable,
            excludedDirectories = inventory.excludedDirectories,
            byCategory = categories.entries
                .map { SpaceEntry(it.key, it.value) }
                .sortedByDescending { it.bytes },
            largest = inventory.files.sortedByDescending { it.size }.take(largestLimit),
        )
    }
}
