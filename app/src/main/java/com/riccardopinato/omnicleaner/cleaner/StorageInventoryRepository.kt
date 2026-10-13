package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

data class StorageInventorySnapshot(
    val rootPath: String,
    val inventory: FileInventory,
    val createdAtMillis: Long,
    val scanElapsedMillis: Long,
    val cacheHit: Boolean,
    val persistentHit: Boolean = false,
    val indexAgeMillis: Long = 0L,
)

/**
 * Shared in-process inventory for storage intelligence.
 *
 * Destructive operations still revalidate each selected file immediately before
 * mutation; this cache only avoids repeated read-only filesystem traversals.
 */
object StorageInventoryRepository {
    const val CACHE_TTL_MS: Long = 120_000L
    const val PERSISTENT_REUSE_TTL_MS: Long = 6L * 60L * 60L * 1000L

    private val mutex = Mutex()
    private val scanner = FileInventoryScanner()

    @Volatile
    private var persistentIndex: PersistentStorageIndex? = null

    fun initialize(context: Context) {
        if (persistentIndex != null) return
        synchronized(this) {
            if (persistentIndex == null) {
                persistentIndex = PersistentStorageIndex(context.applicationContext)
            }
        }
    }

    @Volatile
    private var cached: StorageInventorySnapshot? = null

    private val invalidationGeneration = AtomicLong(0L)

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

            val scanGeneration = invalidationGeneration.get()
            val index = persistentIndex

            if (!forceRefresh && index != null) {
                val persisted = withContext(Dispatchers.IO) {
                    index.loadReusableSnapshot(
                        root = root,
                        nowMillis = lockedNow,
                        maxAgeMillis = PERSISTENT_REUSE_TTL_MS,
                    )
                }
                if (persisted != null) {
                    val snapshot = StorageInventorySnapshot(
                        rootPath = rootPath,
                        inventory = persisted.inventory,
                        createdAtMillis = persisted.createdAtMillis,
                        scanElapsedMillis = 0L,
                        cacheHit = true,
                        persistentHit = true,
                        indexAgeMillis = (lockedNow - persisted.createdAtMillis).coerceAtLeast(0L),
                    )
                    if (isInventoryGenerationCurrent(scanGeneration, invalidationGeneration.get())) {
                        cached = snapshot
                    }
                    return@withLock snapshot
                }
            }

            val started = nowMillis()
            val inventory = scanner.collect(root, onProgress)
            val finished = nowMillis()
            val snapshot = StorageInventorySnapshot(
                rootPath = rootPath,
                inventory = inventory,
                createdAtMillis = finished,
                scanElapsedMillis = (finished - started).coerceAtLeast(0L),
                cacheHit = false,
                persistentHit = false,
                indexAgeMillis = 0L,
            )

            if (isInventoryGenerationCurrent(scanGeneration, invalidationGeneration.get())) {
                if (index != null) {
                    withContext(Dispatchers.IO) {
                        index.reconcileFull(
                            root = root,
                            inventory = inventory,
                            completedAtMillis = finished,
                        )
                    }
                }
                cached = snapshot
            }

            snapshot
        }
    }

    fun invalidate() {
        invalidationGeneration.incrementAndGet()
        cached = null
        persistentIndex?.markStale()
    }

    internal fun duplicateHashCache(): DuplicateHashCache? = persistentIndex

    internal fun currentForTests(): StorageInventorySnapshot? = cached

    private fun canonical(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
}

internal fun isInventoryGenerationCurrent(
    scanGeneration: Long,
    currentGeneration: Long,
): Boolean = scanGeneration == currentGeneration
