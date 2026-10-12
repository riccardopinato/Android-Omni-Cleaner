package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MonetizationRuntime internal constructor(
    val billing: BillingManager,
    val consent: ConsentManager,
)

private object MonetizationRuntimeStore {
    @Volatile
    private var instance: MonetizationRuntime? = null

    fun get(context: Context): MonetizationRuntime {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: run {
                PremiumEntitlementStore.initialize(appContext)
                MonetizationRuntime(
                    billing = BillingManager(appContext),
                    consent = ConsentManager(appContext),
                )
            }.also { instance = it }
        }
    }
}

@Composable
fun rememberMonetizationRuntime(): MonetizationRuntime {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = remember(context) { context.findActivity() }
    val runtime = remember(appContext) { MonetizationRuntimeStore.get(appContext) }
    val billing = runtime.billing
    val consent = runtime.consent
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val consentState by consent.state.collectAsState()

    LaunchedEffect(billing) {
        billing.start()
    }

    val lifecycleOwner = activity as? LifecycleOwner
    DisposableEffect(lifecycleOwner, billing) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                billing.refreshPurchases()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    DisposableEffect(appContext) {
        onDispose {
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

    return runtime
}
