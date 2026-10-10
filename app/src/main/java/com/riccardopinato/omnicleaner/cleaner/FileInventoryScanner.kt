package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class FileInventoryScanner {
    suspend fun collect(
        root: File,
        onProgress: ((checked: Int, currentPath: String) -> Unit)? = null,
    ): FileInventory = withContext(Dispatchers.IO) {
        if (!root.exists() || !root.isDirectory) return@withContext FileInventory(emptyList(), 1, 0)

        val stack = ArrayDeque<File>()
        val visited = HashSet<String>()
        val files = ArrayList<IndexedFile>()
        var unreadable = 0
        var excluded = 0
        var checked = 0
        stack.add(root)

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = stack.removeLast()
            val canonical = runCatching { directory.canonicalPath }.getOrNull()
            if (canonical == null || !visited.add(canonical)) {
                excluded++
                continue
            }
            if (directory != root && PathSafety.isSymlink(directory)) {
                excluded++
                continue
            }

            val children = runCatching { directory.listFiles() }.getOrNull()
            if (children == null) {
                unreadable++
                continue
            }

            for (child in children) {
                coroutineContext.ensureActive()
                checked++
                if (checked % 128 == 0) onProgress?.invoke(checked, child.absolutePath)
                when {
                    child.isDirectory -> stack.add(child)
                    child.isFile -> files += IndexedFile(
                        file = child,
                        size = child.length().coerceAtLeast(0L),
                        modified = child.lastModified().coerceAtLeast(0L),
                    )
                }
            }
        }

        FileInventory(files, unreadable, excluded)
    }

    suspend fun duplicates(root: File): List<DuplicateGroup> {
        val inventory = collect(root)
        return duplicates(inventory.files)
    }

    suspend fun duplicates(files: List<IndexedFile>): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val groups = ArrayList<DuplicateGroup>()
        val sameSizeGroups = files
            .asSequence()
            .filter { it.size > 0L }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }

        for (sameSize in sameSizeGroups) {
            coroutineContext.ensureActive()
            val byDigest = linkedMapOf<String, MutableList<IndexedFile>>()
            for (item in sameSize) {
                coroutineContext.ensureActive()
                val hash = runCatching { digest(item.file) }.getOrNull() ?: continue
                byDigest.getOrPut(hash) { ArrayList() }.add(item)
            }
            for ((hash, matches) in byDigest) {
                if (matches.size <= 1) continue
                groups += DuplicateGroup(
                    hash = hash,
                    files = matches
                        .sortedWith(compareBy<IndexedFile> { it.modified }.thenBy { it.file.absolutePath })
                        .map { DuplicateFile(it.file, it.size, it.modified) },
                )
            }
        }

        groups.sortedByDescending { it.totalWasteBytes }
    }

    fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
