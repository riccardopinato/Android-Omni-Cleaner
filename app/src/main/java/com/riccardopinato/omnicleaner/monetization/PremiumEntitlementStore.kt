package com.riccardopinato.omnicleaner.monetization

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PremiumEntitlementState(
    val resolved: Boolean = false,
    val isPremium: Boolean = false,
)

object PremiumEntitlementStore {
    private val _state = MutableStateFlow(PremiumEntitlementState())
    val state: StateFlow<PremiumEntitlementState> = _state.asStateFlow()

    fun update(owned: Boolean) {
        _state.value = PremiumEntitlementState(
            resolved = true,
            isPremium = owned,
        )
    }

    fun markUnresolved() {
        _state.value = PremiumEntitlementState()
    }
}
