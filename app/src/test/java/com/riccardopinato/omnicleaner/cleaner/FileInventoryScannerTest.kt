package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileInventoryScannerTest {
    @Test
    fun detectsByteIdenticalDuplicates() = runBlocking {
        val root = createTempDir(prefix = "omni-duplicates-")
        try {
            File(root, "a.bin").writeBytes(byteArrayOf(1, 2, 3, 4))
            File(root, "b.bin").writeBytes(byteArrayOf(1, 2, 3, 4))
            File(root, "different.bin").writeBytes(byteArrayOf(9, 9, 9, 9))

            val groups = FileInventoryScanner().duplicates(root)

            assertEquals(1, groups.size)
            assertEquals(2, groups.single().files.size)
            assertEquals(4L, groups.single().totalWasteBytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun inventoryCollectsFiles() = runBlocking {
        val root = createTempDir(prefix = "omni-inventory-")
        try {
            File(root, "hello.txt").writeText("hello")
            val inventory = FileInventoryScanner().collect(root)
            assertEquals(1, inventory.files.size)
            assertTrue(inventory.files.single().size > 0)
        } finally {
            root.deleteRecursively()
        }
    }
}
