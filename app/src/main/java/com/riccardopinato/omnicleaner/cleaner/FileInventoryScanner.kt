package com.riccardopinato.omnicleaner.cleaner

import com.riccardopinato.omnicleaner.model.DuplicateFile
import com.riccardopinato.omnicleaner.model.DuplicateGroup
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class IndexedFile(
    val file: File,
    val size: Long,
    val modified: Long,
)

data class InventoryMetadata(
    val directory: Boolean,
    val size: Long = 0L,
    val modified: Long = 0L,
)

fun interface InventoryMetadataReader {
    fun read(file: File, fallbackSize: Long): InventoryMetadata
}

object NioInventoryMetadata : InventoryMetadataReader {
    override fun read(file: File, fallbackSize: Long): InventoryMetadata {
        return InventoryMetadata(
            directory = file.isDirectory,
            size = if (file.isFile) runCatching { file.length() }.getOrDefault(fallbackSize) else 0L,
            modified = runCatching { file.lastModified() }.getOrDefault(0L),
        )
    }
}

object FileInventoryScanner {
    private const val HASH_CHUNK = 64 * 1024

    suspend fun collect(
        root: File,
        minBytes: Long = 0L,
        onProgress: (Int) -> Unit = {},
        onVisited: (File) -> Unit = {},
    ): List<IndexedFile> = collectUsing(root, minBytes, onProgress, onVisited, NioInventoryMetadata)

    suspend fun collectUsing(
        root: File,
        minBytes: Long = 0L,
        onProgress: (Int) -> Unit = {},
        onVisited: (File) -> Unit = {},
        metadataReader: InventoryMetadataReader,
    ): List<IndexedFile> = withContext(Dispatchers.IO) {
        if (!root.exists() || !root.canRead()) return@withContext emptyList()

        val result = ArrayList<IndexedFile>()
        val queue = ArrayDeque<File>()
        val visitedDirectories = HashSet<String>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = queue.removeLast()
            if (FileInventoryPolicy.shouldIgnore(current, root)) continue

            val metadata = runCatching { metadataReader.read(current, 0L) }.getOrNull() ?: continue
            onVisited(current)
            visited++
            if (visited % 64 == 0) onProgress(visited)

            if (metadata.directory) {
                val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
                if (!visitedDirectories.add(canonical)) continue
                val children = runCatching { current.listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())
                children.forEach(queue::add)
            } else if (metadata.size >= minBytes && current.isFile) {
                result += IndexedFile(current, metadata.size, metadata.modified)
            }
        }

        onProgress(visited)
        result
    }

    suspend fun duplicates(
        files: List<IndexedFile>,
        minBytes: Long = 1L,
        onProgress: (Int) -> Unit = {},
    ): List<DuplicateGroup> {
        val groups = ArrayList<DuplicateGroup>()
        duplicatesProgressive(files, minBytes, onProgress) { groups += it }
        return groups
    }

    suspend fun duplicatesProgressive(
        files: List<IndexedFile>,
        minBytes: Long = 1L,
        onProgress: (Int) -> Unit = {},
        onGroup: (DuplicateGroup) -> Unit = {},
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val sameSize = files
            .asSequence()
            .filter { it.size >= minBytes && it.file.exists() }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }

        val output = ArrayList<DuplicateGroup>()
        var processed = 0
        val total = sameSize.sumOf { it.size }.coerceAtLeast(1)

        for (sizeGroup in sameSize) {
            coroutineContext.ensureActive()
            val quickGroups = LinkedHashMap<String, MutableList<IndexedFile>>()
            for (indexed in sizeGroup) {
                coroutineContext.ensureActive()
                val quick = digest(indexed, full = false) ?: continue
                quickGroups.getOrPut(quick) { ArrayList() } += indexed
                processed++
                onProgress((processed * 100 / total).coerceIn(0, 100))
            }

            for (quickGroup in quickGroups.values.filter { it.size > 1 }) {
                val fullGroups = LinkedHashMap<String, MutableList<IndexedFile>>()
                for (indexed in quickGroup) {
                    coroutineContext.ensureActive()
                    val full = digest(indexed, full = true) ?: continue
                    fullGroups.getOrPut(full) { ArrayList() } += indexed
                }
                for ((hash, matches) in fullGroups) {
                    if (matches.size < 2) continue
                    val duplicateGroup = DuplicateGroup(
                        hash = hash,
                        sizeBytes = matches.first().size,
                        files = matches
                            .sortedBy { it.modified }
                            .map { indexed ->
                                DuplicateFile(
                                    file = indexed.file,
                                    sizeBytes = indexed.size,
                                    lastModified = indexed.modified,
                                )
                            },
                    )
                    output += duplicateGroup
                    onGroup(duplicateGroup)
                }
            }
        }

        onProgress(100)
        output.sortedByDescending { it.totalWasteBytes }
    }

    suspend fun identical(first: File, second: File): Boolean = withContext(Dispatchers.IO) {
        if (!first.isFile || !second.isFile || first.length() != second.length()) return@withContext false
        FileInputStream(first).buffered().use { a ->
            FileInputStream(second).buffered().use { b ->
                val left = ByteArray(64 * 1024)
                val right = ByteArray(64 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val la = a.read(left)
                    val lb = b.read(right)
                    if (la != lb) return@withContext false
                    if (la < 0) return@withContext true
                    for (index in 0 until la) {
                        if (left[index] != right[index]) return@withContext false
                    }
                }
            }
        }
        false
    }

    private suspend fun digest(indexed: IndexedFile, full: Boolean): String? = withContext(Dispatchers.IO) {
        val file = indexed.file
        if (!file.isFile || file.length() != indexed.size || file.lastModified() != indexed.modified) {
            return@withContext null
        }

        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { stream ->
            if (full || indexed.size <= HASH_CHUNK * 2L) {
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    md.update(buffer, 0, count)
                }
            } else {
                val head = ByteArray(HASH_CHUNK)
                val headCount = stream.read(head)
                if (headCount > 0) md.update(head, 0, headCount)

                val tail = ByteArray(HASH_CHUNK)
                java.io.RandomAccessFile(file, "r").use { raf ->
                    raf.seek((indexed.size - HASH_CHUNK).coerceAtLeast(0L))
                    val tailCount = raf.read(tail)
                    if (tailCount > 0) md.update(tail, 0, tailCount)
                }
                md.update(indexed.size.toString().toByteArray())
            }
        }

        md.digest().joinToString("") { "%02x".format(it) }
    }
}
