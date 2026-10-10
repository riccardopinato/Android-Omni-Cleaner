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

data class BillingState(
    val connected: Boolean = false,
    val premiumOwned: Boolean = false,
    val priceLabel: String? = null,
    val productAvailable: Boolean = false,
    val purchaseInProgress: Boolean = false,
    val error: String? = null,
)

class BillingManager(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit = {},
) : PurchasesUpdatedListener {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            queryPurchases()
            queryProductDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ready = result.responseCode == BillingClient.BillingResponseCode.OK
                _state.value = _state.value.copy(
                    connected = ready,
                    error = if (ready) null else result.debugMessage,
                )
                if (ready) {
                    queryPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(connected = false)
            }
        })
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(error = result.debugMessage)
                return@queryPurchasesAsync
            }
            val premium = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    PREMIUM_PRODUCT_ID in it.products
            }
            _state.value = _state.value.copy(premiumOwned = premium, error = null)
            publishPremium(premium)
            purchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    PREMIUM_PRODUCT_ID in it.products &&
                    !it.isAcknowledged
            }.forEach(::acknowledgePurchase)
        }
    }

    fun launchBillingFlow(activity: Activity, onProductNotFound: () -> Unit = {}) {
        if (!billingClient.isReady) {
            _state.value = _state.value.copy(error = "Google Play Billing non è connesso.")
            onProductNotFound()
            return
        }
        _state.value = _state.value.copy(purchaseInProgress = true, error = null)
        billingClient.queryProductDetailsAsync(premiumProductQuery()) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(purchaseInProgress = false, error = result.debugMessage)
                onProductNotFound()
                return@queryProductDetailsAsync
            }
            val product = queryResult.productDetailsList.firstOrNull {
                it.productId == PREMIUM_PRODUCT_ID
            }
            if (product == null) {
                _state.value = _state.value.copy(purchaseInProgress = false, productAvailable = false)
                onProductNotFound()
                return@queryProductDetailsAsync
            }

            val offer = product.oneTimePurchaseOfferDetailsList?.firstOrNull()
                ?: product.oneTimePurchaseOfferDetails
            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .apply {
                    offer?.offerToken?.let { token -> setOfferToken(token) }
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
                    error = launchResult.debugMessage,
                )
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _state.value = _state.value.copy(purchaseInProgress = false)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                _state.value = _state.value.copy(error = result.debugMessage)
            }
            return
        }
        val owned = purchases.orEmpty().filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                PREMIUM_PRODUCT_ID in it.products
        }
        if (owned.isNotEmpty()) {
            _state.value = _state.value.copy(premiumOwned = true, error = null)
            publishPremium(true)
            owned.filterNot { it.isAcknowledged }.forEach(::acknowledgePurchase)
        }
    }

    private fun publishPremium(owned: Boolean) {
        PremiumEntitlementStore.update(owned)
        onPremiumChanged(owned)
    }

    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) return
        billingClient.queryProductDetailsAsync(premiumProductQuery()) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(error = result.debugMessage)
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
                error = null,
            )
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(error = result.debugMessage)
            }
        }
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

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
    }
}
