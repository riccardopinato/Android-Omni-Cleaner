package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class StorageInventorySnapshot(
    val rootPath: String,
    val inventory: FileInventory,
    val createdAtMillis: Long,
    val scanElapsedMillis: Long,
    val cacheHit: Boolean,
)

/**
 * Shared in-process inventory for storage intelligence.
 *
 * Destructive operations still revalidate each selected file immediately before
 * mutation; this cache only avoids repeated read-only filesystem traversals.
 */
object StorageInventoryRepository {
    const val CACHE_TTL_MS: Long = 120_000L

    private val mutex = Mutex()
    private val scanner = FileInventoryScanner()

    @Volatile
    private var cached: StorageInventorySnapshot? = null

    @Volatile
    private var invalidationGeneration: Long = 0L

    suspend fun snapshot(
        root: File,
        forceRefresh: Boolean = false,
        onProgress: ((checked: Int, currentPath: String) -> Unit)? = null,
        nowMillis: () -> Long = System::currentTimeMillis,
    ): StorageInventorySnapshot {
        val rootPath = canonical(root)
        val now = nowMillis()
        cached?.takeIf { current ->
            !forceRefresh &&
                current.rootPath == rootPath &&
                now >= current.createdAtMillis &&
                now - current.createdAtMillis <= CACHE_TTL_MS
        }?.let { return it.copy(cacheHit = true) }

        return mutex.withLock {
            val lockedNow = nowMillis()
            cached?.takeIf { current ->
                !forceRefresh &&
                    current.rootPath == rootPath &&
                    lockedNow >= current.createdAtMillis &&
                    lockedNow - current.createdAtMillis <= CACHE_TTL_MS
            }?.let { return@withLock it.copy(cacheHit = true) }

            val scanGeneration = invalidationGeneration
            val started = nowMillis()
            val inventory = scanner.collect(root, onProgress)
            val finished = nowMillis()
            StorageInventorySnapshot(
                rootPath = rootPath,
                inventory = inventory,
                createdAtMillis = finished,
                scanElapsedMillis = (finished - started).coerceAtLeast(0L),
                cacheHit = false,
            ).also { snapshot ->
                if (isInventoryGenerationCurrent(scanGeneration, invalidationGeneration)) {
                    cached = snapshot
                }
            }
        }
    }

    fun invalidate() {
        invalidationGeneration++
        cached = null
    }

    internal fun currentForTests(): StorageInventorySnapshot? = cached

    private fun canonical(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
}

internal fun isInventoryGenerationCurrent(
    scanGeneration: Long,
    currentGeneration: Long,
): Boolean = scanGeneration == currentGeneration
