package com.riccardopinato.omnicleaner.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRequestPolicyTest {
    @Test fun premiumNeverRequestsAds() {
        assertFalse(AdRequestPolicy.mayRequestAds(isPremium = true, consentAllowsAds = true))
    }

    @Test fun freeRequiresConsentBeforeAnyAdRequest() {
        assertFalse(AdRequestPolicy.mayRequestAds(isPremium = false, consentAllowsAds = false))
        assertTrue(AdRequestPolicy.mayRequestAds(isPremium = false, consentAllowsAds = true))
    }
}
