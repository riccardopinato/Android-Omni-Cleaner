package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProScanModelsTest {
    @Test
    fun donorBatchTypesArePreserved() {
        assertEquals(
            setOf("EMPTY_FOLDERS", "DUPLICATES", "LARGE_FILES", "THUMBNAIL_CACHE", "APP_RESIDUES"),
            ProScanType.entries.mapTo(linkedSetOf()) { it.name },
        )
    }

    @Test
    fun donorPremiumProductIdIsPreserved() {
        assertEquals("premium_unlock", BillingManager.PREMIUM_PRODUCT_ID)
    }

    @Test
    fun defaultSelectionIncludesEveryRecoveredType() {
        assertTrue(ProScanSelection().selectedTypes.containsAll(ProScanType.entries))
    }
}
