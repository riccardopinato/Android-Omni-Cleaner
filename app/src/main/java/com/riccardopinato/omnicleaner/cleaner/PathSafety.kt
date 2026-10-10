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

    private val blockedExactPaths = setOf(
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

    private val blockedPrefixRoots = setOf(
        "/data",
        "/dev",
        "/proc",
        "/product",
        "/system",
        "/vendor",
    )

    fun isDangerousPath(file: File): Boolean {
        val path = canonicalPath(file)
            .replace('\\', '/')
            .lowercase(Locale.ROOT)
            .trimEnd('/')
            .ifBlank { "/" }

        if (path in blockedExactPaths) return true
        if (blockedPrefixRoots.any { path == it || path.startsWith("$it/") }) return true
        if (file.name.lowercase(Locale.ROOT) in knownDevArtifactNames) return true

        // General cleanup never mutates app-owned Android/* trees. Dedicated
        // thumbnail cleanup has its own stronger snapshot/hash contract.
        if (
            path.contains("/android/data/") ||
            path.contains("/android/obb/") ||
            path.contains("/android/media/")
        ) {
            return true
        }

        return path.endsWith("/android") ||
            path.endsWith("/android/data") ||
            path.endsWith("/android/obb") ||
            path.endsWith("/android/media")
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

    internal fun isWithinRoot(candidate: File, allowedRoot: File): Boolean {
        val root = runCatching { allowedRoot.canonicalFile }.getOrNull() ?: return false
        val target = runCatching { candidate.canonicalFile }.getOrNull() ?: return false
        val prefix = root.path.trimEnd(File.separatorChar) + File.separator
        return target != root && target.path.startsWith(prefix)
    }

    internal fun isWithinOrEqualRoot(candidate: File, allowedRoot: File): Boolean {
        val root = runCatching { allowedRoot.canonicalFile }.getOrNull() ?: return false
        val target = runCatching { candidate.canonicalFile }.getOrNull() ?: return false
        val prefix = root.path.trimEnd(File.separatorChar) + File.separator
        return target == root || target.path.startsWith(prefix)
    }

    private fun canonicalPath(file: File): String = runCatching {
        file.canonicalPath
    }.getOrElse { file.absolutePath }
}
