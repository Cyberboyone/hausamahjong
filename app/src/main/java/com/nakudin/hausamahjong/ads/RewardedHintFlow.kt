package com.nakudin.hausamahjong.ads

import android.app.Activity
import android.content.Context
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

class RewardedHintFlow(
    private val adManager: AdManager
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var skuDetails: SkuDetails? = null

    fun init(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySkuDetails()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun querySkuDetails() {
        val skuList = listOf(context.getString(R.string.remove_ads_product_id))
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(BillingClient.SkuType.INAPP)
            .build()

        billingClient?.querySkuDetailsAsync(params, object : SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(result: BillingResult, skuDetailsList: List<SkuDetails>?) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    skuDetails = skuDetailsList?.firstOrNull()
                }
            }
        })
    }

    fun launchRemoveAdsPurchase(activity: Activity) {
        val details = skuDetails ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetailsList(listOf(details))
            .build()
        billingClient?.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                adManager.setRemoveAdsPurchased(context, true)
            }
        }
    }

    fun isRemoveAdsPurchased(): Boolean = adManager.isRemoveAdsPurchased()

    companion object {
        private var context: Context? = null

        fun init(context: Context) {
            this.context = context.applicationContext
        }
    }
}