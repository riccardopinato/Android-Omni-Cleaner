package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedInventoryPolicyTest {
    @Test
    fun cacheFreshnessHasExplicitBoundaries() {
        assertTrue(isInventoryCacheFresh(createdAt = 1_000L, now = 1_500L, maxAgeMs = 500L))
        assertFalse(isInventoryCacheFresh(createdAt = 1_000L, now = 1_501L, maxAgeMs = 500L))
        assertFalse(isInventoryCacheFresh(createdAt = 1_000L, now = 999L, maxAgeMs = 500L))
        assertFalse(isInventoryCacheFresh(createdAt = 0L, now = 1_000L, maxAgeMs = 500L))
    }
}
