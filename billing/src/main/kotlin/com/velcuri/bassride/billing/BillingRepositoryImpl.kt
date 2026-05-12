package com.velcuri.bassride.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.velcuri.bassride.billing.domain.BillingRepository
import com.velcuri.bassride.data.dao.UserSettingsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val PRO_PRODUCT_ID = "bassride_pro_unlock"

@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsDao: UserSettingsDao
) : BillingRepository, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isProUnlocked = MutableStateFlow(false)
    override val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null
    private var activityRef: WeakReference<Activity>? = null

    override fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        // Pre-populate from Room so the UI never flashes "unpurchased" for Pro users.
        scope.launch {
            val cached = userSettingsDao.get()
            if (cached?.isProUnlockedCached == true) {
                _isProUnlocked.value = true
            }
        }
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { verifyAndSyncProStatus() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will reconnect on next API call via ensureConnected()
            }
        })
    }

    /** Ensures the billing client is connected before making any API call. */
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    if (!continuation.isCompleted) continuation.resume(false)
                }
            })
        }
    }

    override suspend fun getProPrice(): String? {
        val details = queryProductDetails() ?: return null
        return details.oneTimePurchaseOfferDetails?.formattedPrice
    }

    override suspend fun launchPurchaseFlow() {
        val activity = activityRef?.get()
            ?: throw IllegalStateException("Activity not available for billing flow")
        val details = queryProductDetails()
            ?: throw BillingException(
                "Product not found on Google Play. " +
                "Please install BassRide from the Play Store to unlock Pro."
            )

        val productList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw BillingException("Purchase flow failed: ${result.debugMessage}")
        }
    }

    override suspend fun restorePurchases() {
        verifyAndSyncProStatus()
    }

    override suspend fun verifyAndSyncProStatus() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchases ->
            val isPro = purchases.any { purchase ->
                purchase.products.contains(PRO_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _isProUnlocked.value = isPro
            scope.launch {
                userSettingsDao.setProUnlocked(isPro)
                // Acknowledge unacknowledged purchases (required by Play policy)
                purchases.filter {
                    it.products.contains(PRO_PRODUCT_ID) && !it.isAcknowledged
                }.forEach { purchase ->
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { _ -> }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Either a new purchase succeeded or they already own it — verify & sync.
                scope.launch { verifyAndSyncProStatus() }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User cancelled — no action needed
            }
            else -> {
                // Other errors (network, service unavailable, etc.) — no state change
            }
        }
    }

    private suspend fun queryProductDetails(): ProductDetails? {
        cachedProductDetails?.let { return it }
        if (!ensureConnected()) return null

        return suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { _, detailsList ->
                val details = detailsList.firstOrNull()
                cachedProductDetails = details
                continuation.resume(details)
            }
        }
    }
}

class BillingException(message: String) : Exception(message)

