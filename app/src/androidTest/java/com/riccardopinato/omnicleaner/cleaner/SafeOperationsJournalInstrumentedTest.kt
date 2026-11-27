package com.riccardopinato.omnicleaner.cleaner

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Synthetic-only, compatible with API 24–36. No user media is mutated. */
@RunWith(AndroidJUnit4::class)
class SafeOperationsJournalInstrumentedTest {
    private val hash = "b".repeat(64)

    @Test
    fun restartAtEveryOperationPhaseNeverReplaysMutation() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(ctx.filesDir, "journal-qa-${UUID.randomUUID()}")
        val userFixture = File(root, "valuable.txt")
        try {
            assertTrue(root.mkdirs())
            userFixture.writeText("do not change this")
            val journalDir = File(root, "journal")
            val initial = SafeOperationsJournal(journalDir)
                .begin("LOCAL_SAFE_TRASH", hash, "read-only test")
            assertNotNull(initial)
            val one = requireNotNull(initial)
            assertEquals(
                SafeOperationPhase.PREPARED,
                SafeOperationsJournal(journalDir).needsReview().single().phase,
            )
            val approved = requireNotNull(
                SafeOperationsJournal(journalDir).advance(
                    one, SafeOperationPhase.USER_APPROVED,
                ),
            )
            assertEquals(
                SafeOperationPhase.USER_APPROVED,
                SafeOperationsJournal(journalDir).needsReview().single().phase,
            )
            val mutating = requireNotNull(
                SafeOperationsJournal(journalDir).advance(
                    approved, SafeOperationPhase.MUTATING,
                ),
            )
            assertEquals(
                SafeOperationPhase.MUTATING,
                SafeOperationsJournal(journalDir).needsReview().single().phase,
            )
            assertEquals("do not change this", userFixture.readText())
            val committed = requireNotNull(
                SafeOperationsJournal(journalDir).advance(
                    mutating, SafeOperationPhase.COMMITTED,
                ),
            )
            assertEquals(SafeOperationPhase.COMMITTED, committed.phase)
            assertTrue(SafeOperationsJournal(journalDir).needsReview().isEmpty())
            assertEquals("do not change this", userFixture.readText())
        } finally {
            root.deleteRecursively() // only this random app-private fixture
        }
    }

    @Test
    fun permissionDeniedOrMissingMediaUriIsUnknownNotSuccess() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val absent = Uri.parse("content://media/external/images/media/999999999")
        assertFalse(MediaStoreHelper.isCurrentMediaMatch(
            ctx.contentResolver, absent, 4L, "unknown.jpg",
        ))
        assertTrue(MediaStoreHelper.verifiedTrashState(ctx.contentResolver, absent) != true)
    }
}
