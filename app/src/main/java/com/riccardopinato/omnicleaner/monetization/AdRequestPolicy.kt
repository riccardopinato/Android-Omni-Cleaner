package com.riccardopinato.omnicleaner.monetization

object AdRequestPolicy {
    const val ACTIONS_REQUIRED_BETWEEN_ADS: Int = 3
    const val MIN_INTERSTITIAL_INTERVAL_MS: Long = 180_000L

    fun mayRequestAds(
        isPremium: Boolean,
        consentAllowsAds: Boolean,
        entitlementResolved: Boolean,
    ): Boolean =
        entitlementResolved && !isPremium && consentAllowsAds

    fun shouldAttemptInterstitial(
        cleanupActionCounter: Int,
        lastInterstitialShownTime: Long,
        now: Long,
    ): Boolean {
        if (cleanupActionCounter < ACTIONS_REQUIRED_BETWEEN_ADS) return false
        if (lastInterstitialShownTime <= 0L) return true
        return now - lastInterstitialShownTime >= MIN_INTERSTITIAL_INTERVAL_MS
    }
}
