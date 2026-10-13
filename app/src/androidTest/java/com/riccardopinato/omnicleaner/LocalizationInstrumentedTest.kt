package com.riccardopinato.omnicleaner

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationInstrumentedTest {
    @Test
    fun hardenedRuntimeErrorsExistInEverySupportedLocale() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        for (locale in listOf(Locale.ENGLISH, Locale.ITALIAN, Locale("es"), Locale.FRENCH, Locale("pt"))) {
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            val localized = base.createConfigurationContext(config)
            assertTrue(localized.getString(R.string.generic_operation_failed).isNotBlank())
            assertTrue(localized.getString(R.string.gallery_error_load_failed).isNotBlank())
            assertTrue(localized.getString(R.string.safe_trash_persistent_note).isNotBlank())
        }
    }
}
