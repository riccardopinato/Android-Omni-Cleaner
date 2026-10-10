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

object AdManager {
    private const val TAG = "AdManager"
    private const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var initialized = false
    private var eligible = false
    private var interstitialAd: InterstitialAd? = null
    private var adLoading = false
    private var cleanupActionCounter = 0

    fun setEligibility(context: Context, mayRequestAds: Boolean) {
        eligible = mayRequestAds
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
        if (!eligible || adLoading || interstitialAd != null) return
        adLoading = true
        InterstitialAd.load(
            context.applicationContext,
            TEST_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    adLoading = false
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    adLoading = false
                    interstitialAd = null
                    Log.d(TAG, "Interstitial test ad not loaded: ${error.message}")
                }
            },
        )
    }

    fun onCleanupCompleted(activity: Activity, isPremium: Boolean, onDismissed: () -> Unit = {}) {
        if (isPremium || !eligible) {
            onDismissed()
            return
        }
        cleanupActionCounter++
        val ad = if (cleanupActionCounter % 3 == 0) interstitialAd else null
        if (ad == null) {
            if (interstitialAd == null) loadInterstitial(activity.applicationContext)
            onDismissed()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial(activity.applicationContext)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                loadInterstitial(activity.applicationContext)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    @Composable
    fun Banner(isPremium: Boolean, consentAllowsAds: Boolean, modifier: Modifier = Modifier) {
        if (!AdRequestPolicy.mayRequestAds(isPremium, consentAllowsAds)) return
        AndroidView(
            modifier = modifier.fillMaxWidth().wrapContentHeight(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = TEST_BANNER_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
