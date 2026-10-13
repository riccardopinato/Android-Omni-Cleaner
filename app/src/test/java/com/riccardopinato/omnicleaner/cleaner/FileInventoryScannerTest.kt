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
    fun duplicateScanCanBeRestrictedByExtensionBeforeHashing() = runBlocking {
        val root = createTempDirectory("omni-extension-duplicates-").toFile()
        try {
            File(root, "one.pdf").writeText("same-pdf")
            File(root, "two.pdf").writeText("same-pdf")
            File(root, "one.apk").writeText("same-apk")
            File(root, "two.apk").writeText("same-apk")

            val inventory = FileInventoryScanner().collect(root)
            val scanner = FileScanner()

            val pdfGroups = scanner.findDuplicates(
                inventory = inventory,
                extensionFilter = "pdf",
            )
            val apkGroups = scanner.findDuplicates(
                inventory = inventory,
                extensionFilter = ".apk",
            )

            assertEquals(1, pdfGroups.size)
            assertEquals(setOf("one.pdf", "two.pdf"), pdfGroups.single().files.map { it.name }.toSet())

            assertEquals(1, apkGroups.size)
            assertEquals(setOf("one.apk", "two.apk"), apkGroups.single().files.map { it.name }.toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateScanProgressStartsAtZeroAndEndsAtTotal() = runBlocking {
        val root = createTempDirectory("omni-duplicate-progress-boundary-").toFile()
        try {
            repeat(20) { index ->
                File(root, "copy-$index.bin").writeText("identical")
            }

            val inventory = FileInventoryScanner().collect(root)
            val progress = mutableListOf<Pair<Int, Int>>()

            val groups = FileInventoryScanner().duplicatesProgressive(inventory.files) { processed, total ->
                progress += processed to total
            }

            assertEquals(1, groups.size)
            assertTrue(progress.isNotEmpty())
            assertEquals(0, progress.first().first)
            assertEquals(progress.first().second, progress.last().second)
            assertEquals(progress.last().second, progress.last().first)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun bigFileFilterRunsBeforeResultLimit() = runBlocking {
        val root = createTempDirectory("omni-big-extension-").toFile()
        try {
            repeat(5) { index ->
                File(root, "video-$index.mp4").writeBytes(ByteArray(64 + index))
            }
            File(root, "package.apk").writeBytes(ByteArray(32))

            val inventory = FileInventoryScanner().collect(root)
            val results = FileScanner().findBigFiles(
                inventory = inventory,
                minimumBytes = 1L,
                limit = 1,
                extensionFilter = "apk",
            )

            assertEquals(1, results.size)
            assertEquals("package.apk", results.single().name)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun boundedHashWorkerPolicyCapsConcurrency() {
        assertEquals(1, boundedHashWorkerCount(1))
        assertEquals(2, boundedHashWorkerCount(2))
        assertEquals(2, boundedHashWorkerCount(4))
        assertEquals(3, boundedHashWorkerCount(6))
        assertEquals(4, boundedHashWorkerCount(8))
        assertEquals(4, boundedHashWorkerCount(32))
    }

    @Test
    fun duplicateScanReusesRevisionBoundHashes() = runBlocking {
        val root = createTempDirectory("omni-hash-cache-").toFile()
        try {
            val first = File(root, "first.bin").apply { writeText("same-content") }
            val second = File(root, "second.bin").apply { writeText("same-content") }
            val scanner = FileInventoryScanner()
            val inventory = scanner.collect(root)
            val digest = scanner.digest(first)

            class FakeCache : DuplicateHashCache {
                var sampleGets = 0
                var fullGets = 0
                var samplePuts = 0
                var fullPuts = 0

                override fun sampleHash(item: IndexedFile): String? {
                    sampleGets++
                    return digest
                }

                override fun fullHash(item: IndexedFile): String? {
                    fullGets++
                    return digest
                }

                override fun putSampleHash(item: IndexedFile, hash: String) {
                    samplePuts++
                }

                override fun putFullHash(item: IndexedFile, hash: String) {
                    fullPuts++
                }
            }

            val cache = FakeCache()
            val groups = scanner.duplicatesProgressive(
                files = inventory.files,
                hashCache = cache,
                parallelism = 2,
            )

            assertEquals(1, groups.size)
            assertEquals(2, groups.single().files.size)
            assertTrue(cache.sampleGets >= 2)
            assertTrue(cache.fullGets >= 2)
            assertEquals(0, cache.samplePuts)
            assertEquals(0, cache.fullPuts)
            assertTrue(first.isFile)
            assertTrue(second.isFile)
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
