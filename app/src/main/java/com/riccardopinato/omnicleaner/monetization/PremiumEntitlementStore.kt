package com.riccardopinato.omnicleaner.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PremiumEntitlementSource {
    UNRESOLVED,
    PLAY,
}

data class PremiumEntitlementState(
    val resolved: Boolean = false,
    val isPremium: Boolean = false,
    val source: PremiumEntitlementSource = PremiumEntitlementSource.UNRESOLVED,
)

object PremiumEntitlementStore {
    private val _state = MutableStateFlow(PremiumEntitlementState())
    val state: StateFlow<PremiumEntitlementState> = _state.asStateFlow()

    fun update(owned: Boolean) {
        updateFromPlay(owned)
    }

    fun updateFromPlay(owned: Boolean) {
        _state.value = PremiumEntitlementState(
            resolved = true,
            isPremium = owned,
            source = PremiumEntitlementSource.PLAY,
        )
    }

    fun markUnresolved() {
        _state.value = PremiumEntitlementState()
    }
}
