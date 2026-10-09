package com.riccardopinato.omnicleaner.cleaner

import android.os.Environment
import java.io.File

object PathSafety {
    val knownDevArtifactNames: Set<String> = setOf(
        ".git",
        ".gradle",
        ".idea",
        "build",
        ".dart_tool",
        "node_modules",
        "__pycache__",
    )

    private val blockedExactPaths = setOf(
        "/",
        "/system",
        "/vendor",
        "/product",
        "/data",
        "/proc",
        "/sys",
        "/dev",
        "/storage",
        "/mnt",
    )

    private val blockedSystemPrefixes = listOf(
        "/system/",
        "/vendor/",
        "/product/",
        "/data/",
        "/proc/",
        "/sys/",
        "/dev/",
    )

    fun isDangerousPath(file: File): Boolean {
        val canonical = runCatching { file.canonicalFile }.getOrElse { return true }
        val path = canonical.path.trimEnd('/').ifBlank { "/" }
        if (path in blockedExactPaths) return true
        if (blockedSystemPrefixes.any(path::startsWith)) return true

        val externalRoot = runCatching {
            Environment.getExternalStorageDirectory().canonicalFile
        }.getOrNull()
        if (externalRoot != null && canonical == externalRoot) return true

        return canonical.parentFile == null
    }

    fun isSafeFolderToDelete(file: File): Boolean {
        if (!file.exists() || !file.isDirectory) return false
        if (isDangerousPath(file)) return false

        val canonical = runCatching { file.canonicalFile }.getOrElse { return false }
        val path = canonical.path
        if (path.length < 4) return false
        if (canonical.name in setOf("Android", "DCIM", "Pictures", "Movies", "Music", "Download", "Documents")) {
            return false
        }
        return true
    }
}

object FileInventoryPolicy {
    private val ignoredNames = PathSafety.knownDevArtifactNames + setOf(
        ".nomedia",
        "lost+found",
        ".Trash",
        ".Trash-1000",
        ".omnicleaner-trash",
    )

    fun shouldIgnore(file: File, root: File): Boolean {
        if (file == root) return false
        if (file.name in ignoredNames) return true
        return runCatching {
            val rootPath = root.canonicalPath.trimEnd(File.separatorChar) + File.separator
            val filePath = file.canonicalPath
            !filePath.startsWith(rootPath)
        }.getOrDefault(true)
    }
}
