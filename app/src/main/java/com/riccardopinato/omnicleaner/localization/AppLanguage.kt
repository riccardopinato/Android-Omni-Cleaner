package com.riccardopinato.omnicleaner.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppLanguage {
    const val SYSTEM_TAG: String = ""

    val supportedTags: Set<String> = setOf("en", "it", "es", "fr", "pt")

    fun selectedTag(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, SYSTEM_TAG)
            .orEmpty()
            .takeIf { it in supportedTags }
            ?: SYSTEM_TAG

    fun setSelectedTag(context: Context, languageTag: String) {
        val normalized = languageTag.takeIf { it in supportedTags } ?: SYSTEM_TAG
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, normalized)
            .commit()
    }

    fun wrap(base: Context): Context {
        val languageTag = selectedTag(base)
        val locale = if (languageTag.isBlank()) {
            base.resources.configuration.locales[0]
        } else {
            Locale.forLanguageTag(languageTag)
        }

        Locale.setDefault(locale)

        if (languageTag.isBlank()) return base

        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }

    private const val PREFERENCES_NAME = "omni_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"
}
