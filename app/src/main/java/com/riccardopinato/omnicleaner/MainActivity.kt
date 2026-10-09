package com.riccardopinato.omnicleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.riccardopinato.omnicleaner.ui.OmniCleanerApp
import com.riccardopinato.omnicleaner.ui.theme.OmniCleanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OmniCleanerTheme {
                OmniCleanerApp()
            }
        }
    }
}
