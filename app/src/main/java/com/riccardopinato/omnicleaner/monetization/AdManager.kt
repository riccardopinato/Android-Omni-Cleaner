package com.riccardopinato.omnicleaner.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.riccardopinato.omnicleaner.BuildConfig

object AdManager {
    private const val TAG = "AdManager"

    private var initialized = false
    private var eligible = false
    private var interstitialAd: InterstitialAd? = null
    private var adLoading = false
    private var cleanupActionCounter = 0
    private var lastInterstitialShownTime = 0L

    fun setEligibility(context: Context, mayRequestAds: Boolean) {
        eligible = mayRequestAds && BuildConfig.ADS_ENABLED
        if (!eligible) {
            interstitialAd = null
            adLoading = false
            return
        }

        if (!initialized) {
            MobileAds.initialize(context.applicationContext) {
                initialized = true
                loadInterstitial(context.applicationContext)
            }
        } else if (interstitialAd == null) {
            loadInterstitial(context.applicationContext)
        }
    }

    fun loadInterstitial(context: Context) {
        if (
            !eligible ||
            !BuildConfig.ADS_ENABLED ||
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID.isBlank() ||
            adLoading ||
            interstitialAd != null
        ) {
            return
        }

        adLoading = true
        InterstitialAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    adLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    adLoading = false
                    interstitialAd = null
                    Log.d(TAG, "Interstitial ad not loaded: ${error.message}")
                }
            },
        )
    }

    fun onCleanupCompleted(
        activity: Activity,
        isPremium: Boolean,
        onDismissed: () -> Unit = {},
    ) {
        if (isPremium || !eligible || !BuildConfig.ADS_ENABLED) {
            onDismissed()
            return
        }

        cleanupActionCounter++
        val now = System.currentTimeMillis()
        val shouldAttempt = AdRequestPolicy.shouldAttemptInterstitial(
            cleanupActionCounter = cleanupActionCounter,
            lastInterstitialShownTime = lastInterstitialShownTime,
            now = now,
        )
        val ad = if (shouldAttempt) interstitialAd else null

        if (ad == null) {
            if (interstitialAd == null) loadInterstitial(activity.applicationContext)
            onDismissed()
            return
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownTime = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                lastInterstitialShownTime = System.currentTimeMillis()
                cleanupActionCounter = 0
                loadInterstitial(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(
                error: com.google.android.gms.ads.AdError,
            ) {
                loadInterstitial(activity.applicationContext)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    @Composable
    fun Banner(
        isPremium: Boolean,
        consentAllowsAds: Boolean,
        entitlementResolved: Boolean,
        modifier: Modifier = Modifier,
    ) {
        if (
            !BuildConfig.ADS_ENABLED ||
            BuildConfig.ADMOB_BANNER_UNIT_ID.isBlank() ||
            !AdRequestPolicy.mayRequestAds(
                isPremium = isPremium,
                consentAllowsAds = consentAllowsAds,
                entitlementResolved = entitlementResolved,
            )
        ) {
            return
        }

        AndroidView(
            modifier = modifier.fillMaxWidth().wrapContentHeight(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
