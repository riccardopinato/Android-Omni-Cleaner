package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Short-lived process cache for expensive full-storage inventory walks.
 *
 * All mutation paths invalidate this repository through CleanupEventBus. The
 * destructive layer still revalidates every candidate immediately before
 * mutation, so a cached inventory can improve discovery performance without
 * weakening cleanup safety.
 */
object SharedFileInventoryRepository {
    private const val DEFAULT_MAX_AGE_MS = 30_000L

    private val mutex = Mutex()

    @Volatile
    private var cached: CachedInventory? = null

    suspend fun get(
        root: File,
        forceRefresh: Boolean = false,
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
    ): FileInventory = mutex.withLock {
        val rootPath = canonicalKey(root)
        val now = System.currentTimeMillis()
        val current = cached
        if (
            !forceRefresh &&
            current != null &&
            current.rootPath == rootPath &&
            isInventoryCacheFresh(current.createdAt, now, maxAgeMs)
        ) {
            return@withLock current.inventory
        }

        val inventory = FileInventoryScanner().collect(root)
        cached = CachedInventory(
            rootPath = rootPath,
            createdAt = now,
            inventory = inventory,
        )
        inventory
    }

    fun invalidate() {
        cached = null
    }

    internal fun peek(): FileInventory? = cached?.inventory

    private fun canonicalKey(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }

    private data class CachedInventory(
        val rootPath: String,
        val createdAt: Long,
        val inventory: FileInventory,
    )
}

internal fun isInventoryCacheFresh(
    createdAt: Long,
    now: Long,
    maxAgeMs: Long,
): Boolean {
    if (createdAt <= 0L || now < createdAt || maxAgeMs < 0L) return false
    return now - createdAt <= maxAgeMs
}
