package com.riccardopinato.omnicleaner.monetization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class MonetizationRuntime internal constructor(
    val billing: BillingManager,
    val consent: ConsentManager,
)

@Composable
fun rememberMonetizationRuntime(): MonetizationRuntime {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = remember(context) { context.findActivity() }
    val billing = remember(appContext) { BillingManager(appContext) }
    val consent = remember(appContext) { ConsentManager(appContext) }
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val consentState by consent.state.collectAsState()

    DisposableEffect(billing) {
        billing.start()
        onDispose {
            billing.endConnection()
            AdManager.setEligibility(appContext, false)
        }
    }

    LaunchedEffect(activity, entitlement.resolved, entitlement.isPremium) {
        if (activity != null && entitlement.resolved && !entitlement.isPremium) {
            consent.refresh(activity)
        }
        if (!entitlement.resolved || entitlement.isPremium) {
            AdManager.setEligibility(appContext, false)
        }
    }

    LaunchedEffect(
        entitlement.resolved,
        entitlement.isPremium,
        consentState.canRequestAds,
    ) {
        AdManager.setEligibility(
            appContext,
            AdRequestPolicy.mayRequestAds(
                isPremium = entitlement.isPremium,
                consentAllowsAds = consentState.canRequestAds,
                entitlementResolved = entitlement.resolved,
            ),
        )
    }

    return remember(billing, consent) {
        MonetizationRuntime(
            billing = billing,
            consent = consent,
        )
    }
}
