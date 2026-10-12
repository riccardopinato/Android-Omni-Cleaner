package com.riccardopinato.omnicleaner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.riccardopinato.omnicleaner.monetization.ProBatchController
import com.riccardopinato.omnicleaner.ui.CleanerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class OmniAppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val retainedScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val cleanerController = CleanerController(
        context = application.applicationContext,
        scope = retainedScope,
    )

    val proBatchController = ProBatchController(
        context = application.applicationContext,
        scope = retainedScope,
    )

    override fun onCleared() {
        retainedScope.cancel()
        super.onCleared()
    }
}
