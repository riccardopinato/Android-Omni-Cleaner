package com.riccardopinato.omnicleaner.parity

import com.riccardopinato.omnicleaner.cleaner.CleanupSelection
import com.riccardopinato.omnicleaner.cleaner.FileInventoryScanner
import com.riccardopinato.omnicleaner.cleaner.FileScanner
import com.riccardopinato.omnicleaner.cleaner.FolderStatus
import com.riccardopinato.omnicleaner.cleaner.SafeTrashManager
import com.riccardopinato.omnicleaner.cleaner.SpaceOverviewScanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class DonorParityFixtureTest {
    @Test
    fun deterministicStorageFixture_matchesRecoveredDonorContracts() = runBlocking {
        val root = createTempDirectory("omni-parity-").toFile()
        try {
            File(root, "empty").mkdirs()

            File(root, "small").apply { mkdirs() }
                .resolve("small.bin")
                .writeBytes(ByteArray(8) { 1 })

            File(root, "normal").apply { mkdirs() }
                .resolve("normal.bin")
                .writeBytes(ByteArray(32) { 2 })

            val duplicates = File(root, "duplicates").apply { mkdirs() }
            val oldest = File(duplicates, "a.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                assertTrue(setLastModified(1_000L))
            }
            File(duplicates, "b.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                assertTrue(setLastModified(2_000L))
            }
            File(duplicates, "same-size-different.bin").apply {
                writeBytes(byteArrayOf(4, 3, 2, 1))
                assertTrue(setLastModified(3_000L))
            }

            File(root, "large").apply { mkdirs() }
                .resolve("large.bin")
                .writeBytes(ByteArray(64) { 7 })

            File(root, ".hidden").apply { mkdirs() }
                .resolve("ignored.bin")
                .writeBytes(ByteArray(17) { 8 })

            File(root, "cache").apply { mkdirs() }
                .resolve("ignored.bin")
                .writeBytes(ByteArray(17) { 9 })

            val thumbnailDir = File(root, "DCIM/.thumbnails").apply { mkdirs() }
            File(thumbnailDir, "thumb.bin").writeBytes(ByteArray(5) { 5 })

            val inventoryScanner = FileInventoryScanner()
            val scanner = FileScanner(inventoryScanner = inventoryScanner)
            val inventory = inventoryScanner.collect(root)

            assertEquals(6, inventory.files.size)
            assertEquals(116L, inventory.files.sumOf { it.size })

            val overview = SpaceOverviewScanner(inventoryScanner).scan(root)
            assertEquals(6, overview.files)
            assertEquals(116L, overview.logicalBytes)
            assertEquals(64L, overview.largest.first().size)

            val folders = scanner.scanFolders(root, smallFolderThresholdBytes = 16L)
            val statusByName = folders.associate { it.name to it.status }
            assertEquals(FolderStatus.COMPLETELY_EMPTY, statusByName["empty"])
            assertEquals(FolderStatus.BELOW_THRESHOLD, statusByName["small"])
            assertEquals(FolderStatus.BELOW_THRESHOLD, statusByName["duplicates"])
            assertFalse("normal" in statusByName)
            assertFalse("large" in statusByName)
            assertFalse(".hidden" in statusByName)
            assertFalse("cache" in statusByName)

            val bigFiles = scanner.findBigFiles(root, minimumBytes = 64L)
            assertEquals(1, bigFiles.size)
            assertEquals("large.bin", bigFiles.single().name)

            val groups = scanner.findDuplicates(root)
            assertEquals(1, groups.size)
            assertEquals(2, groups.single().files.size)
            assertEquals(4L, groups.single().sizeBytes)
            assertEquals(4L, groups.single().totalWasteBytes)
            assertEquals(64, groups.single().hash.length)

            val protection = CleanupSelection.protectDuplicates(groups)
            assertEquals(setOf(oldest.canonicalFile), protection.keepers.map { it.canonicalFile }.toSet())

            val thumbnails = scanner.scanThumbnailCache(root)
            assertEquals(1, thumbnails.size)
            assertEquals(5L, thumbnails.single().sizeBytes)
            assertEquals(1, thumbnails.single().fileCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun safeTrashFixture_isReversibleAndConfined() {
        val root = createTempDirectory("omni-parity-trash-").toFile()
        val source = File(root, "source.bin").apply {
            writeBytes("donor-parity".toByteArray())
        }
        val trashRoot = File(root, "private-trash")
        val manager = SafeTrashManager.forTests(trashRoot)

        try {
            val payload = manager.moveToTrash(source)
            assertTrue(payload != null && payload.exists())
            assertFalse(source.exists())
            assertEquals("donor-parity".length.toLong(), payload!!.length())
            assertTrue(payload.canonicalPath.startsWith(trashRoot.canonicalPath + File.separator))
            assertTrue(manager.getTrashSize() >= payload.length())
            assertTrue(manager.emptyTrash())
            assertEquals(0L, manager.getTrashSize())
        } finally {
            root.deleteRecursively()
        }
    }
}
