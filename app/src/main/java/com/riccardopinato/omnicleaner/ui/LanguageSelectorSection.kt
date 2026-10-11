package com.riccardopinato.omnicleaner.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.localization.AppLanguage

@Composable
fun LanguageSelectorSection() {
    val context = LocalContext.current
    var selectedTag by remember(context) {
        mutableStateOf(AppLanguage.selectedTag(context))
    }

    val choices = listOf(
        LanguageChoice(AppLanguage.SYSTEM_TAG, stringResource(R.string.language_system)),
        LanguageChoice("en", stringResource(R.string.language_english)),
        LanguageChoice("it", stringResource(R.string.language_italian)),
        LanguageChoice("es", stringResource(R.string.language_spanish)),
        LanguageChoice("fr", stringResource(R.string.language_french)),
        LanguageChoice("pt", stringResource(R.string.language_portuguese)),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.language_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                choices.forEach { choice ->
                    val selected = selectedTag == choice.tag
                    if (selected) {
                        Button(onClick = {}) {
                            Text(choice.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedTag = choice.tag
                                AppLanguage.setSelectedTag(context, choice.tag)
                                context.findActivityForLanguage()?.recreate()
                            },
                        ) {
                            Text(choice.label)
                        }
                    }
                }
            }
        }
    }
}

private data class LanguageChoice(
    val tag: String,
    val label: String,
)

private tailrec fun Context.findActivityForLanguage(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityForLanguage()
    else -> null
}
