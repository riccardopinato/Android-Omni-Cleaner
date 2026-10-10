package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CleanupSnapshotValidatorTest {
    @Test
    fun duplicateSnapshotWinsWhenSamePathAlsoAppearsAsLargeFile() {
        val root = createTempDirectory("omni-strongest-").toFile()
        try {
            val file = File(root, "same.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val peer = File(root, "peer.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val hash = FileInventoryScanner().digest(file)

            val large = CleanupFileSnapshot(
                file = file,
                expectedBytes = file.length(),
                expectedModified = file.lastModified(),
                kind = CleanupCandidateKind.LARGE_FILE,
            )
            val duplicate = CleanupFileSnapshot(
                file = file,
                expectedBytes = file.length(),
                expectedModified = file.lastModified(),
                expectedSha256 = hash,
                kind = CleanupCandidateKind.DUPLICATE,
                requiredDuplicatePeer = CleanupPeerSnapshot(
                    file = peer,
                    expectedBytes = peer.length(),
                    expectedModified = peer.lastModified(),
                    expectedSha256 = hash,
                ),
            )

            val merged = strongestCleanupSnapshots(listOf(large, duplicate))
            assertEquals(1, merged.size)
            assertEquals(CleanupCandidateKind.DUPLICATE, merged.single().kind)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsFileChangedAfterScan() {
        val root = createTempDirectory("omni-stale-").toFile()
        try {
            val file = File(root, "candidate.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                setLastModified(10_000L)
            }
            val snapshot = CleanupFileSnapshot(
                file = file,
                expectedBytes = file.length(),
                expectedModified = file.lastModified(),
                kind = CleanupCandidateKind.LARGE_FILE,
            )

            file.writeBytes(byteArrayOf(9, 9, 9, 9, 9))

            val validation = CleanupSnapshotValidator.validate(snapshot)
            assertEquals(CleanupSnapshotRejection.CHANGED_SINCE_SCAN, validation.rejection)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateRequiresUnchangedVerifiedKeeper() {
        val root = createTempDirectory("omni-duplicate-peer-").toFile()
        try {
            val keeper = File(root, "keeper.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                setLastModified(20_000L)
            }
            val selected = File(root, "selected.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                setLastModified(30_000L)
            }
            val hash = FileInventoryScanner().digest(selected)
            val snapshot = CleanupFileSnapshot(
                file = selected,
                expectedBytes = selected.length(),
                expectedModified = selected.lastModified(),
                expectedSha256 = hash,
                kind = CleanupCandidateKind.DUPLICATE,
                requiredDuplicatePeer = CleanupPeerSnapshot(
                    file = keeper,
                    expectedBytes = keeper.length(),
                    expectedModified = keeper.lastModified(),
                    expectedSha256 = hash,
                ),
            )

            assertTrue(CleanupSnapshotValidator.validate(snapshot).valid)

            keeper.writeBytes(byteArrayOf(4, 3, 2, 1))
            keeper.setLastModified(40_000L)

            val validation = CleanupSnapshotValidator.validate(snapshot)
            assertEquals(CleanupSnapshotRejection.DUPLICATE_KEEPER_INVALID, validation.rejection)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun digestCatchesSameSizeChangeWithPreservedTimestamp() {
        val root = createTempDirectory("omni-digest-stale-").toFile()
        try {
            val file = File(root, "duplicate.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                setLastModified(50_000L)
            }
            val originalModified = file.lastModified()
            val originalHash = FileInventoryScanner().digest(file)
            val peer = File(root, "keeper.bin").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
                setLastModified(60_000L)
            }
            val snapshot = CleanupFileSnapshot(
                file = file,
                expectedBytes = 4L,
                expectedModified = originalModified,
                expectedSha256 = originalHash,
                kind = CleanupCandidateKind.DUPLICATE,
                requiredDuplicatePeer = CleanupPeerSnapshot(
                    file = peer,
                    expectedBytes = 4L,
                    expectedModified = peer.lastModified(),
                    expectedSha256 = originalHash,
                ),
            )

            file.writeBytes(byteArrayOf(4, 3, 2, 1))
            file.setLastModified(originalModified)

            val validation = CleanupSnapshotValidator.validate(snapshot)
            assertEquals(CleanupSnapshotRejection.DIGEST_MISMATCH, validation.rejection)
        } finally {
            root.deleteRecursively()
        }
    }
}
