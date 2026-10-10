package com.riccardopinato.omnicleaner.cleaner

import java.io.File

data class DuplicateProtection(
    val keepers: Set<File>,
    val guardians: Map<String, File>,
)

object CleanupSelection {
    fun uniqueEntries(files: List<File>): List<File> {
        val seen = LinkedHashSet<String>()
        val result = ArrayList<File>()
        for (file in files) {
            val key = normalizedPath(file)
            if (seen.add(key)) result += file
        }
        return result
    }

    fun containsKeeper(directory: File, keepers: Set<File>): Boolean {
        val directoryPath = normalizedPath(directory)
        val prefix = "$directoryPath/"
        return keepers.any { keeper ->
            val keeperPath = normalizedPath(keeper)
            keeperPath == directoryPath || keeperPath.startsWith(prefix)
        }
    }

    fun protectDuplicates(groups: List<DuplicateGroup>): DuplicateProtection {
        val keepers = LinkedHashSet<File>()
        val guardians = LinkedHashMap<String, File>()

        for (group in groups) {
            if (group.files.size < 2) continue
            val keeper = group.files.minWithOrNull(
                compareBy<DuplicateFile> { it.lastModified }
                    .thenBy { normalizedPath(it.file) },
            ) ?: continue

            keepers += keeper.file

            for (duplicate in group.files) {
                if (normalizedPath(duplicate.file) == normalizedPath(keeper.file)) continue
                guardians[normalizedPath(duplicate.file)] = keeper.file
            }
        }

        return DuplicateProtection(
            keepers = keepers,
            guardians = guardians,
        )
    }

    private fun normalizedPath(file: File): String =
        runCatching { file.canonicalPath }
            .getOrElse { file.absolutePath }
            .replace('\\', '/')
            .trimEnd('/')
}
