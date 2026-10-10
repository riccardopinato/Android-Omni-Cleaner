package com.riccardopinato.omnicleaner.monetization

object AdRequestPolicy {
    fun mayRequestAds(isPremium: Boolean, consentAllowsAds: Boolean): Boolean =
        !isPremium && consentAllowsAds
}
