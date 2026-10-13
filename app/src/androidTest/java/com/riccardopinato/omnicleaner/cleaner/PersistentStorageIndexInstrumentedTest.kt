package com.riccardopinato.omnicleaner.cleaner

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentStorageIndexInstrumentedTest {
    @Test
    fun persistedInventorySurvivesRepositoryObjectRestartAndInvalidatesChangedHash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "storage-index-test-${UUID.randomUUID()}.db"
        val root = File(context.cacheDir, "persistent-index-${UUID.randomUUID()}").apply { mkdirs() }
        val file = File(root, "sample.pdf").apply {
            writeText("version-one")
            setLastModified(1_000L)
        }

        try {
            val firstItem = IndexedFile(file, file.length(), file.lastModified())
            val firstInventory = FileInventory(
                files = listOf(firstItem),
                unreadable = 0,
                excludedDirectories = 0,
            )
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
            val changedItem = IndexedFile(file, file.length(), file.lastModified())
            restarted.reconcileFull(
                root = root,
                inventory = FileInventory(listOf(changedItem), 0, 0),
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
}
