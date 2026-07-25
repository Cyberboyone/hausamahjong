package com.nakudin.hausamahjong.ads

import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.nakudin.hausamahjong.R

class PurchaseManager(
    private val context: Context
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient
    private var skuDetails: SkuDetails? = null
    private var isRemoveAdsPurchased = false

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        loadPurchaseState()
        startConnection()
    }

    private fun loadPurchaseState() {
        val prefs = context.getSharedPreferences("purchase_prefs", Context.MODE_PRIVATE)
        isRemoveAdsPurchased = prefs.getBoolean("remove_ads_purchased", false)
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                    checkPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
                startConnection()
            }
        })
    }

    private fun querySkuDetails() {
        val skuList = listOf(context.getString(R.string.remove_ads_product_id))
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(params, object : SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(result: BillingResult, skuDetailsList: List<SkuDetails>?) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    skuDetails = skuDetailsList?.firstOrNull()
                }
            }
        })
    }

    private fun checkPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.sku == context.getString(R.string.remove_ads_product_id)) {
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

    fun launchRemoveAdsPurchase(activity: android.app.Activity) {
        val details = skuDetails ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetailsList(listOf(details))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (purchase.sku == context.getString(R.string.remove_ads_product_id)) {
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
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { result, purchases ->
            var found = false
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.sku == context.getString(R.string.remove_ads_product_id)) {
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