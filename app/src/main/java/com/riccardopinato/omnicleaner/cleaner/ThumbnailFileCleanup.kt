package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ThumbnailFileStamp(
    val file: File,
    val bytes: Long,
    val modified: Long,
    val sha256: String,
)

data class ThumbnailSnapshot(
    val directory: File,
    val files: List<ThumbnailFileStamp>,
    val subfolders: Int,
) {
    val bytes: Long get() = files.sumOf { it.bytes }
}

data class ThumbnailCleanupResult(
    val removed: Int,
    val rejected: Int,
)

object ThumbnailFileCleanup {
    suspend fun inventory(
        directory: File,
        onProgress: (Int) -> Unit = {},
        onFile: (Int, File) -> Unit = { _, _ -> },
    ): ThumbnailSnapshot = withContext(Dispatchers.IO) {
        if (!directory.isDirectory || !directory.canRead()) {
            return@withContext ThumbnailSnapshot(directory, emptyList(), 0)
        }

        val queue = ArrayDeque<File>()
        val files = ArrayList<ThumbnailFileStamp>()
        val seen = HashSet<String>()
        var subfolders = 0
        var visited = 0
        queue += directory

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = queue.removeLast()
            if (current.isDirectory) {
                val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
                if (!seen.add(canonical)) continue
                if (current != directory) subfolders++
                current.listFiles()?.forEach(queue::add)
                continue
            }

            if (!current.isFile) continue
            visited++
            val stamp = stamp(current) ?: continue
            files += stamp
            onFile(visited, current)
            if (visited % 32 == 0) onProgress(visited)
        }

        onProgress(visited)
        ThumbnailSnapshot(directory, files, subfolders)
    }

    suspend fun clean(
        snapshot: ThumbnailSnapshot,
        onRemoved: (ThumbnailFileStamp) -> Unit = {},
        protected: Set<String> = emptySet(),
        onRejected: (ThumbnailFileStamp) -> Unit = {},
    ): ThumbnailCleanupResult = withContext(Dispatchers.IO) {
        var removed = 0
        var rejected = 0

        for (original in snapshot.files) {
            coroutineContext.ensureActive()
            val file = original.file
            val current = stamp(file)
            val safe = current != null &&
                current.bytes == original.bytes &&
                current.modified == original.modified &&
                current.sha256 == original.sha256 &&
                file.absolutePath !in protected

            if (!safe || !file.delete()) {
                rejected++
                onRejected(original)
            } else {
                removed++
                onRemoved(original)
            }
        }

        ThumbnailCleanupResult(removed, rejected)
    }

    private suspend fun stamp(file: File): ThumbnailFileStamp? {
        if (!file.isFile || !file.canRead()) return null
        return ThumbnailFileStamp(
            file = file,
            bytes = file.length(),
            modified = file.lastModified(),
            sha256 = hash(file),
        )
    }

    private suspend fun hash(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                md.update(buffer, 0, count)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
}
