package com.riccardopinato.omnicleaner.monetization

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MonetizationSection() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    var premium by remember { mutableStateOf(false) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    val purchaseContextError = stringResource(R.string.pro_purchase_context_error)
    val productUnavailable = stringResource(R.string.pro_product_unavailable)

    val billing = remember(appContext) { BillingManager(appContext) { premium = it } }
    val consent = remember(appContext) { ConsentManager(appContext) }
    val preferences = remember(appContext) { ProPreferencesManager(appContext) }
    val batch = remember(appContext, scope) { ProBatchController(appContext, scope) }

    val billingState by billing.state.collectAsState()
    val consentState by consent.state.collectAsState()
    val selectedTypes by preferences.selectedTypesFlow.collectAsState(
        initial = ProScanType.entries.toSet(),
    )
    val batchState = batch.state

    DisposableEffect(billing) {
        billing.start()
        onDispose {
            billing.endConnection()
            AdManager.setEligibility(appContext, false)
        }
    }

    LaunchedEffect(activity, premium) {
        if (activity != null && !premium) consent.refresh(activity)
        if (premium) AdManager.setEligibility(appContext, false)
    }
    LaunchedEffect(premium, consentState.canRequestAds) {
        AdManager.setEligibility(
            appContext,
            AdRequestPolicy.mayRequestAds(premium, consentState.canRequestAds),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.titleLarge)
            Text(
                if (premium) stringResource(R.string.pro_active) else stringResource(R.string.pro_free),
                color = if (premium) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!premium) {
                Button(
                    onClick = {
                        if (activity == null) {
                            purchaseMessage = purchaseContextError
                        } else {
                            billing.launchBillingFlow(activity) {
                                purchaseMessage = productUnavailable
                            }
                        }
                    },
                    enabled = !billingState.purchaseInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.pro_buy,
                            billingState.priceLabel ?: stringResource(R.string.pro_price_unavailable),
                        ),
                    )
                }
                if (billingState.purchaseInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            billingState.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            purchaseMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            if (premium) {
                Text(stringResource(R.string.pro_batch_title), style = MaterialTheme.typography.titleMedium)
                ProScanType.entries.forEach { type ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = type in selectedTypes,
                            onCheckedChange = { checked ->
                                val updated = if (checked) selectedTypes + type else selectedTypes - type
                                scope.launch { preferences.saveSelectedTypes(updated) }
                            },
                        )
                        Column {
                            Text(type.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (batchState.running) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = batch::cancel) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    Button(
                        onClick = { batch.scan(selectedTypes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.pro_batch_scan))
                    }
                }
                batchState.results.forEach { result ->
                    Text(
                        "${result.type.label}: ${result.candidates} · ${formatBytes(result.candidateBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                batchState.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            if (!premium && consentState.privacyOptionsRequired && activity != null) {
                OutlinedButton(onClick = { consent.showPrivacyOptions(activity) }) {
                    Text(stringResource(R.string.privacy_options))
                }
            }

            if (!premium) {
                AdManager.Banner(
                    isPremium = false,
                    consentAllowsAds = consentState.canRequestAds,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
