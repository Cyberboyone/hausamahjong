package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences
import com.nakudin.hausamahjong.R

object CoinManager {
    private const val PREFS_NAME = "coin_data"
    private const val KEY_COINS = "coins"
    private const val KEY_TOTAL_EARNED = "total_earned"
    private const val KEY_TOTAL_SPENT = "total_spent"

    private var prefs: SharedPreferences? = null
    private var listeners = mutableListOf<OnCoinChangeListener>()

    interface OnCoinChangeListener {
        fun onCoinsChanged(newAmount: Int, change: Int)
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("CoinManager not initialized")

    fun getCoins(): Int = getPrefs().getInt(KEY_COINS, 500)
    fun getBalance(): Int = getCoins()
    fun getTotalEarned(): Long = getPrefs().getLong(KEY_TOTAL_EARNED, 0L)
    fun getTotalSpent(): Long = getPrefs().getLong(KEY_TOTAL_SPENT, 0L)

    fun addCoins(amount: Int, source: String = "") {
        val current = getCoins()
        val newAmount = (current + amount).coerceAtLeast(0)
        getPrefs().edit().apply {
            putInt(KEY_COINS, newAmount)
            putLong(KEY_TOTAL_EARNED, getTotalEarned() + amount)
            apply()
        }
        listeners.forEach { it.onCoinsChanged(newAmount, amount) }
    }

    fun spendCoins(amount: Int): Boolean {
        val current = getCoins()
        if (current < amount) return false
        val newAmount = current - amount
        getPrefs().edit().apply {
            putInt(KEY_COINS, newAmount)
            putLong(KEY_TOTAL_SPENT, getTotalSpent() + amount)
            apply()
        }
        listeners.forEach { it.onCoinsChanged(newAmount, -amount) }
        return true
    }

    fun canAfford(amount: Int): Boolean = getCoins() >= amount

    fun addListener(listener: OnCoinChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnCoinChangeListener) {
        listeners.remove(listener)
    }

    fun formatCoins(coins: Int): String = "₦${coins.toString().reversed().chunked(3).joinToString(",").reversed()}"
}

data class CoinTransaction(
    val amount: Int,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

object CoinRewards {
    const val LEVEL_BASE = 50
    const val LEVEL_PER_DIFFICULTY = 25
    const val COMBO_2 = 10
    const val COMBO_3 = 25
    const val COMBO_4 = 50
    const val COMBO_5_PLUS = 100
    const val PERFECT_CLEAR = 200
    const val NO_HINTS = 100
    const val NO_UNDO = 50
    const val FAST_CLEAR_UNDER_60 = 150
    const val FAST_CLEAR_UNDER_120 = 75
    const val FLIP_BONUS = 5
    const val DAILY_BASE = 100
    const val DAILY_STREAK_BONUS = 50
    const val ACHIEVEMENT_UNLOCK = 500
    const val SHUFFLE_COST = 200
}