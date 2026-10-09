package com.riccardopinato.omnicleaner.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import com.riccardopinato.omnicleaner.recovery.RecoveryFeature

private data class RecoveryFeatureUi(
    val feature: RecoveryFeature,
    @StringRes val title: Int,
)

private val recoveryFeatures = listOf(
    RecoveryFeatureUi(RecoveryFeature.STORAGE_OVERVIEW, R.string.module_storage_overview),
    RecoveryFeatureUi(RecoveryFeature.LARGE_FILES, R.string.module_large_files),
    RecoveryFeatureUi(RecoveryFeature.DUPLICATES, R.string.module_duplicates),
    RecoveryFeatureUi(RecoveryFeature.SAFE_TRASH, R.string.module_safe_trash),
    RecoveryFeatureUi(RecoveryFeature.GALLERY_CLEANER, R.string.module_gallery_cleaner),
    RecoveryFeatureUi(RecoveryFeature.EXIF_PRIVACY, R.string.module_exif_privacy),
)

@Composable
fun OmniCleanerApp() {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            item {
                RecoveryBanner()
            }

            items(
                items = recoveryFeatures,
                key = { it.feature.stableId },
            ) { feature ->
                RecoveryFeatureCard(feature)
            }
        }
    }
}

@Composable
private fun RecoveryBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.recovery_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RecoveryFeatureCard(feature: RecoveryFeatureUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(feature.title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.recovery_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
