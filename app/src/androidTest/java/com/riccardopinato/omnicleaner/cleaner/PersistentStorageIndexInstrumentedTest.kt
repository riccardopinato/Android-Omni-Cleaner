package com.riccardopinato.omnicleaner.cleaner

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentStorageIndexInstrumentedTest {
    @Test
    fun persistedInventorySurvivesRepositoryObjectRestartAndInvalidatesChangedHash() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-test-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-${UUID.randomUUID()}").apply { mkdirs() }
        val file = File(root, "sample.pdf").apply {
            writeText("version-one")
            setLastModified(1_000L)
        }

        try {
            val firstInventory = FileInventoryScanner().collect(root)
            val firstItem = firstInventory.files.single()
            assertTrue(firstInventory.directories.isNotEmpty())
            val first = PersistentStorageIndex(context, databaseName)
            first.reconcileFull(root, firstInventory, completedAtMillis = 10_000L)
            first.putSampleHash(firstItem, "a".repeat(64))
            first.putFullHash(firstItem, "b".repeat(64))

            val restarted = PersistentStorageIndex(context, databaseName)
            val warm = restarted.loadReusableSnapshot(
                root = root,
                nowMillis = 11_000L,
                maxAgeMillis = 60_000L,
            )

            assertNotNull(warm)
            assertEquals(1, warm!!.inventory.files.size)
            assertEquals("a".repeat(64), restarted.sampleHash(firstItem))
            assertEquals("b".repeat(64), restarted.fullHash(firstItem))

            file.writeText("version-two-is-different")
            file.setLastModified(2_000L)
            val changedInventory = FileInventoryScanner().collect(root)
            val changedItem = changedInventory.files.single()
            restarted.reconcileFull(
                root = root,
                inventory = changedInventory,
                completedAtMillis = 20_000L,
            )

            assertNull(restarted.sampleHash(changedItem))
            assertNull(restarted.fullHash(changedItem))

            val refreshed = restarted.loadReusableSnapshot(
                root = root,
                nowMillis = 21_000L,
                maxAgeMillis = 60_000L,
            )
            assertNotNull(refreshed)
            assertEquals(changedItem.size, refreshed!!.inventory.files.single().size)

            restarted.markStale()
            assertNull(
                restarted.loadReusableSnapshot(
                    root = root,
                    nowMillis = 22_000L,
                    maxAgeMillis = 60_000L,
                ),
            )
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_$databaseName",
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }
    @Test
    fun directoryMutationRejectsWarmSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-dir-test-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-dir-${UUID.randomUUID()}").apply {
            mkdirs()
        }
        File(root, "first.txt").writeText("one")

        try {
            val index = PersistentStorageIndex(context, databaseName)
            val firstInventory = FileInventoryScanner().collect(root)
            index.reconcileFull(root, firstInventory, completedAtMillis = 10_000L)

            assertNotNull(
                index.loadReusableSnapshot(
                    root = root,
                    nowMillis = 11_000L,
                    maxAgeMillis = 60_000L,
                ),
            )

            File(root, "new-non-media.bin").writeText("new")
            // Ensure a visible directory revision change even on filesystems
            // with coarse timestamp behavior in instrumentation environments.
            root.setLastModified(System.currentTimeMillis() + 2_000L)

            assertNull(
                index.loadReusableSnapshot(
                    root = root,
                    nowMillis = 12_000L,
                    maxAgeMillis = 60_000L,
                ),
            )
        } finally {
            root.deleteRecursively()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(
                "storage_index_state_$databaseName",
                android.content.Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
    }

}
