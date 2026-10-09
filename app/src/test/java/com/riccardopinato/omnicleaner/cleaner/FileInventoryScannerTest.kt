package com.riccardopinato.omnicleaner.cleaner

import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileInventoryScannerTest {
    @Test
    fun duplicateScan_requiresIdenticalContentNotOnlyEqualSize() = runTest {
        val root = Files.createTempDirectory("omni-duplicates").toFile()
        try {
            val first = root.resolve("first.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val second = root.resolve("second.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            root.resolve("same-size-different.bin").writeBytes(byteArrayOf(9, 8, 7, 6))

            val inventory = FileInventoryScanner.collect(root)
            val groups = FileInventoryScanner.duplicates(inventory)

            assertEquals(1, groups.size)
            assertEquals(
                setOf(first.name, second.name),
                groups.single().files.map { it.name }.toSet(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun identical_detectsChangedContent() = runTest {
        val root = Files.createTempDirectory("omni-identical").toFile()
        try {
            val first = root.resolve("a").apply { writeText("same") }
            val second = root.resolve("b").apply { writeText("same") }
            assertTrue(FileInventoryScanner.identical(first, second))
            second.writeText("diff")
            assertFalse(FileInventoryScanner.identical(first, second))
        } finally {
            root.deleteRecursively()
        }
    }
}
