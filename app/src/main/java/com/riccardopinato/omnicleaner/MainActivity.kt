package com.riccardopinato.omnicleaner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
            OmniCleanerTheme {
                RedesignedOmniCleanerApp(omniCleanerViewModel)
            }
        }
    }
}
