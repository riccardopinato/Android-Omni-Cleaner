package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundIndexPolicyTest {
    private val now = 1_800_000_000_000L
    private val token = MediaRevisionToken("version-A", 15L)
    private val saved = SavedIndexSummary(
        rootPath = "/storage/emulated/0",
        completedAtMillis = now - 60_000L,
        mediaVersion = token.version,
        mediaGeneration = token.generation,
    )

    @Test fun unchangedMediaSkipsFullTraversal() {
        assertFalse(shouldRebuildIndex(saved, saved.rootPath, token, now))
    }

    @Test fun generationAndVersionChangesInvalidate() {
        assertTrue(shouldRebuildIndex(saved, saved.rootPath, token.copy(generation = 16L), now))
        assertTrue(shouldRebuildIndex(saved, saved.rootPath, token.copy(version = "version-B"), now))
    }

    @Test fun missingIndexOrChangedRootRequiresBuild() {
        assertTrue(shouldRebuildIndex(SavedIndexSummary(), saved.rootPath, token, now))
        assertTrue(shouldRebuildIndex(saved, "/different", token, now))
    }

    @Test fun explicitInvalidationAndFullAuditAreHonored() {
        assertTrue(shouldRebuildIndex(saved.copy(dirty = true), saved.rootPath, token, now))
        assertTrue(shouldRebuildIndex(
            saved.copy(completedAtMillis = now - INDEX_FULL_AUDIT_MS),
            saved.rootPath, token, now,
        ))
    }

    @Test fun nonMediaChangesGetDailySafetyAudit() {
        assertFalse(shouldRebuildIndex(saved, saved.rootPath, null, now))
        assertTrue(shouldRebuildIndex(
            saved.copy(completedAtMillis = now - INDEX_FULL_AUDIT_MS),
            saved.rootPath, null, now,
        ))
    }

    @Test fun clockRollbackInvalidates() {
        assertTrue(shouldRebuildIndex(saved.copy(completedAtMillis = now + 1), saved.rootPath, token, now))
    }
}
