package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
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

            if (directory != root && FileInventoryPolicy.shouldIgnore(root, directory)) {
                excluded++
                continue
            }

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
        return duplicatesProgressive(inventory.files)
    }

    suspend fun duplicates(files: List<IndexedFile>): List<DuplicateGroup> =
        duplicatesProgressive(files)

    suspend fun duplicatesProgressive(
        root: File,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<DuplicateGroup> {
        val inventory = collect(root)
        return duplicatesProgressive(inventory.files, onProgress)
    }

    suspend fun duplicatesProgressive(
        files: List<IndexedFile>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val bySize = files
            .asSequence()
            .filter { it.size > 0L }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }

        val totalCandidates = bySize.sumOf { it.size }
        var processed = 0
        val result = ArrayList<DuplicateGroup>()

        for (sameSize in bySize) {
            coroutineContext.ensureActive()

            // A cheap prefix/suffix fingerprint avoids reading every same-size
            // file in full. Identical files necessarily share this fingerprint;
            // collisions are still verified later with full SHA-256 and a
            // byte-for-byte comparison, so correctness is unchanged.
            val bySample = linkedMapOf<String, MutableList<IndexedFile>>()
            for (item in sameSize) {
                coroutineContext.ensureActive()

                if (unchanged(item)) {
                    val sample = runCatching {
                        sampleFingerprintCancellable(item.file, item.size)
                    }.getOrNull()
                    if (sample != null && unchanged(item)) {
                        bySample.getOrPut(sample) { ArrayList() }.add(item)
                    }
                }

                processed++
                onProgress?.invoke(processed, totalCandidates)
            }

            for (sampleMatches in bySample.values) {
                coroutineContext.ensureActive()
                if (sampleMatches.size <= 1) continue

                val byHash = linkedMapOf<String, MutableList<IndexedFile>>()
                for (item in sampleMatches) {
                    coroutineContext.ensureActive()
                    if (!unchanged(item)) continue

                    val hash = runCatching {
                        digestCancellable(item.file)
                    }.getOrNull()
                    if (hash != null && unchanged(item)) {
                        byHash.getOrPut(hash) { ArrayList() }.add(item)
                    }
                }

                for ((hash, hashes) in byHash) {
                coroutineContext.ensureActive()
                if (hashes.size <= 1) continue

                // SHA-256 is used to reduce the candidate set, but a hash match
                // is not accepted as proof of identical content. The donor APK
                // also exposed an explicit identical() verification stage.
                val verifiedClusters = ArrayList<MutableList<IndexedFile>>()

                for (candidate in hashes) {
                    coroutineContext.ensureActive()
                    if (!unchanged(candidate)) continue

                    var matched: MutableList<IndexedFile>? = null
                    for (cluster in verifiedClusters) {
                        val representative = cluster.firstOrNull() ?: continue
                        if (
                            unchanged(representative) &&
                            identical(representative.file, candidate.file) &&
                            unchanged(representative) &&
                            unchanged(candidate)
                        ) {
                            matched = cluster
                            break
                        }
                    }

                    if (matched != null) {
                        matched.add(candidate)
                    } else {
                        verifiedClusters += arrayListOf(candidate)
                    }
                }

                for (verified in verifiedClusters) {
                    if (verified.size <= 1) continue

                    val sorted = verified.sortedWith(
                        compareBy<IndexedFile> { it.modified }
                            .thenBy { it.file.absolutePath },
                    )

                    if (sorted.any { !unchanged(it) }) continue

                    result += DuplicateGroup(
                        hash = hash,
                        sizeBytes = sorted.first().size,
                        files = sorted.map { item ->
                            val path = item.file.absolutePath
                            DuplicateFile(
                                file = item.file,
                                name = item.file.name,
                                path = path,
                                displayPath = path,
                                sizeBytes = item.size,
                                lastModified = item.modified,
                                extension = item.file.extension.lowercase(),
                            )
                        },
                    )
                }
                }
            }
        }

        result.sortedByDescending { it.totalWasteBytes }
    }

    suspend fun identical(first: File, second: File): Boolean = withContext(Dispatchers.IO) {
        if (!first.isFile || !second.isFile) return@withContext false
        val expectedBytes = first.length()
        if (expectedBytes <= 0L || second.length() != expectedBytes) return@withContext false

        FileInputStream(first).use { left ->
            FileInputStream(second).use { right ->
                val leftBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val rightBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var compared = 0L

                while (true) {
                    coroutineContext.ensureActive()
                    val leftRead = left.read(leftBuffer)
                    val rightRead = right.read(rightBuffer)
                    if (leftRead != rightRead) return@withContext false
                    if (leftRead <= 0) break

                    compared += leftRead
                    for (index in 0 until leftRead) {
                        if (leftBuffer[index] != rightBuffer[index]) return@withContext false
                    }
                }

                compared == expectedBytes
            }
        }
    }

    fun unchanged(item: IndexedFile): Boolean =
        item.file.isFile &&
            item.file.length() == item.size &&
            item.file.lastModified() == item.modified

    private suspend fun sampleFingerprintCancellable(
        file: File,
        expectedBytes: Long,
    ): String {
        val md = MessageDigest.getInstance("SHA-256")
        val sampleBytes = DUPLICATE_SAMPLE_BYTES.toInt()

        if (expectedBytes <= DUPLICATE_SAMPLE_BYTES * 2L) {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                }
            }
        } else {
            RandomAccessFile(file, "r").use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var remaining = sampleBytes
                while (remaining > 0) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                    remaining -= read
                }

                input.seek(expectedBytes - DUPLICATE_SAMPLE_BYTES)
                remaining = sampleBytes
                while (remaining > 0) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                    remaining -= read
                }
            }
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun digestCancellable(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
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

    private companion object {
        const val DUPLICATE_SAMPLE_BYTES: Long = 64L * 1024L
    }
}
