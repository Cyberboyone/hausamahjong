package com.nakudin.hausamahjong.data

import android.content.Context
import android.content.SharedPreferences

object LevelProgressManager {
    private const val PREFS_NAME = "level_progress"
    private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked"
    private const val KEY_HIGHEST_COMPLETED = "highest_completed"
    private const val MAX_LEVEL = 500

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences =
        prefs ?: throw IllegalStateException("LevelProgressManager not initialized")

    fun getHighestUnlocked(): Int {
        return getPrefs().getInt(KEY_HIGHEST_UNLOCKED, 1)
    }

    fun getHighestCompleted(): Int {
        return getPrefs().getInt(KEY_HIGHEST_COMPLETED, 0)
    }

    fun setHighestUnlocked(level: Int) {
        getPrefs().edit().putInt(KEY_HIGHEST_UNLOCKED, level).apply()
    }

    fun setHighestCompleted(level: Int) {
        val editor = getPrefs().edit()
        editor.putInt(KEY_HIGHEST_COMPLETED, level)
        if (level >= getHighestUnlocked()) {
            editor.putInt(KEY_HIGHEST_UNLOCKED, level + 1)
        }
        editor.apply()
    }

    fun isLevelUnlocked(levelNumber: Int): Boolean {
        return levelNumber <= getHighestUnlocked()
    }

    fun isLevelCompleted(levelNumber: Int): Boolean {
        return levelNumber <= getHighestCompleted()
    }

    fun getCurrentLevel(): Int {
        return getHighestUnlocked().coerceAtMost(MAX_LEVEL)
    }

    fun isGameComplete(): Boolean {
        return getHighestCompleted() >= MAX_LEVEL
    }

    fun resetProgress() {
        getPrefs().edit()
            .putInt(KEY_HIGHEST_UNLOCKED, 1)
            .putInt(KEY_HIGHEST_COMPLETED, 0)
            .apply()
    }
}