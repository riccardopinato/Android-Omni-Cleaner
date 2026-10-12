package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementCacheTest {
    @Test
    fun verifiedLifetimePurchaseIsFreshInsideOfflineGrace() {
        val verifiedAt = 1_000_000L

        assertTrue(
            isOfflineEntitlementFresh(
                verifiedAt = verifiedAt,
                now = verifiedAt + 1_000L,
                graceMs = PremiumEntitlementStore.OFFLINE_GRACE_MS,
            ),
        )
    }

    @Test
    fun verifiedLifetimePurchaseExpiresAfterOfflineGrace() {
        val verifiedAt = 1_000_000L

        assertFalse(
            isOfflineEntitlementFresh(
                verifiedAt = verifiedAt,
                now = verifiedAt + PremiumEntitlementStore.OFFLINE_GRACE_MS + 1L,
                graceMs = PremiumEntitlementStore.OFFLINE_GRACE_MS,
            ),
        )
    }

    @Test
    fun invalidOrFutureVerificationNeverBecomesFresh() {
        assertFalse(isOfflineEntitlementFresh(0L, 1_000L, PremiumEntitlementStore.OFFLINE_GRACE_MS))
        assertFalse(isOfflineEntitlementFresh(2_000L, 1_000L, PremiumEntitlementStore.OFFLINE_GRACE_MS))
        assertFalse(isOfflineEntitlementFresh(1_000L, 1_001L, 0L))
    }
}
