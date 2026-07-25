package com.nakudin.hausamahjong

import android.app.Application
import android.util.Log
import com.nakudin.hausamahjong.ads.AdManager
import com.nakudin.hausamahjong.ads.PurchaseManager
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.data.ProverbRepository
import com.nakudin.hausamahjong.data.TileSetRepository

class HausaMahjongApplication : Application() {

    var adManager: AdManager? = null
        private set

    var purchaseManager: PurchaseManager? = null
        private set

    private var adsInitialized = false
    private var billingInitialized = false

    override fun onCreate() {
        super.onCreate()

        try {
            LevelRepository.init(this)
            ProverbRepository.init(this)
            TileSetRepository.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init repositories", e)
        }
    }

    fun ensureAdsInitialized() {
        if (adsInitialized) return
        adsInitialized = true
        try {
            adManager = AdManager()
            adManager?.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init AdManager", e)
        }
    }

    fun ensureBillingInitialized() {
        if (billingInitialized) return
        billingInitialized = true
        try {
            purchaseManager = PurchaseManager(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init PurchaseManager", e)
        }
    }
}
