package com.riccardopinato.omnicleaner.monetization

import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CleanupEventBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    fun publishCompleted() {
        StorageInventoryRepository.invalidate()
        _events.tryEmit(Unit)
    }
}
