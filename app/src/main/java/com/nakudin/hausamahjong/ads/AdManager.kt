package com.nakudin.hausamahjong.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.services.banners.BannerPosition
import com.unity3d.services.banners.UnityBannerListener

class AdManager {
    private var interstitialAdId = "interstitial"
    private var rewardedAdId = "rewarded"
    private var isInitialized = false
    private var removeAdsPurchased = false
    private var levelCompletions = 0
    private val interstitialFrequencyCap = 4

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
        removeAdsPurchased = prefs.getBoolean("remove_ads", false)
        levelCompletions = prefs.getInt("level_completions", 0)

        if (!removeAdsPurchased) {
            isInitialized = true
            loadInterstitialAd(context)
            loadRewardedAd(context)
        }
    }

    fun setAdUnitIds(interstitialId: String, rewardedId: String) {
        interstitialAdId = interstitialId
        rewardedAdId = rewardedId
    }

    private fun loadInterstitialAd(context: Context) {
        if (!isInitialized || removeAdsPurchased) return
        val options = UnityAdsLoadOptions()
        UnityAds.load(interstitialAdId, options, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {}
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {}
        })
    }

    private fun loadRewardedAd(context: Context) {
        if (!isInitialized || removeAdsPurchased) return
        val options = UnityAdsLoadOptions()
        UnityAds.load(rewardedAdId, options, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {}
            override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAds.UnityAdsLoadError, message: String) {}
        })
    }

    fun isInterstitialAdReady(): Boolean {
        return isInitialized && !removeAdsPurchased && UnityAds.isReady(interstitialAdId)
    }

    fun isRewardedAdReady(): Boolean {
        return isInitialized && !removeAdsPurchased && UnityAds.isReady(rewardedAdId)
    }

    fun showInterstitialAd(activity: Activity) {
        if (!isInterstitialAdReady()) return
        UnityAds.show(activity, interstitialAdId)
        loadInterstitialAd(activity)
    }

    fun showRewardedAd(activity: Activity, onComplete: () -> Unit) {
        if (!isRewardedAdReady()) return
        UnityAds.show(activity, rewardedAdId, object : com.unity3d.ads.UnityAds.IUnityAdsShowListener {
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
}