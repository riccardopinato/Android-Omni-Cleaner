package com.riccardopinato.omnicleaner.monetization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.riccardopinato.omnicleaner.R
import kotlinx.coroutines.launch

@Composable
fun MonetizationSection(
    runtime: MonetizationRuntime,
    batch: ProBatchController,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val entitlement by PremiumEntitlementStore.state.collectAsState()
    val premium = entitlement.isPremium
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    val purchaseContextError = stringResource(R.string.pro_purchase_context_error)
    val productUnavailable = stringResource(R.string.pro_product_unavailable)

    val billing = runtime.billing
    val consent = runtime.consent
    val preferences = remember(appContext) { ProPreferencesManager(appContext) }
    val billingState by billing.state.collectAsState()
    val consentState by consent.state.collectAsState()
    val selectedTypes by preferences.selectedTypesFlow.collectAsState(
        initial = ProScanType.entries.toSet(),
    )
    val batchState = batch.state

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
            Text(
                stringResource(R.string.pro_model),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (
                premium &&
                entitlement.source == PremiumEntitlementSource.OFFLINE_CACHE
            ) {
                Text(
                    stringResource(R.string.pro_offline_cache),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!billingState.ownershipResolved && !premium) {
                Text(
                    stringResource(R.string.pro_checking_purchase),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (billingState.purchasePending && !premium) {
                Text(
                    stringResource(R.string.pro_purchase_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

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
                    enabled =
                        !billingState.purchaseInProgress &&
                            !billingState.purchasePending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.pro_buy,
                            billingState.priceLabel ?: stringResource(R.string.pro_price_unavailable),
                        ),
                    )
                }

                OutlinedButton(
                    onClick = {
                        purchaseMessage = null
                        billing.restorePurchases()
                    },
                    enabled = !billingState.restoreInProgress && !billingState.purchaseInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.pro_restore))
                }

                if (billingState.purchaseInProgress || billingState.restoreInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            billingState.error?.let { error ->
                Text(
                    billingErrorMessage(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            purchaseMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            if (premium) {
                Text(stringResource(R.string.pro_batch_title), style = MaterialTheme.typography.titleMedium)
                ProScanType.entries.forEach { type ->
                    val selected = type in selectedTypes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selected,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    val updated = if (checked) selectedTypes + type else selectedTypes - type
                                    scope.launch { preferences.saveSelectedTypes(updated) }
                                },
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = null,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proScanTypeLabel(type), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                proScanTypeDescription(type),
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
                        enabled = !batchState.resolving && !batchState.awaitingSystemTrash,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.pro_batch_scan))
                    }
                }
                ProBatchReview(
                    controller = batch,
                    state = batchState,
                )
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
                    entitlementResolved = entitlement.resolved,
                )
            }
        }
    }
}


@Composable
private fun billingErrorMessage(error: BillingError): String =
    stringResource(
        when (error) {
            BillingError.SERVICE_DISCONNECTED -> R.string.billing_error_service_disconnected
            BillingError.SERVICE_UNAVAILABLE -> R.string.billing_error_service_unavailable
            BillingError.NETWORK_ERROR -> R.string.billing_error_network
            BillingError.BILLING_UNAVAILABLE -> R.string.billing_error_billing_unavailable
            BillingError.DEVELOPER_ERROR -> R.string.billing_error_developer
            BillingError.PRODUCT_UNAVAILABLE -> R.string.billing_error_product_unavailable
            BillingError.ACKNOWLEDGEMENT_PENDING -> R.string.billing_error_acknowledgement_pending
            BillingError.GENERIC -> R.string.billing_error_generic
        },
    )


@Composable
private fun proScanTypeLabel(type: ProScanType): String =
    stringResource(
        when (type) {
            ProScanType.EMPTY_FOLDERS -> R.string.pro_scan_empty_folders_label
            ProScanType.DUPLICATES -> R.string.pro_scan_duplicates_label
            ProScanType.LARGE_FILES -> R.string.pro_scan_large_files_label
            ProScanType.THUMBNAIL_CACHE -> R.string.pro_scan_thumbnail_label
            ProScanType.APP_RESIDUES -> R.string.pro_scan_residues_label
        },
    )

@Composable
private fun proScanTypeDescription(type: ProScanType): String =
    stringResource(
        when (type) {
            ProScanType.EMPTY_FOLDERS -> R.string.pro_scan_empty_folders_description
            ProScanType.DUPLICATES -> R.string.pro_scan_duplicates_description
            ProScanType.LARGE_FILES -> R.string.pro_scan_large_files_description
            ProScanType.THUMBNAIL_CACHE -> R.string.pro_scan_thumbnail_description
            ProScanType.APP_RESIDUES -> R.string.pro_scan_residues_description
        },
    )
