package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumCacheInstrumentedTest {
    @Test
    fun verifiedLifetimeCacheRestoresDuringOfflineGrace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("premium_entitlement", Context.MODE_PRIVATE)
        val verifiedAt = 1_000_000L
        prefs.edit()
            .clear()
            .putBoolean("owned", true)
            .putLong("verified_at", verifiedAt)
            .commit()

        try {
            PremiumEntitlementStore.initialize(context, now = verifiedAt + 1_000L)
            val state = PremiumEntitlementStore.state.value
            assertTrue(state.resolved)
            assertTrue(state.isPremium)
            assertEquals(PremiumEntitlementSource.OFFLINE_CACHE, state.source)
        } finally {
            prefs.edit().clear().commit()
            PremiumEntitlementStore.markUnresolved()
        }
    }
}
