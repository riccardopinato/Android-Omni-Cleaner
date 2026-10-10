package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class ThumbnailFileCleanup {
    suspend fun inventory(
        directory: File,
        onProgress: (Int, File) -> Unit = { _, _ -> },
    ): ThumbnailSnapshot = withContext(Dispatchers.IO) {
        val root = runCatching { directory.canonicalFile }.getOrNull()
            ?: return@withContext ThumbnailSnapshot(directory, emptyList(), 0)
        if (!root.isDirectory || PathSafety.isSymlink(root)) {
            return@withContext ThumbnailSnapshot(directory, emptyList(), 0)
        }

        val stack = ArrayDeque<File>()
        val visited = HashSet<String>()
        val stamps = ArrayList<ThumbnailFileStamp>()
        var subfolders = 0
        var checked = 0
        stack.add(root)

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = stack.removeLast()
            val canonical = runCatching { current.canonicalFile }.getOrNull() ?: continue
            if (!visited.add(canonical.path)) continue
            val children = runCatching { canonical.listFiles() }.getOrNull() ?: continue

            for (child in children) {
                coroutineContext.ensureActive()
                when {
                    child.isDirectory && !PathSafety.isSymlink(child) -> {
                        val candidate = runCatching { child.canonicalFile }.getOrNull() ?: continue
                        if (candidate.path.startsWith(root.path + File.separator)) {
                            subfolders++
                            stack.add(candidate)
                        }
                    }
                    regular(child) -> {
                        checked++
                        onProgress(checked, child)
                        stamp(child)?.let(stamps::add)
                    }
                }
            }
        }

        ThumbnailSnapshot(root, stamps, subfolders)
    }

    suspend fun clean(
        snapshot: ThumbnailSnapshot,
        approvedPaths: Set<String> = snapshot.files.mapTo(linkedSetOf()) { it.file.absolutePath },
        onProgress: (ThumbnailFileStamp) -> Unit = {},
    ): ThumbnailCleanupResult = withContext(Dispatchers.IO) {
        val root = runCatching { snapshot.directory.canonicalFile }.getOrNull()
            ?: return@withContext ThumbnailCleanupResult(0, snapshot.files.size)
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        var removed = 0
        var rejected = 0

        for (expected in snapshot.files) {
            coroutineContext.ensureActive()
            if (expected.file.absolutePath !in approvedPaths) continue
            onProgress(expected)

            val current = runCatching { expected.file.canonicalFile }.getOrNull()
            if (current == null ||
                !current.path.startsWith(rootPrefix) ||
                !regular(current) ||
                current.length() != expected.bytes ||
                current.lastModified() != expected.modified
            ) {
                rejected++
                continue
            }

            val currentHash = runCatching { hash(current) }.getOrNull()
            if (currentHash == null || currentHash != expected.sha256) {
                rejected++
                continue
            }

            if (current.delete()) removed++ else rejected++
        }

        root.walkBottomUp()
            .filter { it != root && it.isDirectory }
            .forEach { dir ->
                runCatching {
                    if (dir.listFiles()?.isEmpty() == true) dir.delete()
                }
            }

        ThumbnailCleanupResult(removed, rejected)
    }

    private fun regular(file: File): Boolean = runCatching {
        file.canonicalFile == file.absoluteFile && file.isFile && file.canRead()
    }.getOrDefault(false)

    private fun stamp(file: File): ThumbnailFileStamp? {
        if (!regular(file)) return null
        val beforeBytes = file.length().coerceAtLeast(0L)
        val beforeModified = file.lastModified().coerceAtLeast(0L)
        val digest = hash(file)
        if (file.length() != beforeBytes || file.lastModified() != beforeModified) return null
        return ThumbnailFileStamp(file, beforeBytes, beforeModified, digest)
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
