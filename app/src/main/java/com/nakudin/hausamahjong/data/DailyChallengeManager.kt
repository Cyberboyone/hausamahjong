package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences
import com.nakudin.hausamahjong.game.LevelData
import com.nakudin.hausamahjong.game.LevelLoader
import java.util.Calendar

object DailyChallengeManager {
    private const val PREFS_NAME = "daily_challenge"
    private const val KEY_LAST_CHALLENGE_DATE = "last_challenge_date"
    private const val KEY_CHALLENGE_BEST = "challenge_best"
    private const val KEY_CHALLENGE_COMPLETED = "challenge_completed"

    private var prefs: SharedPreferences? = null

    data class DailyChallenge(
        val dateSeed: Int,
        val levelNumber: Int,
        val difficulty: Int,
        val completed: Boolean = false,
        val bestMoves: Int = 0,
        val bestTime: Long = 0L
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("DailyChallengeManager not initialized")

    fun getTodaySeed(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 10000 +
                (cal.get(Calendar.MONTH) + 1) * 100 +
                cal.get(Calendar.DAY_OF_MONTH)
    }

    fun getDailyChallenge(): LevelData {
        val seed = getTodaySeed()
        // Use the seed to create a deterministic level between 501-600
        val levelNumber = 501 + (seed % 100)
        // Override level generation with the date seed influence
        return LevelLoader.generateLevel(levelNumber)
    }

    fun isNewChallengeAvailable(): Boolean {
        val lastDate = getPrefs().getInt(KEY_LAST_CHALLENGE_DATE, 0)
        return lastDate != getTodaySeed()
    }

    fun markChallengeCompleted(moves: Int, timeMs: Long) {
        val today = getTodaySeed()
        val editor = getPrefs().edit()
        editor.putInt(KEY_LAST_CHALLENGE_DATE, today)
        editor.putBoolean(KEY_CHALLENGE_COMPLETED, true)

        val prevBest = getPrefs().getInt(KEY_CHALLENGE_BEST, Int.MAX_VALUE)
        if (moves < prevBest) {
            editor.putInt(KEY_CHALLENGE_BEST, moves)
        }
        editor.apply()

        // Reward: bonus coins for daily challenge completion
        val baseReward = CoinRewards.LEVEL_BASE * 3
        val perfectBonus = if (moves <= 20) CoinRewards.PERFECT_CLEAR else 0
        CoinManager.addCoins(baseReward + perfectBonus, "daily_challenge_$today")
    }

    fun getStreakBonus(): Int {
        // 3x daily coins for completing today's challenge
        return CoinRewards.DAILY_BASE * 3
    }

    fun getChallengeStats(): DailyChallenge {
        val today = getTodaySeed()
        return DailyChallenge(
            dateSeed = today,
            levelNumber = 501 + (today % 100),
            difficulty = 5,
            completed = getPrefs().getBoolean(KEY_CHALLENGE_COMPLETED, false),
            bestMoves = getPrefs().getInt(KEY_CHALLENGE_BEST, 0),
            bestTime = 0L
        )
    }

    fun resetForNewDay() {
        val today = getTodaySeed()
        val lastDate = getPrefs().getInt(KEY_LAST_CHALLENGE_DATE, 0)
        if (lastDate != today) {
            getPrefs().edit().apply {
                putBoolean(KEY_CHALLENGE_COMPLETED, false)
                apply()
            }
        }
    }
}