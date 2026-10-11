package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileInventoryScannerTest {
    @Test
    fun detectsByteIdenticalDuplicates() = runBlocking {
        val root = createTempDirectory("omni-duplicates-").toFile()
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
    fun sampledFingerprintCollisionStillRequiresFullIdentity() = runBlocking {
        val root = createTempDirectory("omni-sampled-collision-").toFile()
        try {
            val edge = ByteArray(64 * 1024) { index -> (index % 251).toByte() }
            val middleA = ByteArray(32 * 1024) { 0x11 }
            val middleB = ByteArray(32 * 1024) { 0x22 }

            val identicalBytes = edge + middleA + edge
            File(root, "a.bin").writeBytes(identicalBytes)
            File(root, "b.bin").writeBytes(identicalBytes)
            File(root, "same-sample-different-middle.bin").writeBytes(edge + middleB + edge)

            val groups = FileInventoryScanner().duplicates(root)

            assertEquals(1, groups.size)
            assertEquals(
                setOf("a.bin", "b.bin"),
                groups.single().files.map { it.name }.toSet(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun nullableUnlessCancelledRethrowsCancellation() = runBlocking {
        var propagated = false
        try {
            nullableUnlessCancelled<Unit> {
                throw CancellationException("cancel")
            }
        } catch (_: CancellationException) {
            propagated = true
        }

        assertTrue(propagated)
    }

    @Test
    fun nullableUnlessCancelledContainsOrdinaryIoFailure() = runBlocking {
        val value = nullableUnlessCancelled<String> {
            error("io-like failure")
        }

        assertEquals(null, value)
    }

    @Test
    fun inventoryCollectsFiles() = runBlocking {
        val root = createTempDirectory("omni-inventory-").toFile()
        try {
            File(root, "hello.txt").writeText("hello")
            val inventory = FileInventoryScanner().collect(root)
            assertEquals(1, inventory.files.size)
            assertTrue(inventory.files.single().size > 0)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun byteVerificationRejectsSameSizeDifferentContents() = runBlocking {
        val root = createTempDirectory("omni-identical-").toFile()
        try {
            val first = File(root, "first.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
            }
            val identical = File(root, "identical.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
            }
            val different = File(root, "different.bin").apply {
                writeBytes(byteArrayOf(4, 3, 2, 1))
            }

            val scanner = FileInventoryScanner()
            assertTrue(scanner.identical(first, identical))
            assertFalse(scanner.identical(first, different))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun unchangedRejectsFileMutatedAfterInventorySnapshot() = runBlocking {
        val root = createTempDirectory("omni-unchanged-").toFile()
        try {
            val file = File(root, "candidate.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
            }
            val scanner = FileInventoryScanner()
            val snapshot = scanner.collect(root).files.single()

            assertTrue(scanner.unchanged(snapshot))

            file.writeBytes(byteArrayOf(9, 9, 9, 9, 9))
            assertFalse(scanner.unchanged(snapshot))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun progressiveDuplicateScanReportsCandidateProgress() = runBlocking {
        val root = createTempDirectory("omni-progressive-").toFile()
        try {
            File(root, "a.bin").writeBytes(byteArrayOf(1, 2, 3, 4))
            File(root, "b.bin").writeBytes(byteArrayOf(1, 2, 3, 4))
            File(root, "c.bin").writeBytes(byteArrayOf(4, 3, 2, 1))
            File(root, "unique.bin").writeBytes(byteArrayOf(1, 2, 3))

            val progress = mutableListOf<Pair<Int, Int>>()
            val groups = FileInventoryScanner().duplicatesProgressive(root) { processed, total ->
                progress += processed to total
            }

            assertEquals(1, groups.size)
            assertTrue(progress.isNotEmpty())
            assertEquals(progress.last().second, progress.last().first)
            assertEquals(3, progress.last().second)
        } finally {
            root.deleteRecursively()
        }
    }
}
