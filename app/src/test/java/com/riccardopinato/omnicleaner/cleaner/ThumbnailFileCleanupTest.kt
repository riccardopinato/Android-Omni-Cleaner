package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class ThumbnailFileCleanupTest {
    @Test
    fun deletesOnlyUnchangedInventoryEntries() = runBlocking {
        val root = createTempDirectory("omni-thumbnails-").toFile()
        try {
            val stable = root.resolve("stable.thumb").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val changed = root.resolve("changed.thumb").apply { writeBytes(byteArrayOf(4, 5, 6)) }
            val cleaner = ThumbnailFileCleanup()
            val snapshot = cleaner.inventory(root)

            changed.writeBytes(byteArrayOf(9, 9, 9, 9))

            val result = cleaner.clean(snapshot)

            assertEquals(1, result.removed)
            assertEquals(1, result.rejected)
            assertTrue(!stable.exists())
            assertTrue(changed.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
