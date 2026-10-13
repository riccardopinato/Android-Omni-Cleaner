package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
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

            val absolute = directory.absoluteFile
            val canonicalFile = runCatching { directory.canonicalFile }.getOrNull()
            if (canonicalFile == null) {
                excluded++
                continue
            }
            if (directory != root && canonicalFile != absolute) {
                excluded++
                continue
            }
            if (!visited.add(canonicalFile.path)) {
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

                val metadata = FileSystemIdentity.read(child)
                if (metadata == null) {
                    unreadable++
                    continue
                }
                if (metadata.isSymlink) {
                    excluded++
                    continue
                }

                when {
                    metadata.isDirectory -> stack.add(child)
                    metadata.isRegularFile -> files += IndexedFile(
                        file = child,
                        size = metadata.sizeBytes,
                        modified = metadata.modifiedMillis,
                        revision = metadata.revision,
                        physicalIdentity = metadata.physicalIdentity,
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
        return duplicatesProgressive(
            files = inventory.files,
            onProgress = onProgress,
        )
    }

    suspend fun duplicatesProgressive(
        files: List<IndexedFile>,
        hashCache: DuplicateHashCache? = null,
        parallelism: Int = boundedHashWorkerCount(),
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val bySize = uniquePhysicalFiles(files)
            .asSequence()
            .filter { it.size > 0L }
            .groupBy { it.size }
            .values
            .filter { it.size > 1 }

        val totalCandidates = bySize.sumOf { it.size }
        val result = ArrayList<DuplicateGroup>()
        val progressLock = Any()
        var processed = 0
        onProgress?.invoke(0, totalCandidates)

        fun reportProcessed() {
            synchronized(progressLock) {
                processed++
                if (
                    processed == totalCandidates ||
                    processed % DUPLICATE_PROGRESS_INTERVAL == 0
                ) {
                    onProgress?.invoke(processed, totalCandidates)
                }
            }
        }

        val workerCount = parallelism.coerceAtLeast(1)

        for (sameSize in bySize) {
            coroutineContext.ensureActive()

            // A cheap prefix/suffix fingerprint avoids full reads for most
            // same-size files. Sample hashes are revision-bound in the
            // persistent index and may be reused across sessions.
            val sampled = boundedHashFiles(
                items = sameSize,
                parallelism = workerCount,
            ) { item ->
                try {
                    if (!unchanged(item)) return@boundedHashFiles null

                    val cached = hashCache?.sampleHash(item)
                    val sample = cached ?: nullableUnlessCancelled {
                        sampleFingerprintCancellable(item.file, item.size)
                    }?.also { computed ->
                        if (unchanged(item)) {
                            hashCache?.putSampleHash(item, computed)
                            if (sampleCoversWholeFile(item.size)) {
                                hashCache?.putFullHash(item, computed)
                            }
                        }
                    }

                    sample?.takeIf { unchanged(item) }
                } finally {
                    reportProcessed()
                }
            }

            val bySample = linkedMapOf<String, MutableList<IndexedFile>>()
            for (sampledItem in sampled) {
                bySample.getOrPut(sampledItem.hash) { ArrayList() }.add(sampledItem.item)
            }

            for (sampleMatches in bySample.values) {
                coroutineContext.ensureActive()
                if (sampleMatches.size <= 1) continue

                val hashed = boundedHashFiles(
                    items = sampleMatches,
                    parallelism = workerCount,
                ) { item ->
                    if (!unchanged(item)) return@boundedHashFiles null

                    val cached = hashCache?.fullHash(item)
                    val sampleAsFull = if (
                        cached == null &&
                        sampleCoversWholeFile(item.size)
                    ) {
                        hashCache?.sampleHash(item)
                    } else {
                        null
                    }
                    val hash = cached ?: sampleAsFull ?: nullableUnlessCancelled {
                        digestCancellable(item.file)
                    }?.also { computed ->
                        if (unchanged(item)) {
                            hashCache?.putFullHash(item, computed)
                        }
                    }

                    hash?.takeIf { unchanged(item) }
                }

                val byHash = linkedMapOf<String, MutableList<IndexedFile>>()
                for (hashedItem in hashed) {
                    byHash.getOrPut(hashedItem.hash) { ArrayList() }.add(hashedItem.item)
                }

                for ((hash, hashes) in byHash) {
                    coroutineContext.ensureActive()
                    if (hashes.size <= 1) continue

                    // SHA-256 reduces the candidate set, but destructive
                    // duplicate decisions still require byte-for-byte identity.
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
                val leftBuffer = ByteArray(IO_BUFFER_BYTES)
                val rightBuffer = ByteArray(IO_BUFFER_BYTES)
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

    fun unchanged(item: IndexedFile): Boolean {
        val current = FileSystemIdentity.read(item.file) ?: return false
        if (!current.isRegularFile || current.isSymlink) return false
        if (current.sizeBytes != item.size || current.modifiedMillis != item.modified) return false

        val expectedRevision = item.revision
        return expectedRevision == null || current.revision == expectedRevision
    }

    private suspend fun sampleFingerprintCancellable(
        file: File,
        expectedBytes: Long,
    ): String {
        val md = MessageDigest.getInstance("SHA-256")
        val sampleBytes = DUPLICATE_SAMPLE_BYTES.toInt()

        if (expectedBytes <= DUPLICATE_SAMPLE_BYTES * 2L) {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(IO_BUFFER_BYTES)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                }
            }
        } else {
            RandomAccessFile(file, "r").use { input ->
                val buffer = ByteArray(IO_BUFFER_BYTES)
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
            val buffer = ByteArray(IO_BUFFER_BYTES)
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
            val buffer = ByteArray(IO_BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sampleCoversWholeFile(sizeBytes: Long): Boolean =
        sizeBytes <= DUPLICATE_SAMPLE_BYTES * 2L

    private companion object {
        const val DUPLICATE_SAMPLE_BYTES: Long = 64L * 1024L
        const val IO_BUFFER_BYTES: Int = 256 * 1024
        const val DUPLICATE_PROGRESS_INTERVAL: Int = 16
    }
}



internal fun uniquePhysicalFiles(files: List<IndexedFile>): List<IndexedFile> =
    files.distinctBy { item ->
        item.physicalIdentity?.let { identity ->
            "physical:${identity.deviceId}:${identity.inode}"
        } ?: "path:${item.file.absolutePath}"
    }

internal data class IndexedHashResult(
    val index: Int,
    val item: IndexedFile,
    val hash: String,
)

internal suspend fun boundedHashFiles(
    items: List<IndexedFile>,
    parallelism: Int,
    onInFlightChanged: ((Int) -> Unit)? = null,
    transform: suspend (IndexedFile) -> String?,
): List<IndexedHashResult> = coroutineScope {
    if (items.isEmpty()) return@coroutineScope emptyList()

    val nextIndex = AtomicInteger(0)
    val inFlight = AtomicInteger(0)
    val results = ArrayList<IndexedHashResult>()
    val resultLock = Any()
    val workers = boundedHashWorkerLaunchCount(items.size, parallelism)

    List(workers) {
        launch(Dispatchers.IO) {
            while (true) {
                ensureActive()
                val index = nextIndex.getAndIncrement()
                if (index >= items.size) break

                val item = items[index]
                val active = inFlight.incrementAndGet()
                onInFlightChanged?.invoke(active)
                try {
                    val hash = transform(item)
                    if (hash != null) {
                        synchronized(resultLock) {
                            results += IndexedHashResult(index, item, hash)
                        }
                    }
                } finally {
                    val remaining = inFlight.decrementAndGet()
                    onInFlightChanged?.invoke(remaining)
                }
            }
        }
    }.joinAll()

    results.sortedBy { it.index }
}

internal fun boundedHashWorkerLaunchCount(
    itemCount: Int,
    parallelism: Int,
): Int = minOf(itemCount.coerceAtLeast(0), parallelism.coerceAtLeast(1))

internal fun boundedHashWorkerCount(
    availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
): Int = when {
    availableProcessors <= 1 -> 1
    availableProcessors <= 3 -> 2
    availableProcessors <= 5 -> 2
    availableProcessors <= 7 -> 3
    else -> 4
}

internal suspend fun <T> nullableUnlessCancelled(
    block: suspend () -> T,
): T? = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (_: Throwable) {
    null
}
