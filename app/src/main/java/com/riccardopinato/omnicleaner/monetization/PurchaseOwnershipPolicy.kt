package com.riccardopinato.omnicleaner.monetization

internal data class PurchaseSignal(
    val products: Set<String>,
    val purchaseState: Int,
)

internal data class PremiumPurchaseEvaluation(
    val owned: Boolean,
    val pending: Boolean,
)

internal fun evaluatePremiumPurchases(
    signals: List<PurchaseSignal>,
    premiumProductId: String,
    purchasedState: Int,
    pendingState: Int,
): PremiumPurchaseEvaluation {
    val relevant = signals.filter { premiumProductId in it.products }
    return PremiumPurchaseEvaluation(
        owned = relevant.any { it.purchaseState == purchasedState },
        pending = relevant.any { it.purchaseState == pendingState },
    )
}
