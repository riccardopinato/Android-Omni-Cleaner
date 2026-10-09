package com.riccardopinato.omnicleaner.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryFeatureTest {
    @Test
    fun stableIds_areUnique() {
        val ids = RecoveryFeature.entries.map { it.stableId }

        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun baseline_containsExpectedRecoveredAreas() {
        val ids = RecoveryFeature.entries.map { it.stableId }.toSet()

        assertTrue("storage_overview" in ids)
        assertTrue("duplicates" in ids)
        assertTrue("gallery_cleaner" in ids)
        assertTrue("exif_privacy" in ids)
    }
}
