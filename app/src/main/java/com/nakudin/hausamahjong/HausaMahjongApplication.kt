package com.nakudin.hausamahjong

import android.app.Application
import android.util.Log
import com.nakudin.hausamahjong.ads.AdManager
import com.nakudin.hausamahjong.ads.PurchaseManager
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.data.ProverbRepository
import com.nakudin.hausamahjong.data.TileSetRepository

class HausaMahjongApplication : Application() {

    lateinit var adManager: AdManager
        private set

    lateinit var purchaseManager: PurchaseManager
        private set

    override fun onCreate() {
        super.onCreate()

        try {
            LevelRepository.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init LevelRepository", e)
        }

        try {
            ProverbRepository.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init ProverbRepository", e)
        }

        try {
            TileSetRepository.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init TileSetRepository", e)
        }

        adManager = AdManager()
        try {
            adManager.init(this)
        } catch (e: Exception) {
            Log.e("HausaMahjong", "Failed to init AdManager", e)
        }

        purchaseManager = PurchaseManager(this)
    }
}
