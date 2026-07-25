package com.nakudin.hausamahjong.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.nakudin.hausamahjong.R

class PurchaseManager(
    private val context: Context
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null
    private var isRemoveAdsPurchased = false
    private var isAvailable = false

    init {
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()
            loadPurchaseState()
            startConnection()
            isAvailable = true
        } catch (e: Exception) {
            Log.e("PurchaseManager", "Billing init failed", e)
        }
    }

    private fun loadPurchaseState() {
        val prefs = context.getSharedPreferences("purchase_prefs", Context.MODE_PRIVATE)
        isRemoveAdsPurchased = prefs.getBoolean("remove_ads_purchased", false)
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    checkPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        val productId = context.getString(R.string.remove_ads_product_id)
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList?.firstOrNull()
            }
        }
    }

    private fun checkPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (context.getString(R.string.remove_ads_product_id) in purchase.products) {
                            if (!purchase.isAcknowledged) {
                                acknowledgePurchase(purchase)
                            } else {
                                isRemoveAdsPurchased = true
                                savePurchaseState()
                            }
                        }
                    }
                }
            }
        }
    }

    fun launchRemoveAdsPurchase(activity: Activity) {
        if (!isAvailable) return
        val details = productDetails ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (context.getString(R.string.remove_ads_product_id) in purchase.products) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                isRemoveAdsPurchased = true
                savePurchaseState()
            }
        }
    }

    private fun savePurchaseState() {
        val prefs = context.getSharedPreferences("purchase_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("remove_ads_purchased", isRemoveAdsPurchased).apply()
    }

    fun isRemoveAdsPurchased(): Boolean = isRemoveAdsPurchased

    fun restorePurchases(callback: (Boolean) -> Unit) {
        if (!isAvailable) {
            callback(false)
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            var found = false
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        context.getString(R.string.remove_ads_product_id) in purchase.products) {
                        isRemoveAdsPurchased = true
                        savePurchaseState()
                        found = true
                    }
                }
            }
            callback(found)
        }
    }
}
