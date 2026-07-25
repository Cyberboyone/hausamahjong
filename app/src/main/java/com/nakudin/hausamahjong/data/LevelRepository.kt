package com.nakudin.hausamahjong.data

import android.content.Context
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.game.LevelData
import com.nakudin.hausamahjong.game.LevelLoader
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

object LevelRepository {
    private var levels: List<LevelData> = emptyList()

    fun init(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.levels)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        levels = Json.decodeFromString<List<LevelData>>(jsonString)
    }

    fun getLevel(levelNumber: Int): LevelData? {
        return levels.find { it.levelNumber == levelNumber }
    }

    fun getLevels(): List<LevelData> = levels.toList()

    fun getMaxLevel(): Int = levels.maxOfOrNull { it.levelNumber } ?: 1

    fun getLevelCount(): Int = levels.size

    fun isLevelAvailable(levelNumber: Int, highestCompleted: Int): Boolean {
        return levelNumber <= highestCompleted + 1
    }

    fun isLevelCompleted(levelNumber: Int, completedLevels: Set<Int>): Boolean {
        return completedLevels.contains(levelNumber)
    }

    fun getLevelsByDifficulty(difficulty: Int): List<LevelData> {
        return levels.filter { it.difficulty == difficulty }
    }
}