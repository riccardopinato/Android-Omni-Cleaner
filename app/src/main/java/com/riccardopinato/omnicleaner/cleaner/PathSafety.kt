package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.util.Locale

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

    private val protectedTopLevelUserFolders = setOf(
        "android",
        "alarms",
        "dcim",
        "documents",
        "download",
        "movies",
        "music",
        "notifications",
        "pictures",
        "podcasts",
        "ringtones",
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
        val path = canonicalPath(file).lowercase(Locale.ROOT).trimEnd('/').ifBlank { "/" }
        if (path in blockedSystemPaths) return true
        if (file.name.lowercase(Locale.ROOT) in knownDevArtifactNames) return true
        return path.endsWith("/android") ||
            path.endsWith("/android/data") ||
            path.endsWith("/android/obb")
    }

    fun isSafeFolderToDelete(folder: File, allowedRoot: File): Boolean {
        if (!folder.isDirectory || isDangerousPath(folder)) return false

        val rootPath = canonicalPath(allowedRoot).trimEnd(File.separatorChar)
        val candidatePath = canonicalPath(folder).trimEnd(File.separatorChar)
        val rootPrefix = rootPath + File.separator

        if (candidatePath == rootPath || !candidatePath.startsWith(rootPrefix)) return false
        if (isSymlink(folder)) return false

        val relative = candidatePath.removePrefix(rootPrefix)
        val isTopLevel = File.separatorChar !in relative
        if (
            isTopLevel &&
            relative.lowercase(Locale.ROOT) in protectedTopLevelUserFolders
        ) {
            return false
        }

        return true
    }

    fun isSymlink(file: File): Boolean = runCatching {
        file.canonicalFile != file.absoluteFile
    }.getOrDefault(true)

    private fun canonicalPath(file: File): String = runCatching {
        file.canonicalPath
    }.getOrElse { file.absolutePath }
}
