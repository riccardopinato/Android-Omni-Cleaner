package com.riccardopinato.omnicleaner.monetization

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonetizationContractTest {
    @Before
    fun resetBefore() {
        PremiumEntitlementStore.markUnresolved()
    }

    @After
    fun resetAfter() {
        PremiumEntitlementStore.markUnresolved()
    }

    @Test
    fun donorPremiumSku_remainsOneTimeUnlockContract() {
        assertEquals("premium_unlock", BillingManager.PREMIUM_PRODUCT_ID)
    }

    @Test
    fun unresolvedEntitlement_cannotRequestAds() {
        val entitlement = PremiumEntitlementStore.state.value

        assertFalse(entitlement.resolved)
        assertFalse(entitlement.isPremium)
        assertFalse(
            AdRequestPolicy.mayRequestAds(
                isPremium = entitlement.isPremium,
                consentAllowsAds = true,
                entitlementResolved = entitlement.resolved,
            ),
        )
    }

    @Test
    fun premiumEntitlement_blocksAdsAfterResolution() {
        PremiumEntitlementStore.update(true)
        val entitlement = PremiumEntitlementStore.state.value

        assertTrue(entitlement.resolved)
        assertTrue(entitlement.isPremium)
        assertFalse(
            AdRequestPolicy.mayRequestAds(
                isPremium = entitlement.isPremium,
                consentAllowsAds = true,
                entitlementResolved = entitlement.resolved,
            ),
        )
    }

    @Test
    fun appResiduesRemainReviewOnlyInBatchState() {
        assertTrue(ProScanType.APP_RESIDUES in ProScanType.entries)
        assertFalse(
            ProBatchState::class.java.declaredFields.any { field ->
                field.name.contains("selectedResidue", ignoreCase = true)
            },
        )
    }
}
