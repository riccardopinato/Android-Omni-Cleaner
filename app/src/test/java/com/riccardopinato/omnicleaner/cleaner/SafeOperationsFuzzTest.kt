package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic fuzz/property tests operate only on temporary synthetic files.
 * No permissions, no user media, no external storage and no production data.
 */
class SafeOperationsFuzzTest {
    private fun hash(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun reversibleMovesWithSameLengthTamperingPreserveAllSampleBytes() {
        val root = createTempDirectory("omni-fuzz-reversible-").toFile()
        try {
            val trash = SafeTrashManager.forTests(root.resolve("safe-trash"), root)
            val journal = SafeOperationsJournal(root.resolve("journal"))
            val coordinator = DeletionCoordinator(trash, journal)
            val random = Random(87241)
            repeat(48) { index ->
                val expected = ByteArray(random.nextInt(1, 1500)).also { random.nextBytes(it) }
                val target = root.resolve("sample-$index.apk").apply { writeBytes(expected) }
                val time = target.lastModified()
                val snapshot = CleanupFileSnapshot(
                    target, expected.size.toLong(), time, hash(expected),
                )
                val tampered = index % 3 == 0
                val contents = if (tampered) expected.copyOf().apply {
                    this[0] = (this[0].toInt() xor 0x7F).toByte()
                } else expected
                if (tampered) {
                    target.writeBytes(contents)
                    target.setLastModified(time)
                }
                val result = coordinator.moveApproved(listOf(snapshot))
                if (tampered) {
                    assertEquals(0, result.moved)
                    assertArrayEquals(contents, target.readBytes())
                } else {
                    assertEquals(1, result.moved)
                    assertFalse(target.exists())
                    val entry = trash.listEntries().single {
                        it.originalName == target.name
                    }
                    assertEquals(SafeTrashRestoreStatus.RESTORED, trash.restore(entry.id).status)
                    assertArrayEquals(expected, target.readBytes())
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun symlinkCandidateCannotMoveOrEscapeApprovedRoot() {
        val root = createTempDirectory("omni-symlink-").toFile()
        try {
            val allowed = root.resolve("allowed").apply { mkdirs() }
            val outside = root.resolve("outside.apk").apply { writeText("keep external") }
            val link = allowed.resolve("looks-safe.apk")
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            val before = outside.readBytes()
            val candidate = CleanupFileSnapshot(
                link, outside.length(), outside.lastModified(), hash(before),
            )
            val trash = SafeTrashManager.forTests(allowed.resolve("trash"), allowed)
            val moved = DeletionCoordinator(
                trash, SafeOperationsJournal(allowed.resolve("journal")),
            ).moveApproved(listOf(candidate))
            assertEquals(0, moved.moved)
            assertArrayEquals(before, outside.readBytes())
            assertTrue(Files.isSymbolicLink(link.toPath()))
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun concurrentBulkPurgeCanNeverIncludeLateDifferentFile() {
        val root = createTempDirectory("omni-purge-race-").toFile()
        try {
            val trash = SafeTrashManager.forTests(root.resolve("trash"), root)
            val first = root.resolve("first.apk").apply { writeText("first") }
            val later = root.resolve("later.apk").apply { writeText("later") }
            assertTrue(trash.moveToTrash(first) != null)
            val approved = requireNotNull(trash.planPurge(trash.listEntries()))
            val go = CountDownLatch(1)
            var purged: SafeTrashEmptyResult? = null
            val thread = Thread {
                go.await(10, TimeUnit.SECONDS)
                purged = trash.purgeApproved(approved)
            }
            thread.start()
            go.countDown()
            // Whether this happens before or after purge, the new entry was
            // never in the frozen manifest and must never be purged.
            trash.moveToTrash(later)
            thread.join(12000L)
            assertFalse(thread.isAlive)
            assertTrue(purged != null)
            assertTrue(trash.listEntries().any { it.originalName == later.name })
            assertTrue(trash.listEntries().none { it.originalName == "outside.apk" })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun unknownLedgerStatePreventsMutationEvenIfSelectionLooksValid() {
        val root = createTempDirectory("omni-unknown-ledger-").toFile()
        try {
            val journalDir = root.resolve("journal").apply { mkdirs() }
            journalDir.resolve(".interrupted-write.tmp").writeText("incomplete")
            val file = root.resolve("keep.apk").apply { writeText("KEEP") }
            val trash = SafeTrashManager.forTests(root.resolve("trash"), root)
            val snapshot = CleanupFileSnapshot(file, file.length(), file.lastModified())
            assertEquals(0, DeletionCoordinator(
                trash, SafeOperationsJournal(journalDir),
            ).moveApproved(listOf(snapshot)).moved)
            assertEquals("KEEP", file.readText())
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
