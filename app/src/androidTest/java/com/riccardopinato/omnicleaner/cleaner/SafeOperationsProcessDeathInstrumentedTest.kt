package com.riccardopinato.omnicleaner.cleaner

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Methods MUST be invoked separately by the CI shell script with Android
 * force-stop and app relaunch in between. Synthetic app-private data only.
 */
@RunWith(AndroidJUnit4::class)
class SafeOperationsProcessDeathInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture get() = File(context.filesDir, "omni-synthetic-process-death")

    @Test
    fun createPendingMutatingFixture() {
        val root = fixture
        if (!root.exists()) assertTrue(root.mkdirs())
        val content = File(root, "valuable-user-like.apk")
        content.writeText("synthetic-original-byte-string")
        val journal = SafeOperationsJournal(File(root, "journal"))
        val stage = requireNotNull(journal.begin("LOCAL_SAFE_TRASH", "c".repeat(64), "QA_ONLY"))
        val approved = requireNotNull(journal.advance(stage, SafeOperationPhase.USER_APPROVED))
        requireNotNull(journal.advance(approved, SafeOperationPhase.MUTATING))
        assertEquals("synthetic-original-byte-string", content.readText())
        assertEquals(SafeOperationPhase.MUTATING, journal.needsReview().single().phase)
    }

    @Test
    fun assertReopenDoesNotReplayMutationAndPreservesOriginal() {
        val root = fixture
        val content = File(root, "valuable-user-like.apk")
        assertTrue("Source file must still exist after Android process death", content.isFile)
        assertEquals("synthetic-original-byte-string", content.readText())
        val journal = SafeOperationsJournal(File(root, "journal"))
        val pending = journal.needsReview()
        assertEquals(1, pending.size)
        assertEquals(SafeOperationPhase.MUTATING, pending.single().phase)
        assertFalse(File(root, "trash").exists())
        // Only this uniquely named app-private synthetic fixture is removed.
        assertTrue(root.deleteRecursively())
    }
}
