package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class StorageInventoryRepositoryTest {
    @Test
    fun reusesInventoryWithinTtlAndInvalidatesAfterCleanup() = runBlocking {
        val root = createTempDirectory("omni-inventory-cache-").toFile()
        File(root, "Download").mkdirs()
        File(root, "Download/a.txt").writeText("a")

        try {
            StorageInventoryRepository.invalidate()
            var now = 1_000L

            val first = StorageInventoryRepository.snapshot(root, nowMillis = { now })
            now += 500L
            val second = StorageInventoryRepository.snapshot(root, nowMillis = { now })

            assertFalse(first.cacheHit)
            assertTrue(second.cacheHit)
            assertTrue(second.inventory.files.size == 1)

            File(root, "Download/b.txt").writeText("b")
            StorageInventoryRepository.invalidate()
            now += 500L
            val refreshed = StorageInventoryRepository.snapshot(root, nowMillis = { now })

            assertFalse(refreshed.cacheHit)
            assertTrue(refreshed.inventory.files.size == 2)
        } finally {
            StorageInventoryRepository.invalidate()
            root.deleteRecursively()
        }
    }
    @Test
    fun staleGenerationCannotInstallInventory() {
        assertTrue(isInventoryGenerationCurrent(scanGeneration = 4L, currentGeneration = 4L))
        assertFalse(isInventoryGenerationCurrent(scanGeneration = 4L, currentGeneration = 5L))
    }
}

