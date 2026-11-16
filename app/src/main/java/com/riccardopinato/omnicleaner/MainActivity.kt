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
import androidx.compose.runtime.saveable.rememberSaveable
import com.riccardopinato.omnicleaner.localization.AppLanguage
import com.riccardopinato.omnicleaner.ui.OmniCleanerViewModel
import com.riccardopinato.omnicleaner.ui.redesign.RedesignedOmniCleanerApp
import com.riccardopinato.omnicleaner.ui.theme.OmniCleanerTheme

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
            var themeOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkTheme = themeOverride ?: systemDark
            OmniCleanerTheme(darkTheme = darkTheme) {
                RedesignedOmniCleanerApp(
                    viewModel = omniCleanerViewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = { themeOverride = !darkTheme },
                )
            }
        }
    }
}
