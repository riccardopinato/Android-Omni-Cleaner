package com.riccardopinato.omnicleaner.monetization

import com.android.billingclient.api.Purchase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingOwnershipPolicyTest {
    @Test
    fun purchasedPremiumGrantsOwnership() {
        val result = evaluatePremiumPurchases(
            signals = listOf(
                PurchaseSignal(
                    products = setOf(BillingManager.PREMIUM_PRODUCT_ID),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                ),
            ),
            premiumProductId = BillingManager.PREMIUM_PRODUCT_ID,
            purchasedState = Purchase.PurchaseState.PURCHASED,
            pendingState = Purchase.PurchaseState.PENDING,
        )

        assertTrue(result.owned)
        assertFalse(result.pending)
    }

    @Test
    fun pendingPremiumDoesNotGrantOwnership() {
        val result = evaluatePremiumPurchases(
            signals = listOf(
                PurchaseSignal(
                    products = setOf(BillingManager.PREMIUM_PRODUCT_ID),
                    purchaseState = Purchase.PurchaseState.PENDING,
                ),
            ),
            premiumProductId = BillingManager.PREMIUM_PRODUCT_ID,
            purchasedState = Purchase.PurchaseState.PURCHASED,
            pendingState = Purchase.PurchaseState.PENDING,
        )

        assertFalse(result.owned)
        assertTrue(result.pending)
    }

    @Test
    fun unrelatedProductsNeverGrantPremium() {
        val result = evaluatePremiumPurchases(
            signals = listOf(
                PurchaseSignal(
                    products = setOf("other_product"),
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                ),
            ),
            premiumProductId = BillingManager.PREMIUM_PRODUCT_ID,
            purchasedState = Purchase.PurchaseState.PURCHASED,
            pendingState = Purchase.PurchaseState.PENDING,
        )

        assertFalse(result.owned)
        assertFalse(result.pending)
    }
}
