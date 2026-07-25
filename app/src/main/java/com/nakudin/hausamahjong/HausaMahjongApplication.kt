package com.nakudin.hausamahjong

import android.app.Application
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

        adManager = AdManager()
        purchaseManager = PurchaseManager(this)

        LevelRepository.init(this)
        ProverbRepository.init(this)
        TileSetRepository.init(this)
    }
}