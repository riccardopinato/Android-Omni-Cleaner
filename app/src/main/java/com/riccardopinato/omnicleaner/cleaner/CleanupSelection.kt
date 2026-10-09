package com.riccardopinato.omnicleaner.cleaner

import com.riccardopinato.omnicleaner.model.DuplicateFile
import com.riccardopinato.omnicleaner.model.DuplicateGroup
import java.io.File

data class DuplicateProtection(
    val keepers: Set<String>,
    val guardians: Map<String, String>,
)

object CleanupSelection {
    fun uniqueEntries(files: List<File>): List<File> {
        val seen = HashSet<String>()
        return files.filter { file ->
            val key = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
            seen.add(key)
        }
    }

    fun protectDuplicates(groups: List<DuplicateGroup>): DuplicateProtection {
        val keepers = LinkedHashSet<String>()
        val guardians = LinkedHashMap<String, String>()

        for (group in groups) {
            if (group.files.isEmpty()) continue
            val selected = group.files.filter { it.isSelected }
            val candidates = if (selected.size >= group.files.size) {
                group.files.sortedWith(compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path })
            } else {
                group.files.filterNot { it.isSelected }
            }
            val keeper = candidates.firstOrNull() ?: group.files.first()
            keepers += keeper.path
            selected.filter { it.path != keeper.path }.forEach { guardians[it.path] = keeper.path }
        }
        return DuplicateProtection(keepers, guardians)
    }

    fun containsKeeper(file: File, keepers: Set<String>): Boolean {
        val path = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        return path in keepers
    }
}
