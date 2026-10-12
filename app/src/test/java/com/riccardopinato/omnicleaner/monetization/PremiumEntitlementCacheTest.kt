package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementCacheTest {
    @Test
    fun verifiedLifetimePurchaseIsAvailableInsideOfflineGrace() {
        val verifiedAt = 1_000_000L
        val state = resolveCachedPremium(
            record = CachedPremiumRecord(owned = true, verifiedAtMillis = verifiedAt),
            nowMillis = verifiedAt + 1_000L,
        )

        assertTrue(state.resolved)
        assertTrue(state.isPremium)
        assertTrue(state.source == PremiumEntitlementSource.OFFLINE_CACHE)
        assertTrue(state.graceUntilMillis!! > verifiedAt)
    }

    @Test
    fun expiredPremiumCacheReturnsUnknownInsteadOfFree() {
        val verifiedAt = 1_000_000L
        val state = resolveCachedPremium(
            record = CachedPremiumRecord(owned = true, verifiedAtMillis = verifiedAt),
            nowMillis = verifiedAt + PremiumEntitlementStore.OFFLINE_PREMIUM_GRACE_MS + 1L,
        )

        assertFalse(state.resolved)
        assertFalse(state.isPremium)
        assertNull(state.verifiedAtMillis)
    }

    @Test
    fun cachedFreeStateNeverAuthorizesAdsOffline() {
        val state = resolveCachedPremium(
            record = CachedPremiumRecord(owned = false, verifiedAtMillis = 1_000_000L),
            nowMillis = 1_000_001L,
        )

        assertFalse(state.resolved)
        assertFalse(state.isPremium)
    }
}
