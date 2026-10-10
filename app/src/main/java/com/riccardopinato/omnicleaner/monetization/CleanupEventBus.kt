package com.riccardopinato.omnicleaner.monetization

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CleanupEventBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    fun publishCompleted() {
        _events.tryEmit(Unit)
    }
}
