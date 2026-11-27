package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory

class SafeOperationsCoreTest {
    private val sampleSha = "a".repeat(64)

    @Test
    fun journalStagesSurviveReinstantiationWithoutAutomaticReplay() {
        val root = createTempDirectory("omni-journal-restart-").toFile()
        try {
            val ledger = root.resolve("journal")
            val journal = SafeOperationsJournal(ledger)
            val prepared = requireNotNull(journal.begin("LOCAL_SAFE_TRASH", sampleSha, "source=test"))
            val approved = requireNotNull(journal.advance(prepared, SafeOperationPhase.USER_APPROVED))
            requireNotNull(journal.advance(approved, SafeOperationPhase.MUTATING))

            // New instance represents process death. It reads state only; no
            // callback or moved file is ever replayed on startup.
            val recovered = SafeOperationsJournal(ledger).needsReview()
            assertEquals(1, recovered.size)
            assertEquals(SafeOperationPhase.MUTATING, recovered.single().phase)
            assertEquals(prepared.id, recovered.single().id)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun corruptedOrInterruptedJournalBlocksAnyNewMutation() {
        val root = createTempDirectory("omni-corrupt-journal-").toFile()
        try {
            val ledger = root.resolve("journal").apply { mkdirs() }
            val source = root.resolve("valuable.apk").apply { writeText("unchanged") }
            ledger.resolve("interrupted-stage.tmp").writeText("partial data")
            val journal = SafeOperationsJournal(ledger)
            assertFalse(journal.integrityHealthy())
            assertNull(journal.begin("LOCAL_SAFE_TRASH", sampleSha))
            val coordinator = DeletionCoordinator(
                SafeTrashManager.forTests(root.resolve("trash"), root), journal,
            )
            val move = CleanupFileSnapshot(source, source.length(), source.lastModified())
            assertEquals(0, coordinator.moveApproved(listOf(move)).moved)
            assertTrue(source.isFile)
            assertEquals("unchanged", source.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun illegalJournalTransitionsCannotForgeCommitOrSkipApproval() {
        val root = createTempDirectory("omni-invalid-transition-").toFile()
        try {
            val journal = SafeOperationsJournal(root.resolve("journal"))
            val prepared = requireNotNull(journal.begin("LOCAL_SAFE_TRASH", sampleSha))
            assertNull(journal.advance(prepared, SafeOperationPhase.COMMITTED))
            assertNull(journal.advance(prepared, SafeOperationPhase.MUTATING))
            val approved = requireNotNull(journal.advance(prepared, SafeOperationPhase.USER_APPROVED))
            assertNull(journal.advance(approved, SafeOperationPhase.COMMITTED))
            val pending = requireNotNull(journal.advance(approved, SafeOperationPhase.MUTATING))
            assertNull(journal.advance(pending, SafeOperationPhase.USER_APPROVED))
            val unknown = requireNotNull(
                journal.advance(pending, SafeOperationPhase.UNKNOWN_NEEDS_REVIEW),
            )
            assertNull(journal.advance(unknown, SafeOperationPhase.MUTATING))
            assertNotNull(journal.advance(unknown, SafeOperationPhase.COMMITTED))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun journalRejectsTamperAndDuplicateTransition() {
        val root = createTempDirectory("omni-journal-tamper-").toFile()
        try {
            val journal = SafeOperationsJournal(root.resolve("journal"))
            assertNull(journal.begin("LOCAL", "not a sha256"))
            val initial = requireNotNull(journal.begin("LOCAL", sampleSha))
            val approved = requireNotNull(journal.advance(initial, SafeOperationPhase.USER_APPROVED))
            assertNull(journal.advance(initial, SafeOperationPhase.USER_APPROVED))
            val record = root.resolve("journal").resolve("${approved.id}-${approved.sequence}.record")
            record.appendText("checksum=malicious\n")
            // Corrupt latest events are not accepted as a valid next state.
            assertNull(journal.advance(approved, SafeOperationPhase.MUTATING))
            assertNotNull(journal.latest(initial.id))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun coordinatorRejectsSelectionOfDuplicateKeeper() {
        val root = createTempDirectory("omni-coordinator-keeper-").toFile()
        try {
            val keeper = root.resolve("keeper.apk").apply { writeText("same") }
            val candidate = root.resolve("candidate.apk").apply { writeText("same") }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest("same".toByteArray())
                .joinToString("") { "%02x".format(it) }
            val duplicate = CleanupFileSnapshot(
                file = candidate,
                expectedBytes = candidate.length(),
                expectedModified = candidate.lastModified(),
                expectedSha256 = hash,
                kind = CleanupCandidateKind.DUPLICATE,
                requiredDuplicatePeer = CleanupPeerSnapshot(
                    keeper, keeper.length(), keeper.lastModified(), hash,
                ),
            )
            val selectedKeeper = CleanupFileSnapshot(
                keeper, keeper.length(), keeper.lastModified(),
                kind = CleanupCandidateKind.LARGE_FILE,
            )
            val trash = SafeTrashManager.forTests(root.resolve("trash"), root)
            val coordinator = DeletionCoordinator(
                trash, SafeOperationsJournal(root.resolve("journal")),
            )
            val result = coordinator.moveApproved(listOf(duplicate, selectedKeeper))
            assertEquals(0, result.moved)
            assertEquals(2, result.rejected)
            assertTrue(candidate.isFile)
            assertTrue(keeper.isFile)
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun hardLinkedDuplicateKeeperCannotBeLostThroughDifferentPath() {
        val root = createTempDirectory("omni-hardlink-keeper-").toFile()
        try {
            val keeper = root.resolve("keeper.apk").apply { writeText("same bytes") }
            val hardLink = root.resolve("keeper-alias.apk")
            java.nio.file.Files.createLink(hardLink.toPath(), keeper.toPath())
            val removable = root.resolve("removable.apk").apply { writeText("same bytes") }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(keeper.readBytes()).joinToString("") { "%02x".format(it) }
            val peer = CleanupPeerSnapshot(keeper, keeper.length(),
                keeper.lastModified(), hash)
            val candidate = CleanupFileSnapshot(removable, removable.length(),
                removable.lastModified(), hash, CleanupCandidateKind.DUPLICATE, peer)
            val aliasSelection = CleanupFileSnapshot(
                hardLink, hardLink.length(), hardLink.lastModified(),
                kind = CleanupCandidateKind.LARGE_FILE,
            )
            val trash = SafeTrashManager.forTests(root.resolve("trash"), root)
            val result = DeletionCoordinator(
                trash, SafeOperationsJournal(root.resolve("journal")),
            ).moveApproved(listOf(candidate, aliasSelection))
            assertEquals(0, result.moved)
            assertTrue(keeper.isFile)
            assertTrue(hardLink.isFile)
            assertTrue(removable.isFile)
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun coordinatorMovesOnlyVerifiedSelectionAndRecordsFinalStatus() {
        val root = createTempDirectory("omni-coordinator-success-").toFile()
        try {
            val source = root.resolve("invoice.apk").apply { writeText("preserve") }
            val trash = SafeTrashManager.forTests(root.resolve("trash"), root)
            val ledger = SafeOperationsJournal(root.resolve("journal"))
            val snapshot = CleanupFileSnapshot(
                source, source.length(), source.lastModified(),
            )
            val moved = DeletionCoordinator(trash, ledger).moveApproved(listOf(snapshot))
            assertEquals(1, moved.moved)
            assertFalse(source.exists())
            assertEquals(1, trash.listEntries().size)
            assertTrue(ledger.needsReview().isEmpty())
            assertEquals(
                SafeTrashRestoreStatus.RESTORED,
                trash.restore(trash.listEntries().single().id).status,
            )
            assertEquals("preserve", source.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun staleSameSizeChangedBytesCannotBeMovedFromApprovedSnapshot() {
        val root = createTempDirectory("omni-stale-same-size-").toFile()
        try {
            val source = root.resolve("data.apk").apply { writeText("first") }
            val sha = MessageDigest.getInstance("SHA-256")
                .digest(source.readBytes()).joinToString("") { "%02x".format(it) }
            val before = CleanupFileSnapshot(
                source, source.length(), source.lastModified(), sha,
            )
            source.writeText("other")
            source.setLastModified(before.expectedModified)
            val coordinator = DeletionCoordinator(
                SafeTrashManager.forTests(root.resolve("trash"), root),
                SafeOperationsJournal(root.resolve("journal")),
            )
            assertEquals(0, coordinator.moveApproved(listOf(before)).moved)
            assertEquals("other", source.readText())
        } finally {
            root.deleteRecursively()
        }
    }
}
