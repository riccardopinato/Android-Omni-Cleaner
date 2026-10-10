package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRequestPolicyTest {
    @Test
    fun unresolvedEntitlementNeverRequestsAds() {
        assertFalse(
            AdRequestPolicy.mayRequestAds(
                isPremium = false,
                consentAllowsAds = true,
                entitlementResolved = false,
            ),
        )
    }

    @Test
    fun premiumNeverRequestsAds() {
        assertFalse(
            AdRequestPolicy.mayRequestAds(
                isPremium = true,
                consentAllowsAds = true,
                entitlementResolved = true,
            ),
        )
    }

    @Test
    fun freeRequiresConsentAndResolvedEntitlementBeforeAds() {
        assertFalse(
            AdRequestPolicy.mayRequestAds(
                isPremium = false,
                consentAllowsAds = false,
                entitlementResolved = true,
            ),
        )
        assertTrue(
            AdRequestPolicy.mayRequestAds(
                isPremium = false,
                consentAllowsAds = true,
                entitlementResolved = true,
            ),
        )
    }

    @Test
    fun donorCadenceRequiresThreeCompletedCleanups() {
        assertFalse(
            AdRequestPolicy.shouldAttemptInterstitial(
                cleanupActionCounter = 2,
                lastInterstitialShownTime = 0L,
                now = 1_000L,
            ),
        )
        assertTrue(
            AdRequestPolicy.shouldAttemptInterstitial(
                cleanupActionCounter = 3,
                lastInterstitialShownTime = 0L,
                now = 1_000L,
            ),
        )
    }

    @Test
    fun donorCadenceRequiresThreeMinutesAfterPreviousInterstitial() {
        val shownAt = 10_000L
        assertFalse(
            AdRequestPolicy.shouldAttemptInterstitial(
                cleanupActionCounter = 3,
                lastInterstitialShownTime = shownAt,
                now = shownAt + 179_999L,
            ),
        )
        assertTrue(
            AdRequestPolicy.shouldAttemptInterstitial(
                cleanupActionCounter = 3,
                lastInterstitialShownTime = shownAt,
                now = shownAt + 180_000L,
            ),
        )
    }
}
