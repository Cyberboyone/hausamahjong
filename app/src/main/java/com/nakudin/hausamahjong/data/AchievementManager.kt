package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences
import com.nakudin.hausamahjong.R

object AchievementManager {
    private const val PREFS_NAME = "achievement_data"
    private const val KEY_PREFIX = "unlocked_"
    private const val KEY_PROGRESS_PREFIX = "progress_"

    private var prefs: SharedPreferences? = null
    private var listeners = mutableListOf<OnAchievementUnlockListener>()

    enum class Type {
        LEVELS_COMPLETED,
        COINS_EARNED,
        FLIPS_DONE,
        COMBO_MASTER,
        SPEED_RUNNER,
        NO_HINT_RUNS,
        NO_UNDO_RUNS,
        DAILY_STREAK,
        PERFECT_CLEARS,
        TOTAL_MATCHES
    }

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val iconRes: Int,
        val type: Type,
        val target: Int,
        val reward: Int
    )

    private val allAchievements = listOf(
        Achievement("level_10", "Apprentice", "Complete 10 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 10, 500),
        Achievement("level_25", "Scholar", "Complete 25 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 25, 1000),
        Achievement("level_50", "Master", "Complete 50 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 50, 2000),
        Achievement("level_100", "Grandmaster", "Complete 100 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 100, 5000),
        Achievement("level_250", "Sage", "Complete 250 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 250, 10000),
        Achievement("level_500", "Legend", "Complete all 500 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 500, 25000),

        Achievement("coin_1000", "Saver", "Earn 1,000 coins total", R.drawable.ic_achievement_coin, Type.COINS_EARNED, 1000, 500),
        Achievement("coin_5000", "Wealthy", "Earn 5,000 coins total", R.drawable.ic_achievement_coin, Type.COINS_EARNED, 5000, 1000),
        Achievement("coin_10000", "Mogul", "Earn 10,000 coins total", R.drawable.ic_achievement_coin, Type.COINS_EARNED, 10000, 5000),
        Achievement("coin_50000", "Tycoon", "Earn 50,000 coins total", R.drawable.ic_achievement_coin, Type.COINS_EARNED, 50000, 10000),

        Achievement("flip_10", "Curious", "Flip 10 face-down tiles", R.drawable.ic_achievement_flip, Type.FLIPS_DONE, 10, 100),
        Achievement("flip_50", "Seeker", "Flip 50 face-down tiles", R.drawable.ic_achievement_flip, Type.FLIPS_DONE, 50, 500),
        Achievement("flip_100", "Explorer", "Flip 100 face-down tiles", R.drawable.ic_achievement_flip, Type.FLIPS_DONE, 100, 1000),
        Achievement("flip_500", "Omniscient", "Flip 500 face-down tiles", R.drawable.ic_achievement_flip, Type.FLIPS_DONE, 500, 5000),

        Achievement("combo_2", "Chain Reaction", "Reach 2x combo", R.drawable.ic_achievement_combo, Type.COMBO_MASTER, 2, 200),
        Achievement("combo_5", "Unstoppable", "Reach 5x combo", R.drawable.ic_achievement_combo, Type.COMBO_MASTER, 5, 1000),
        Achievement("combo_10", "Combo King", "Reach 10x combo", R.drawable.ic_achievement_combo, Type.COMBO_MASTER, 10, 2500),

        Achievement("speed_30", "Swift", "Clear a level under 30 seconds", R.drawable.ic_achievement_speed, Type.SPEED_RUNNER, 1, 1000),
        Achievement("speed_60", "Speed Demon", "Clear a level under 60 seconds", R.drawable.ic_achievement_speed, Type.SPEED_RUNNER, 1, 500),

        Achievement("nohint_10", "Independent", "Clear 10 levels without hints", R.drawable.ic_achievement_nohint, Type.NO_HINT_RUNS, 10, 500),
        Achievement("nohint_50", "Self-Made", "Clear 50 levels without hints", R.drawable.ic_achievement_nohint, Type.NO_HINT_RUNS, 50, 2000),
        Achievement("noundo_10", "Confident", "Clear 10 levels without undo", R.drawable.ic_achievement_noundo, Type.NO_UNDO_RUNS, 10, 500),
        Achievement("noundo_50", "Decisive", "Clear 50 levels without undo", R.drawable.ic_achievement_noundo, Type.NO_UNDO_RUNS, 50, 2000),

        Achievement("streak_7", "Consistent", "7 day login streak", R.drawable.ic_achievement_streak, Type.DAILY_STREAK, 7, 1000),
        Achievement("streak_30", "Dedicated", "30 day login streak", R.drawable.ic_achievement_streak, Type.DAILY_STREAK, 30, 5000),
        Achievement("streak_100", "Legendary", "100 day login streak", R.drawable.ic_achievement_streak, Type.DAILY_STREAK, 100, 25000),

        Achievement("perfect_clear", "Flawless", "Perfect clear (no hints, no undo, under 60s)", R.drawable.ic_achievement_perfect, Type.PERFECT_CLEARS, 1, 1000),
        Achievement("perfect_5", "Perfectionist", "5 perfect clears", R.drawable.ic_achievement_perfect, Type.PERFECT_CLEARS, 5, 5000),

        Achievement("match_100", "Match Maker", "Make 100 matches", R.drawable.ic_achievement_match, Type.TOTAL_MATCHES, 100, 200),
        Achievement("match_500", "Match Master", "Make 500 matches", R.drawable.ic_achievement_match, Type.TOTAL_MATCHES, 500, 1000),
        Achievement("match_1000", "Match Legend", "Make 1000 matches", R.drawable.ic_achievement_match, Type.TOTAL_MATCHES, 1000, 2500),

        Achievement("complete_all_levels", "Hausa Champion", "Complete all 500 levels", R.drawable.ic_achievement_level, Type.LEVELS_COMPLETED, 500, 50000),
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("AchievementManager not initialized")

    fun getAllAchievements(): List<Achievement> = allAchievements

    fun getAchievement(id: String): Achievement? = allAchievements.find { it.id == id }

    fun isUnlocked(achievement: Achievement): Boolean {
        return getPrefs().getBoolean("$KEY_PREFIX${achievement.id}", false)
    }

    fun getProgress(achievement: Achievement): Int {
        return getPrefs().getInt("$KEY_PROGRESS_PREFIX${achievement.id}", 0)
    }

    fun setProgress(achievement: Achievement, progress: Int) {
        val editor = getPrefs().edit()
        editor.putInt("$KEY_PROGRESS_PREFIX${achievement.id}", progress.coerceAtMost(achievement.target))
        if (progress >= achievement.target) {
            editor.putBoolean("$KEY_PREFIX${achievement.id}", true)
        }
        editor.apply()
    }

    fun incrementProgress(type: Type, amount: Int = 1) {
        allAchievements.filter { it.type == type && !isUnlocked(it) }
            .forEach { ach ->
                val current = getProgress(ach)
                val newProgress = current + amount
                setProgress(ach, newProgress)
                if (newProgress >= ach.target && current < ach.target) {
                    unlockAchievement(ach)
                }
            }
    }

    fun unlockAchievement(context: Context, id: String): Boolean {
        val achievement = getAchievement(id) ?: return false
        if (isUnlocked(achievement)) return false
        val editor = getPrefs().edit()
        editor.putInt("$KEY_PROGRESS_PREFIX${achievement.id}", achievement.target)
        editor.putBoolean("$KEY_PREFIX${achievement.id}", true)
        editor.apply()

        CoinManager.addCoins(achievement.reward, "achievement_${achievement.id}")
        listeners.forEach { it.onAchievementUnlocked(achievement) }

        val toast = android.widget.Toast.makeText(
            context,
            "Achievement Unlocked: ${achievement.title}\n+${CoinManager.formatCoins(achievement.reward)}",
            android.widget.Toast.LENGTH_LONG
        )
        toast.show()
        return true
    }

    fun updateProgress(context: Context, id: String, currentValue: Int) {
        val achievement = getAchievement(id) ?: return
        if (isUnlocked(achievement)) return
        val clamped = currentValue.coerceAtMost(achievement.target)
        getPrefs().edit().putInt("$KEY_PROGRESS_PREFIX${achievement.id}", clamped).apply()
        if (currentValue >= achievement.target) {
            unlockAchievement(context, id)
        }
    }

    private fun unlockAchievement(achievement: Achievement) {
        val editor = getPrefs().edit()
        editor.putBoolean("$KEY_PREFIX${achievement.id}", true)
        editor.apply()

        CoinManager.addCoins(achievement.reward, "achievement_${achievement.id}")
        listeners.forEach { it.onAchievementUnlocked(achievement) }
    }

    fun addListener(listener: OnAchievementUnlockListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnAchievementUnlockListener) {
        listeners.remove(listener)
    }

    fun getUnlockedCount(): Int = allAchievements.count { isUnlocked(it) }
    fun getTotalCount(): Int = allAchievements.size

    interface OnAchievementUnlockListener {
        fun onAchievementUnlocked(achievement: Achievement)
    }
}