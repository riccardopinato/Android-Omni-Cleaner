package com.riccardopinato.omnicleaner.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BillingError {
    SERVICE_DISCONNECTED,
    SERVICE_UNAVAILABLE,
    NETWORK_ERROR,
    BILLING_UNAVAILABLE,
    DEVELOPER_ERROR,
    PRODUCT_UNAVAILABLE,
    ACKNOWLEDGEMENT_PENDING,
    GENERIC,
}

data class BillingState(
    val connected: Boolean = false,
    val ownershipResolved: Boolean = false,
    val premiumOwned: Boolean = false,
    val purchasePending: Boolean = false,
    val priceLabel: String? = null,
    val productAvailable: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val restoreInProgress: Boolean = false,
    val error: BillingError? = null,
)

class BillingManager(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit = {},
) : PurchasesUpdatedListener {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private var restoreAfterConnect = false
    private var connectionInProgress = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (billingClient.isReady) {
            queryPurchases()
            queryProductDetails()
            return
        }
        if (connectionInProgress) return
        connectionInProgress = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionInProgress = false
                val ready = result.responseCode == BillingClient.BillingResponseCode.OK
                _state.value = _state.value.copy(
                    connected = ready,
                    error = if (ready) null else errorFor(result),
                )
                if (ready) {
                    val restoring = restoreAfterConnect
                    restoreAfterConnect = false
                    queryPurchases(userInitiated = restoring)
                    queryProductDetails()
                } else {
                    PremiumEntitlementStore.markProviderUnavailable()
                    if (restoreAfterConnect) {
                        restoreAfterConnect = false
                        _state.value = _state.value.copy(restoreInProgress = false)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionInProgress = false
                PremiumEntitlementStore.markProviderUnavailable()
                _state.value = _state.value.copy(connected = false)
            }
        })
    }

    fun refreshPurchases() {
        if (billingClient.isReady) {
            queryPurchases()
            queryProductDetails()
        } else {
            start()
        }
    }

    fun restorePurchases() {
        _state.value = _state.value.copy(
            restoreInProgress = true,
            error = null,
        )
        if (billingClient.isReady) {
            queryPurchases(userInitiated = true)
        } else {
            restoreAfterConnect = true
            start()
        }
    }

    fun queryPurchases(userInitiated: Boolean = false) {
        if (!billingClient.isReady) {
            if (userInitiated) {
                restoreAfterConnect = true
                start()
            }
            return
        }

        if (userInitiated) {
            _state.value = _state.value.copy(restoreInProgress = true, error = null)
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                PremiumEntitlementStore.markProviderUnavailable()
                _state.value = _state.value.copy(
                    restoreInProgress = false,
                    error = errorFor(result),
                )
                return@queryPurchasesAsync
            }

            val evaluation = evaluate(purchases)
            _state.value = _state.value.copy(
                ownershipResolved = true,
                premiumOwned = evaluation.owned,
                purchasePending = evaluation.pending,
                restoreInProgress = false,
                error = null,
            )
            publishPremium(evaluation.owned)

            purchases
                .asSequence()
                .filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        PREMIUM_PRODUCT_ID in it.products &&
                        !it.isAcknowledged
                }
                .forEach(::acknowledgePurchase)
        }
    }

    fun launchBillingFlow(activity: Activity, onProductNotFound: () -> Unit = {}) {
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(
                purchaseInProgress = false,
                error = BillingError.SERVICE_DISCONNECTED,
            )
            start()
            return
        }
        if (_state.value.premiumOwned) return

        _state.value = _state.value.copy(purchaseInProgress = true, error = null)
        billingClient.queryProductDetailsAsync(premiumProductQuery()) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    purchaseInProgress = false,
                    error = errorFor(result),
                )
                onProductNotFound()
                return@queryProductDetailsAsync
            }

            val product = queryResult.productDetailsList.firstOrNull {
                it.productId == PREMIUM_PRODUCT_ID
            }
            if (product == null) {
                _state.value = _state.value.copy(
                    purchaseInProgress = false,
                    productAvailable = false,
                    error = if (queryResult.unfetchedProductList.isNotEmpty()) {
                        BillingError.PRODUCT_UNAVAILABLE
                    } else {
                        null
                    },
                )
                onProductNotFound()
                return@queryProductDetailsAsync
            }

            val offer = product.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: product.oneTimePurchaseOfferDetails
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .apply {
                    offer?.offerToken?.let(::setOfferToken)
                }
                .build()

            val launchResult = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build(),
            )
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    purchaseInProgress = false,
                    error = errorFor(launchResult),
                )
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _state.value = _state.value.copy(purchaseInProgress = false)

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val relevant = purchases.orEmpty()
                val evaluation = evaluate(relevant)
                if (evaluation.owned) {
                    _state.value = _state.value.copy(
                        ownershipResolved = true,
                        premiumOwned = true,
                        purchasePending = false,
                        error = null,
                    )
                    publishPremium(true)
                    relevant
                        .filter {
                            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                PREMIUM_PRODUCT_ID in it.products &&
                                !it.isAcknowledged
                        }
                        .forEach(::acknowledgePurchase)
                } else if (evaluation.pending) {
                    _state.value = _state.value.copy(
                        purchasePending = true,
                        error = null,
                    )
                } else {
                    queryPurchases()
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(error = null)
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryPurchases()
            }

            else -> {
                _state.value = _state.value.copy(error = errorFor(result))
            }
        }
    }

    fun endConnection() {
        connectionInProgress = false
        billingClient.endConnection()
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) return
        billingClient.queryProductDetailsAsync(premiumProductQuery()) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(error = errorFor(result))
                return@queryProductDetailsAsync
            }

            val product = queryResult.productDetailsList.firstOrNull {
                it.productId == PREMIUM_PRODUCT_ID
            }
            val offer = product?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: product?.oneTimePurchaseOfferDetails

            _state.value = _state.value.copy(
                productAvailable = product != null,
                priceLabel = offer?.formattedPrice,
                error = if (product == null && queryResult.unfetchedProductList.isNotEmpty()) {
                    BillingError.PRODUCT_UNAVAILABLE
                } else {
                    null
                },
            )
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    error = BillingError.ACKNOWLEDGEMENT_PENDING,
                )
            }
        }
    }

    private fun evaluate(purchases: List<Purchase>): PremiumPurchaseEvaluation =
        evaluatePremiumPurchases(
            signals = purchases.map {
                PurchaseSignal(
                    products = it.products.toSet(),
                    purchaseState = it.purchaseState,
                )
            },
            premiumProductId = PREMIUM_PRODUCT_ID,
            purchasedState = Purchase.PurchaseState.PURCHASED,
            pendingState = Purchase.PurchaseState.PENDING,
        )

    private fun publishPremium(owned: Boolean) {
        PremiumEntitlementStore.updateFromPlay(owned)
        onPremiumChanged(owned)
    }

    private fun premiumProductQuery(): QueryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

    private fun errorFor(result: BillingResult): BillingError = when (result.responseCode) {
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
            BillingError.SERVICE_DISCONNECTED
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
            BillingError.SERVICE_UNAVAILABLE
        BillingClient.BillingResponseCode.NETWORK_ERROR ->
            BillingError.NETWORK_ERROR
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            BillingError.BILLING_UNAVAILABLE
        BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
            BillingError.DEVELOPER_ERROR
        else -> BillingError.GENERIC
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
    }
}
