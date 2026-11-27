package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.riccardopinato.omnicleaner.ui.BentoScanRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

// The one-shot launch check and periodic work may overlap. Only one complete
// decision + reconciliation + publish cycle may execute in this process.
private val indexWorkerMutex = Mutex()

/**
 * Non-expedited, read-only work. No foreground service is started by background
 * work. On cancellation no incomplete summary can be published.
 */
internal class BackgroundIndexWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = indexWorkerMutex.withLock {
        runIndexOnce()
    }

    private suspend fun runIndexOnce(): Result {
        val app = applicationContext
        val store = BackgroundIndexStore(app)
        if (!store.isEnabled()) return Result.success()
        if (!StorageAccess.capabilityHealth(app).deepScanAvailable) return Result.success()
        if (BentoScanRuntime.activeId != null ||
            StorageInventoryRepository.hasActiveUserScan() ||
            StorageInventoryRepository.isInventoryBusy()
        ) return Result.retry()

        val root = StorageAccess.primaryRoot()
        if (!root.isDirectory || !root.canRead()) return Result.retry()
        val before = BackgroundIndexStore.mediaRevision(app)
        val now = System.currentTimeMillis()
        if (!shouldRebuildIndex(store.read(), root.absolutePath, before, now)) {
            store.recordChecked(now)
            return Result.success()
        }

        return try {
            StorageInventoryRepository.initialize(app)
            val generation = StorageInventoryRepository.currentInvalidationGeneration()
            val inventory = StorageInventoryRepository.snapshot(
                root = root,
                forceRefresh = true,
                backgroundRequest = true,
            ).inventory
            coroutineContext.ensureActive()
            val after = BackgroundIndexStore.mediaRevision(app)
            // Cleanup or MediaStore updates during traversal invalidate the result.
            if (generation != StorageInventoryRepository.currentInvalidationGeneration() ||
                (before != null && after != null && before != after)
            ) {
                store.markDirty()
                Result.retry()
            } else if (StorageInventoryRepository.publishCompletedIndexIfCurrent(
                    root = root,
                    inventory = inventory,
                    expectedGeneration = generation,
                    media = after,
                    requireBackgroundEnabled = true,
                )
            ) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            Result.success() // Revoked permission is not a permanent worker failure.
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

internal object BackgroundIndexScheduler {
    private const val PERIODIC = "omni_index_periodic_v1"
    private const val ON_OPEN = "omni_index_quick_check_v1"
    private const val HOURS = 6L

    private fun constraints() = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

    fun sync(context: Context) {
        val app = context.applicationContext
        val manager = WorkManager.getInstance(app)
        if (!BackgroundIndexStore(app).isEnabled()) {
            manager.cancelUniqueWork(PERIODIC)
            manager.cancelUniqueWork(ON_OPEN)
            return
        }
        val periodic = PeriodicWorkRequestBuilder<BackgroundIndexWorker>(HOURS, TimeUnit.HOURS)
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)
    }

    fun onAppOpened(context: Context) {
        val app = context.applicationContext
        sync(app)
        val store = BackgroundIndexStore(app)
        val now = System.currentTimeMillis()
        if (!store.isEnabled() || !store.shouldEnqueueCheck(now)) return
        val oneTime = OneTimeWorkRequestBuilder<BackgroundIndexWorker>()
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(ON_OPEN, ExistingWorkPolicy.KEEP, oneTime)
        store.markCheckEnqueued(now)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        BackgroundIndexStore(context).setEnabled(enabled)
        sync(context)
        if (enabled) onAppOpened(context)
    }
}
