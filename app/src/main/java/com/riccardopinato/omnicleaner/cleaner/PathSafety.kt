package com.riccardopinato.omnicleaner.cleaner

import java.io.File

object PathSafety {
    val knownDevArtifactNames: Set<String> = setOf(
        ".git",
        ".gradle",
        ".idea",
        "node_modules",
        "build",
        "lost+found",
        "lost.dir",
    )

    private val blockedSystemPaths = setOf(
        "/",
        "/android",
        "/android/data",
        "/android/obb",
        "/data",
        "/dev",
        "/obb",
        "/proc",
        "/product",
        "/storage",
        "/storage/emulated",
        "/storage/emulated/0",
        "/system",
        "/vendor",
    )

    fun isDangerousPath(file: File): Boolean {
        val path = canonicalPath(file).lowercase().trimEnd('/').ifBlank { "/" }
        if (path in blockedSystemPaths) return true
        if (file.name.lowercase() in knownDevArtifactNames) return true
        return path.endsWith("/android") ||
            path.endsWith("/android/data") ||
            path.endsWith("/android/obb")
    }

    fun isSafeFolderToDelete(folder: File, allowedRoot: File): Boolean {
        if (!folder.isDirectory || isDangerousPath(folder)) return false
        val root = canonicalPath(allowedRoot).trimEnd('/') + "/"
        val candidate = canonicalPath(folder).trimEnd('/') + "/"
        if (candidate == root || !candidate.startsWith(root)) return false
        return !isSymlink(folder)
    }

    fun isSymlink(file: File): Boolean = runCatching {
        file.canonicalFile != file.absoluteFile
    }.getOrDefault(true)

    private fun canonicalPath(file: File): String = runCatching {
        file.canonicalPath
    }.getOrElse { file.absolutePath }
}
