package com.riccardopinato.omnicleaner.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import com.riccardopinato.omnicleaner.cleaner.StorageInventoryRepository
import com.riccardopinato.omnicleaner.gallery.GalleryController
import com.riccardopinato.omnicleaner.monetization.ProBatchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Retains long-running scan controllers across Activity/configuration
 * recreation. Process death remains truthful: durable Gallery progress is
 * restored from DataStore, while filesystem scans are restarted against fresh
 * storage state instead of pretending an old in-memory snapshot is current.
 */
class OmniCleanerViewModel(application: Application) : AndroidViewModel(application) {
    init {
        StorageInventoryRepository.initialize(application.applicationContext)
    }

    private val controllerScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal val cleanerController =
        CleanerController(application.applicationContext, controllerScope)

    internal val galleryController =
        GalleryController(application.applicationContext, controllerScope)

    internal val proBatchController =
        ProBatchController(application.applicationContext, controllerScope)

    private var uiLocaleKey: String? = null

    fun updateUiContext(context: Context) {
        val localeKey = context.resources.configuration.locales.toLanguageTags()
        if (uiLocaleKey == localeKey) return
        uiLocaleKey = localeKey
        val localizedContext = getApplication<Application>().createConfigurationContext(
            Configuration(context.resources.configuration),
        )
        cleanerController.updateUiContext(localizedContext)
        galleryController.updateUiContext(localizedContext)
        proBatchController.updateUiContext(localizedContext)
    }

    override fun onCleared() {
        cleanerController.cancelScan()
        proBatchController.cancel()
        controllerScope.cancel()
        super.onCleared()
    }
}
