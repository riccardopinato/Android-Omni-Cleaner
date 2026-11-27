package com.riccardopinato.omnicleaner.cleaner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Synthetic-only WorkManager/invalidation test. Never touches user files. */
@RunWith(AndroidJUnit4::class)
class BackgroundIndexRuntimeInstrumentedTest {
    @Test
    fun previouslyInvalidatedGenerationCannotPublishCompletedSummary() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        StorageInventoryRepository.initialize(app)
        val generation = StorageInventoryRepository.currentInvalidationGeneration()
        StorageInventoryRepository.invalidate()
        assertFalse(
            StorageInventoryRepository.publishCompletedIndexIfCurrent(
                root = StorageAccess.primaryRoot(),
                inventory = FileInventory(emptyList(), 0, 0),
                expectedGeneration = generation,
                media = null,
            ),
        )
        assertTrue(BackgroundIndexStore(app).read().dirty)
    }

    @Test
    fun disablingAutomaticIndexCancelsUniqueRequests() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val store = BackgroundIndexStore(app)
        val original = store.isEnabled()
        val manager = WorkManager.getInstance(app)
        try {
            BackgroundIndexScheduler.setEnabled(app, true)
            val scheduled = manager.getWorkInfosForUniqueWork("omni_index_periodic_v1")
                .get(20, TimeUnit.SECONDS)
            assertTrue("Periodic job must exist", scheduled.isNotEmpty())
            BackgroundIndexScheduler.setEnabled(app, false)
            val cancelled = manager.getWorkInfosForUniqueWork("omni_index_periodic_v1")
                .get(20, TimeUnit.SECONDS)
            assertTrue(
                "No running or enqueued work after opting out",
                cancelled.all { it.state.isFinished },
            )
        } finally {
            BackgroundIndexScheduler.setEnabled(app, original)
        }
    }
}
