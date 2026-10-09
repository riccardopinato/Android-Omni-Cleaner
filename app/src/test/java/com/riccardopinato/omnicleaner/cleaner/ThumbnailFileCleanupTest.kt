package com.riccardopinato.omnicleaner.cleaner

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailFileCleanupTest {
    @Test
    fun cleanup_rejectsFileThatChangedAfterInventory() = runTest {
        val root = Files.createTempDirectory("omni-thumbs").toFile()
        try {
            val file = root.resolve("thumb.jpg").apply { writeText("one") }
            val snapshot = ThumbnailFileCleanup.inventory(root)
            file.writeText("changed-after-scan")

            val result = ThumbnailFileCleanup.clean(snapshot)

            assertEquals(0, result.removed)
            assertEquals(1, result.rejected)
            assertTrue(file.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
