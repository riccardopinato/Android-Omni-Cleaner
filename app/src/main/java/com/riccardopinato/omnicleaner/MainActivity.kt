package com.riccardopinato.omnicleaner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.riccardopinato.omnicleaner.localization.AppLanguage
import com.riccardopinato.omnicleaner.ui.OmniCleanerViewModel
import com.riccardopinato.omnicleaner.ui.redesign.RedesignedOmniCleanerApp
import com.riccardopinato.omnicleaner.ui.theme.OmniCleanerTheme
import com.riccardopinato.omnicleaner.ui.theme.AppAppearance

class MainActivity : ComponentActivity() {
    private val omniCleanerViewModel: OmniCleanerViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var themeMode by remember { mutableStateOf(AppAppearance.selectedMode(this)) }
            val darkTheme = when (themeMode) {
                AppAppearance.Mode.SYSTEM -> systemDark
                AppAppearance.Mode.LIGHT -> false
                AppAppearance.Mode.DARK -> true
            }
            OmniCleanerTheme(darkTheme = darkTheme) {
                RedesignedOmniCleanerApp(
                    viewModel = omniCleanerViewModel,
                    darkTheme = darkTheme,
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        AppAppearance.setMode(this, newMode)
                        themeMode = newMode
                    },
                    onToggleTheme = {
                        val newMode = if (darkTheme) AppAppearance.Mode.LIGHT
                            else AppAppearance.Mode.DARK
                        AppAppearance.setMode(this, newMode)
                        themeMode = newMode
                    },
                )
            }
        }
    }
}
