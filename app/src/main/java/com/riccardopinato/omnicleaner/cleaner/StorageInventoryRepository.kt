package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

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
    private val invalidationGuard = Any()
    private val scanner = FileInventoryScanner()

    @Volatile
    private var persistentIndex: PersistentStorageIndex? = null

    @Volatile
    private var backgroundIndexStore: BackgroundIndexStore? = null

    fun initialize(context: Context) {
        if (persistentIndex != null) return
        synchronized(this) {
            if (persistentIndex == null) {
                persistentIndex = PersistentStorageIndex(context.applicationContext)
                backgroundIndexStore = BackgroundIndexStore(context.applicationContext)
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
        onStage: ((String) -> Unit)? = null,
        onIndexReconcile: ((IndexReconcileDiagnostics) -> Unit)? = null,
        nowMillis: () -> Long = System::currentTimeMillis,
    ): StorageInventorySnapshot {
        val operationContext = coroutineContext
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
                onStage?.invoke("persistent_validate")
                val persisted = withContext(Dispatchers.IO) {
                    index.loadReusableSnapshot(
                        root = root,
                        nowMillis = lockedNow,
                        maxAgeMillis = PERSISTENT_REUSE_TTL_MS,
                        cancellationCheck = { operationContext.ensureActive() },
                    )
                }
                if (
                    persisted != null &&
                    isInventoryGenerationCurrent(scanGeneration, invalidationGeneration.get())
                ) {
                    val snapshot = StorageInventorySnapshot(
                        rootPath = rootPath,
                        inventory = persisted.inventory,
                        // The RAM-cache TTL starts when the persisted snapshot is
                        // hydrated, not when the underlying index was created.
                        // Otherwise a slow warm validation can make the freshly
                        // installed memory snapshot expire before this call ends.
                        createdAtMillis = lockedNow,
                        scanElapsedMillis = 0L,
                        cacheHit = true,
                        persistentHit = true,
                        indexAgeMillis = (lockedNow - persisted.createdAtMillis).coerceAtLeast(0L),
                    )
                    cached = snapshot
                    return@withLock snapshot
                }
            }

            onStage?.invoke("filesystem_scan")
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
                    onStage?.invoke("index_reconcile")
                    withContext(Dispatchers.IO) {
                        index.reconcileFull(
                            root = root,
                            inventory = inventory,
                            completedAtMillis = finished,
                            cancellationCheck = { operationContext.ensureActive() },
                            onMetrics = onIndexReconcile,
                        )
                    }
                }
                synchronized(invalidationGuard) {
                    if (isInventoryGenerationCurrent(scanGeneration, invalidationGeneration.get())) {
                        cached = snapshot
                    } else {
                        // An app cleanup raced the transaction; keep the old
                        // persistent generation non-reusable.
                        index?.markStale()
                        backgroundIndexStore?.markDirty()
                    }
                }
            }

            snapshot
        }
    }

    internal fun currentInvalidationGeneration(): Long = invalidationGeneration.get()

    /** All writers of a completed summary synchronize with invalidation. */
    internal fun publishCompletedIndexIfCurrent(
        root: File,
        inventory: FileInventory,
        expectedGeneration: Long,
        media: MediaRevisionToken?,
        requireBackgroundEnabled: Boolean = false,
    ): Boolean = synchronized(invalidationGuard) {
        val store = backgroundIndexStore ?: return@synchronized false
        if (expectedGeneration != invalidationGeneration.get() ||
            (requireBackgroundEnabled && !store.isEnabled())
        ) {
            store.markDirty()
            return@synchronized false
        }
        store.recordComplete(root, inventory, System.currentTimeMillis(), media)
        true
    }

    /** Read existing RAM/SQLite data, but never start a filesystem scan on cache miss. */
    suspend fun reusableSnapshotOrNull(root: File): StorageInventorySnapshot? {
        val operationContext = coroutineContext
        val rootPath = canonical(root)
        val now = System.currentTimeMillis()
        return mutex.withLock {
            cached?.takeIf { it.rootPath == rootPath && now >= it.createdAtMillis &&
                now - it.createdAtMillis <= CACHE_TTL_MS
            }?.let { return@withLock it.copy(cacheHit = true) }
            val generation = invalidationGeneration.get()
            val index = persistentIndex ?: return@withLock null
            val persisted = withContext(Dispatchers.IO) {
                index.loadReusableSnapshot(root, now, PERSISTENT_REUSE_TTL_MS) {
                    operationContext.ensureActive()
                }
            } ?: return@withLock null
            synchronized(invalidationGuard) {
                if (generation != invalidationGeneration.get() ||
                    backgroundIndexStore?.read()?.dirty == true
                ) {
                    null
                } else {
                    StorageInventorySnapshot(
                        rootPath = rootPath,
                        inventory = persisted.inventory,
                        createdAtMillis = now,
                        scanElapsedMillis = 0L,
                        cacheHit = true,
                        persistentHit = true,
                        indexAgeMillis = (now - persisted.createdAtMillis).coerceAtLeast(0L),
                    ).also { cached = it }
                }
            }
        }
    }

    fun invalidate() {
        synchronized(invalidationGuard) {
            invalidationGeneration.incrementAndGet()
            cached = null
            persistentIndex?.markStale()
            backgroundIndexStore?.markDirty()
        }
    }

    internal fun dropMemoryCacheForBenchmark() {
        cached = null
    }

    internal fun duplicateHashCache(): DuplicateHashCache? = persistentIndex?.newScanCache()

    internal fun currentForTests(): StorageInventorySnapshot? = cached

    private fun canonical(file: File): String =
        runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
}

internal fun isInventoryGenerationCurrent(
    scanGeneration: Long,
    currentGeneration: Long,
): Boolean = scanGeneration == currentGeneration
