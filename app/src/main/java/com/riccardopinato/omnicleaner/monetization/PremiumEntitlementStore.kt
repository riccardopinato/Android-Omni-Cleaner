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
    val verifiedAt: Long? = null,
    val graceUntil: Long? = null,
)

object PremiumEntitlementStore {
    private const val PREFS = "premium_entitlement"
    private const val KEY_OWNED = "owned"
    private const val KEY_VERIFIED_AT = "verified_at"
    internal const val OFFLINE_GRACE_MS: Long = 30L * 24L * 60L * 60L * 1000L

    private val _state = MutableStateFlow(PremiumEntitlementState())
    val state: StateFlow<PremiumEntitlementState> = _state.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context, now: Long = System.currentTimeMillis()) {
        val application = context.applicationContext
        appContext = application
        val preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val owned = preferences.getBoolean(KEY_OWNED, false)
        val verifiedAt = preferences.getLong(KEY_VERIFIED_AT, 0L).takeIf { it > 0L }

        _state.value = if (
            owned &&
            verifiedAt != null &&
            isOfflineEntitlementFresh(verifiedAt, now, OFFLINE_GRACE_MS)
        ) {
            PremiumEntitlementState(
                resolved = true,
                isPremium = true,
                source = PremiumEntitlementSource.OFFLINE_CACHE,
                verifiedAt = verifiedAt,
                graceUntil = verifiedAt + OFFLINE_GRACE_MS,
            )
        } else {
            PremiumEntitlementState()
        }
    }

    fun update(owned: Boolean) {
        updateFromPlay(owned)
    }

    fun updateFromPlay(
        owned: Boolean,
        verifiedAt: Long = System.currentTimeMillis(),
    ) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_OWNED, owned)
            ?.putLong(KEY_VERIFIED_AT, verifiedAt)
            ?.apply()

        _state.value = PremiumEntitlementState(
            resolved = true,
            isPremium = owned,
            source = PremiumEntitlementSource.PLAY,
            verifiedAt = verifiedAt,
            graceUntil = if (owned) verifiedAt + OFFLINE_GRACE_MS else null,
        )
    }

    /**
     * Unknown provider state must not overwrite a still-valid cached lifetime
     * entitlement. Once the grace expires, the state becomes unresolved and ads
     * remain disabled until Google Play resolves ownership again.
     */
    fun markProviderUnavailable(now: Long = System.currentTimeMillis()) {
        val current = _state.value
        val verifiedAt = current.verifiedAt
        if (
            current.isPremium &&
            verifiedAt != null &&
            isOfflineEntitlementFresh(verifiedAt, now, OFFLINE_GRACE_MS)
        ) {
            _state.value = current.copy(
                resolved = true,
                source = PremiumEntitlementSource.OFFLINE_CACHE,
                graceUntil = verifiedAt + OFFLINE_GRACE_MS,
            )
        } else {
            _state.value = PremiumEntitlementState()
        }
    }

    fun markUnresolved() {
        _state.value = PremiumEntitlementState()
    }
}

internal fun isOfflineEntitlementFresh(
    verifiedAt: Long,
    now: Long,
    graceMs: Long,
): Boolean {
    if (verifiedAt <= 0L || now < verifiedAt || graceMs <= 0L) return false
    return now - verifiedAt <= graceMs
}
