package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementGraceTest {
    @Test
    fun freshVerifiedLifetimeEntitlementUsesOfflineGrace() {
        val verifiedAt = 1_000_000L
        assertTrue(
            isOfflineEntitlementFresh(
                verifiedAt = verifiedAt,
                now = verifiedAt + PremiumEntitlementStore.OFFLINE_GRACE_MS,
                graceMs = PremiumEntitlementStore.OFFLINE_GRACE_MS,
            ),
        )
    }

    @Test
    fun expiredOrClockInvalidEntitlementIsNotTrusted() {
        val verifiedAt = 1_000_000L
        assertFalse(
            isOfflineEntitlementFresh(
                verifiedAt = verifiedAt,
                now = verifiedAt + PremiumEntitlementStore.OFFLINE_GRACE_MS + 1L,
                graceMs = PremiumEntitlementStore.OFFLINE_GRACE_MS,
            ),
        )
        assertFalse(isOfflineEntitlementFresh(verifiedAt, verifiedAt - 1L, 1_000L))
        assertFalse(isOfflineEntitlementFresh(0L, verifiedAt, 1_000L))
    }
}
