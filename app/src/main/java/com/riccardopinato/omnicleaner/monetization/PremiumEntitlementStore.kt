package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PremiumEntitlementSource {
    UNRESOLVED,
    PLAY,
    OFFLINE_CACHE,
}

data class PremiumEntitlementState(
    val resolved: Boolean = false,
    val isPremium: Boolean = false,
    val source: PremiumEntitlementSource = PremiumEntitlementSource.UNRESOLVED,
    val verifiedAtMillis: Long? = null,
    val graceUntilMillis: Long? = null,
)

internal data class CachedPremiumRecord(
    val owned: Boolean,
    val verifiedAtMillis: Long,
)

internal fun resolveCachedPremium(
    record: CachedPremiumRecord?,
    nowMillis: Long,
    graceMillis: Long = PremiumEntitlementStore.OFFLINE_PREMIUM_GRACE_MS,
): PremiumEntitlementState {
    if (record == null || !record.owned || record.verifiedAtMillis <= 0L) {
        return PremiumEntitlementState()
    }

    val safeGrace = graceMillis.coerceAtLeast(0L)
    val graceUntil = runCatching {
        Math.addExact(record.verifiedAtMillis, safeGrace)
    }.getOrDefault(Long.MAX_VALUE)

    if (nowMillis > graceUntil) {
        return PremiumEntitlementState()
    }

    return PremiumEntitlementState(
        resolved = true,
        isPremium = true,
        source = PremiumEntitlementSource.OFFLINE_CACHE,
        verifiedAtMillis = record.verifiedAtMillis,
        graceUntilMillis = graceUntil,
    )
}

/**
 * Provider-agnostic entitlement boundary for the UI.
 *
 * A previously PLAY-verified lifetime purchase is cached for a bounded offline
 * grace period. A cached FREE state is intentionally never used to authorize
 * ad requests: until Play answers, unknown users remain unresolved/no-ads.
 */
object PremiumEntitlementStore {
    internal const val OFFLINE_PREMIUM_GRACE_MS: Long =
        30L * 24L * 60L * 60L * 1000L

    private const val PREFS_NAME = "premium_entitlement"
    private const val KEY_OWNED = "owned"
    private const val KEY_VERIFIED_AT = "verified_at_ms"

    private val _state = MutableStateFlow(PremiumEntitlementState())
    val state: StateFlow<PremiumEntitlementState> = _state.asStateFlow()

    @Volatile
    private var initialized = false
    private var preferences: android.content.SharedPreferences? = null

    @Synchronized
    fun initialize(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (initialized) return

        val prefs = context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )
        preferences = prefs

        val verifiedAt = prefs.getLong(KEY_VERIFIED_AT, 0L)
        val record = if (prefs.contains(KEY_OWNED) && verifiedAt > 0L) {
            CachedPremiumRecord(
                owned = prefs.getBoolean(KEY_OWNED, false),
                verifiedAtMillis = verifiedAt,
            )
        } else {
            null
        }

        _state.value = resolveCachedPremium(record, nowMillis)
        initialized = true
    }

    fun update(owned: Boolean) {
        updateFromPlay(owned)
    }

    fun updateFromPlay(
        owned: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        preferences?.edit()
            ?.putBoolean(KEY_OWNED, owned)
            ?.putLong(KEY_VERIFIED_AT, nowMillis)
            ?.apply()

        _state.value = PremiumEntitlementState(
            resolved = true,
            isPremium = owned,
            source = PremiumEntitlementSource.PLAY,
            verifiedAtMillis = nowMillis,
            graceUntilMillis = if (owned) {
                runCatching { Math.addExact(nowMillis, OFFLINE_PREMIUM_GRACE_MS) }
                    .getOrDefault(Long.MAX_VALUE)
            } else {
                null
            },
        )
    }

    /**
     * Provider temporarily unknown. Keep only a still-valid premium cache; never
     * downgrade a previously verified lifetime user merely because Play is
     * offline, and never authorize ads from a cached FREE result.
     */
    fun markProviderUnknown(nowMillis: Long = System.currentTimeMillis()) {
        val current = _state.value
        val record = current.verifiedAtMillis?.let {
            CachedPremiumRecord(current.isPremium, it)
        }
        _state.value = resolveCachedPremium(record, nowMillis)
    }

    fun markUnresolved() {
        markProviderUnknown()
    }
}
