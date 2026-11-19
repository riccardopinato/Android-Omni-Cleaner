package com.riccardopinato.omnicleaner.cleaner

import android.os.Build
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Destructive-operation regression on uniquely named synthetic shared-storage fixtures.
 * Never targets real user media, does not simulate MediaStore approval and does NOT
 * certify OEM physical behavior. All assertions are against actual file bytes.
 */
class OmniSafetyGateInstrumentedTest {
    @Test
    fun twoFiftyMebibyteDuplicatePairsKeepOneAndRestoreOrPurgeOnlySelectedCopy() = runBlocking {
        val root = syntheticSharedStorageRoot()
        try {
            val alpha = File(root, "alpha-keeper.apk").apply { writeSparseApk(this, 0x11) }
            val alphaCopy = File(root, "alpha-copy.apk")
            alpha.copyTo(alphaCopy)
            val beta = File(root, "beta-keeper.apk").apply { writeSparseApk(this, 0x44) }
            val betaCopy = File(root, "beta-copy.apk")
            beta.copyTo(betaCopy)
            val hashes = setOf(sha256(alpha), sha256(beta))
            assertEquals(2, hashes.size)
            assertEquals(FOUR_FILES, root.listFiles()?.count { it.isFile })

            val scanner = FileInventoryScanner()
            val inventory = scanner.collect(root)
            val groups = scanner.duplicates(inventory.files)
            assertEquals("Each content family must have a distinct verified duplicate group", 2, groups.size)
            assertTrue(groups.all { it.files.size == 2 })
            val trash = SafeTrashManager.forTests(File(root, "safe-trash"), root)
            val engine = StorageCleanupEngine(
                InstrumentationRegistry.getInstrumentation().targetContext, trash,
            )
            val selected = groups.flatMap { group -> group.files.map { it.path } }.toSet()
            val snapshots = engine.protectedDuplicateSnapshots(groups, selected)
            assertEquals("Every group must retain a keeper", 2, snapshots.size)
            val keepers = snapshots.map { requireNotNull(it.requiredDuplicatePeer).file }
            assertEquals(2, keepers.distinct().size)
            val preparation = engine.prepareSnapshots(snapshots)
            assertTrue(preparation.blockedFiles.isEmpty())
            assertTrue(preparation.staleFiles.isEmpty())
            assertEquals(2, preparation.safeTrashCandidates.size)

            val moved = engine.movePreparedSnapshotsToSafeTrash(preparation.safeTrashCandidates)
            assertEquals(2, moved.moved)
            assertEquals(0, moved.rejected)
            assertEquals(2L * APK_BYTES, moved.movedBytes)
            val entries = trash.inspect()
            assertTrue(entries.readable)
            assertEquals(2, entries.entries.size)
            assertEquals(2L * APK_BYTES, entries.totalPayloadBytes)
            assertTrue(keepers.all { it.exists() && sha256(it) in hashes })
            assertTrue(snapshots.all { !it.file.exists() })

            val first = entries.entries.first()
            val second = entries.entries.last()
            val restored = trash.restore(first.id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, restored.status)
            assertTrue(requireNotNull(restored.restoredFile).exists())
            assertEquals(first.sha256, sha256(restored.restoredFile))
            val purged = trash.purgeEntry(second.id)
            assertEquals(SafeTrashPurgeStatus.DELETED, purged.status)
            assertEquals(APK_BYTES, purged.reclaimedBytes)
            assertFalse(File(root, second.originalName).exists())
            assertTrue(keepers.all { it.exists() })
            assertEquals(hashes, keepers.map(::sha256).toSet())
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively() // only the random synthetic QA directory
        }
    }

    @Test
    fun changedKeeperWithSameSizeAndTimestampBlocksDuplicateDeletion() = runBlocking {
        val root = syntheticSharedStorageRoot()
        try {
            val first = File(root, "same-a.apk").apply { writeText("original-content") }
            val second = first.copyTo(File(root, "same-b.apk"))
            val scanner = FileInventoryScanner()
            val group = scanner.duplicates(scanner.collect(root).files).single()
            val trash = SafeTrashManager.forTests(File(root, "safe-trash"), root)
            val engine = StorageCleanupEngine(
                InstrumentationRegistry.getInstrumentation().targetContext, trash,
            )
            val snapshots = engine.protectedDuplicateSnapshots(
                listOf(group), group.files.map { it.path }.toSet(),
            )
            assertEquals(1, snapshots.size)
            val keeper = requireNotNull(snapshots.single().requiredDuplicatePeer).file
            val originalModified = keeper.lastModified()
            keeper.writeText("modified-content") // same 16-byte length
            assertEquals("Fixture must change bytes without changing size", 16L, keeper.length())
            assertTrue(keeper.setLastModified(originalModified))
            val prepared = engine.prepareSnapshots(snapshots)
            assertTrue(prepared.safeTrashCandidates.isEmpty())
            assertEquals(1, prepared.staleFiles.size)
            assertTrue(first.exists() && second.exists())
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun unknownTrashEntryIsNotSilentlyPurgedOrReportedAsEmpty() {
        val root = syntheticSharedStorageRoot()
        try {
            val manager = SafeTrashManager.forTests(File(root, "safe-trash"), root)
            val source = File(root, "safe.apk").apply { writeText("synthetic") }
            assertNotNull(manager.moveToTrash(source))
            val unknown = File(manager.getTrashDirectory(), "foreign-note.txt")
                .apply { writeText("must remain") }
            val purged = manager.purgeAll()
            assertFalse(purged.completed)
            assertEquals(1, purged.deletedEntries)
            assertEquals(1, purged.rejectedEntries)
            assertEquals(9L, purged.reclaimedBytes)
            assertTrue(unknown.exists())
            assertEquals("must remain", unknown.readText())
            assertEquals(1, manager.inspect().unrecognizedEntries)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun syntheticSharedStorageRoot(): File {
        // Do not silently alter device permissions from instrumentation.
        // The emulator workflow grants access to the debug test package only;
        // a physical-device operator must explicitly authorize this beforehand.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertTrue(
                "Authorize All files access for the DEBUG QA app before physical testing",
                Environment.isExternalStorageManager(),
            )
        }
        val root = File(
            StorageAccess.primaryRoot(), "OmniCleanerSafetyQA-" + UUID.randomUUID().toString(),
        )
        assertTrue("Synthetic shared-storage fixture must be writable", root.mkdirs())
        return root
    }

    private fun writeSparseApk(file: File, marker: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(APK_BYTES)
            raf.seek(0L)
            raf.write(marker)
            raf.seek(APK_BYTES / 2L)
            raf.write(marker xor 0x3A)
            raf.seek(APK_BYTES - 1)
            raf.write(marker xor 0x7F)
        }
    }

    private fun sha256(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val APK_BYTES = 50L * 1024L * 1024L
        const val FOUR_FILES = 4
    }
}
