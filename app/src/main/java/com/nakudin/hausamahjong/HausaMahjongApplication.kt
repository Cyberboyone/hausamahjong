package com.nakudin.hausamahjong

import android.app.Application
import android.content.Intent
import android.util.Log
import com.nakudin.hausamahjong.ads.AdManager
import com.nakudin.hausamahjong.ads.PurchaseManager
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.data.ProverbRepository
import com.nakudin.hausamahjong.data.TileSetRepository
import com.nakudin.hausamahjong.ui.CrashActivity

class HausaMahjongApplication : Application() {

    var adManager: AdManager? = null
        private set

    var purchaseManager: PurchaseManager? = null
        private set

    private var adsInitialized = false
    private var billingInitialized = false

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            val trace = sw.toString()
            Log.e("HausaMahjong", "CRASH: $trace")

            try {
                val intent = Intent(this, CrashActivity::class.java)
                intent.putExtra("error", "${throwable.javaClass.simpleName}: ${throwable.message}\n\n$trace")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            } catch (_: Throwable) {}
        }

        try {
            LevelRepository.init(this)
            ProverbRepository.init(this)
            TileSetRepository.init(this)
            LevelProgressManager.init(this)
            CoinManager.init(this)
            DailyRewardManager.init(this)
            AchievementManager.init(this)
            ShopManager.init(this)
            DailyChallengeManager.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init repositories", e)
        }
    }

    fun ensureAdsInitialized() {
        if (adsInitialized) return
        adsInitialized = true
    }

    fun ensureBillingInitialized() {
        if (billingInitialized) return
        billingInitialized = true
    }
}
