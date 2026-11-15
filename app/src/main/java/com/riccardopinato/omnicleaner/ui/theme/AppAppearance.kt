package com.riccardopinato.omnicleaner.ui.theme

import android.content.Context

/**
 * App-local appearance preference. SYSTEM follows Android's current theme.
 * Persist the user's explicit choice without affecting gallery/cleaner data.
 */
object AppAppearance {
    enum class Mode { SYSTEM, LIGHT, DARK }

    private const val PREFS = "omni_appearance"
    private const val KEY_MODE = "theme_mode"

    fun selectedMode(context: Context): Mode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, Mode.SYSTEM.name)
        return Mode.entries.firstOrNull { it.name == raw } ?: Mode.SYSTEM
    }

    fun setMode(context: Context, mode: Mode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
