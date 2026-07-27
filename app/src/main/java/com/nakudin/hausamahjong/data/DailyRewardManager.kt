package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object DailyRewardManager {
    private const val PREFS_NAME = "daily_rewards"
    private const val KEY_LAST_CLAIM = "last_claim_day"
    private const val KEY_STREAK = "streak"
    private const val KEY_CLAIMED_TODAY = "claimed_today"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        checkNewDay()
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("DailyRewardManager not initialized")

    private fun checkNewDay() {
        val today = getTodayOrdinal()
        val lastClaim = getPrefs().getInt(KEY_LAST_CLAIM, -1)
        if (lastClaim != today) {
            getPrefs().edit().putBoolean(KEY_CLAIMED_TODAY, false).apply()
        }
    }

    private fun getTodayOrdinal(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)
    }

    fun canClaimToday(): Boolean {
        checkNewDay()
        return !getPrefs().getBoolean(KEY_CLAIMED_TODAY, false)
    }

    fun getCurrentStreak(): Int {
        return getPrefs().getInt(KEY_STREAK, 0)
    }

    fun claimDailyReward(): Int {
        if (!canClaimToday()) return 0

        val today = getTodayOrdinal()
        val lastClaim = getPrefs().getInt(KEY_LAST_CLAIM, -1)
        val prevStreak = getPrefs().getInt(KEY_STREAK, 0)

        var newStreak = 1
        if (lastClaim == today - 1) {
            newStreak = prevStreak + 1
        } else if (lastClaim != today) {
            newStreak = 1
        } else {
            newStreak = prevStreak
        }

        val baseReward = CoinRewards.DAILY_BASE
        val streakBonus = (newStreak - 1) * CoinRewards.DAILY_STREAK_BONUS
        val totalReward = baseReward + streakBonus

        getPrefs().edit().apply {
            putInt(KEY_LAST_CLAIM, today)
            putInt(KEY_STREAK, newStreak)
            putBoolean(KEY_CLAIMED_TODAY, true)
            apply()
        }

        CoinManager.addCoins(totalReward, "daily_reward_streak_$newStreak")
        return totalReward
    }

    fun getStreakRewardPreview(streak: Int): Int {
        return CoinRewards.DAILY_BASE + (streak - 1) * CoinRewards.DAILY_STREAK_BONUS
    }

    fun getNextStreakReward(): Int {
        val nextStreak = getCurrentStreak() + 1
        return getStreakRewardPreview(nextStreak)
    }

    fun resetForTesting() {
        getPrefs().edit().apply {
            putInt(KEY_LAST_CLAIM, -1)
            putInt(KEY_STREAK, 0)
            putBoolean(KEY_CLAIMED_TODAY, false)
            apply()
        }
    }
}