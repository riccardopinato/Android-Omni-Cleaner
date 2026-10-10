package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.util.Locale

object FileInventoryPolicy {
    val ignoredNames: Set<String> = setOf(
        "cache",
        "lost+found",
        "lost.dir",
    )

    fun shouldIgnore(root: File, directory: File): Boolean {
        if (directory == root) return false

        val rootPath = normalizedPath(root)
        val directoryPath = normalizedPath(directory)
        if (directoryPath == rootPath || !directoryPath.startsWith("$rootPath/")) return true

        val relative = directoryPath.removePrefix("$rootPath/").trim('/')
        if (relative.isEmpty() || relative == ".." || relative.startsWith("../")) return true

        val name = directory.name.lowercase(Locale.ROOT)
        if (name.startsWith(".")) return true
        if (name in ignoredNames) return true

        val segments = relative.split('/')
        if (segments.firstOrNull()?.equals("android", ignoreCase = true) == true) {
            val second = segments.getOrNull(1)
            if (second.equals("data", ignoreCase = true) || second.equals("obb", ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    private fun normalizedPath(file: File): String =
        runCatching { file.canonicalPath }
            .getOrElse { file.absolutePath }
            .replace('\\', '/')
            .trimEnd('/')
}
