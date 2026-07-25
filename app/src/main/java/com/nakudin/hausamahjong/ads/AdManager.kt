package com.nakudin.hausamahjong.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions

class AdManager {
    private var interstitialAdId = "interstitial"
    private var rewardedAdId = "rewarded"
    private var isInitialized = false
    private var removeAdsPurchased = false
    private var levelCompletions = 0
    private val interstitialFrequencyCap = 4

    private var interstitialLoaded = false
    private var rewardedLoaded = false

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        removeAdsPurchased = prefs.getBoolean("remove_ads", false)
        levelCompletions = prefs.getInt("level_completions", 0)

        if (!removeAdsPurchased) {
            isInitialized = true
            UnityAds.initialize(context, TEST_GAME_ID, true, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    loadInterstitialAd(context)
                    loadRewardedAd(context)
                }
                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                    Log.w("AdManager", "Unity Ads init failed: $message")
                }
            })
        }
    }

    fun setAdUnitIds(interstitialId: String, rewardedId: String) {
        interstitialAdId = interstitialId
        rewardedAdId = rewardedId
    }

    private fun loadInterstitialAd(context: Context) {
        if (!isInitialized || removeAdsPurchased) return
        interstitialLoaded = false
        val options = UnityAdsLoadOptions()
        UnityAds.load(interstitialAdId, options, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                interstitialLoaded = true
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                interstitialLoaded = false
            }
        })
    }

    private fun loadRewardedAd(context: Context) {
        if (!isInitialized || removeAdsPurchased) return
        rewardedLoaded = false
        val options = UnityAdsLoadOptions()
        UnityAds.load(rewardedAdId, options, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                rewardedLoaded = true
            }
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {
                rewardedLoaded = false
            }
        })
    }

    fun isInterstitialAdReady(): Boolean {
        return isInitialized && !removeAdsPurchased && interstitialLoaded
    }

    fun isRewardedAdReady(): Boolean {
        return isInitialized && !removeAdsPurchased && rewardedLoaded
    }

    fun showInterstitialAd(activity: Activity) {
        if (!isInterstitialAdReady()) return
        UnityAds.show(activity, interstitialAdId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                loadInterstitialAd(activity)
            }
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                loadInterstitialAd(activity)
            }
            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
        })
    }

    fun showRewardedAd(activity: Activity, onComplete: () -> Unit) {
        if (!isRewardedAdReady()) return
        UnityAds.show(activity, rewardedAdId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowComplete(placementId: String, state: UnityAds.UnityAdsShowCompletionState) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onComplete()
                }
                loadRewardedAd(activity)
            }
            override fun onUnityAdsShowFailure(placementId: String, error: UnityAds.UnityAdsShowError, message: String) {
                loadRewardedAd(activity)
            }
            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
        })
    }

    fun shouldShowInterstitialAfterLevel(): Boolean {
        levelCompletions++
        return levelCompletions % interstitialFrequencyCap == 0
    }

    fun setRemoveAdsPurchased(context: Context, purchased: Boolean) {
        removeAdsPurchased = purchased
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("remove_ads", purchased).apply()
    }

    fun isRemoveAdsPurchased(): Boolean = removeAdsPurchased

    fun saveLevelCompletions(context: Context) {
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("level_completions", levelCompletions).apply()
    }

    companion object {
        private const val TEST_GAME_ID = "00000"
    }
}
