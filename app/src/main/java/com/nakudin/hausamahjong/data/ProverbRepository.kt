package com.nakudin.hausamahjong.data

import android.content.Context
import com.nakudin.hausamahjong.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random

@Serializable
data class Proverb(
    val hausa_text: String,
    val english_translation: String,
    val meaning_note: String
)

object ProverbRepository {
    private var proverbs: List<Proverb> = emptyList()
    private var shuffledOrder: MutableList<Int> = mutableListOf()
    private var cycleIndex = 0

    fun init(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.proverbs)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        proverbs = Json.decodeFromString<List<Proverb>>(jsonString)
        reshuffle()
    }

    private fun reshuffle() {
        shuffledOrder = (proverbs.indices).toMutableList()
        shuffledOrder.shuffle(Random(System.nanoTime()))
        cycleIndex = 0
    }

    fun getNextProverb(): Proverb {
        if (proverbs.isEmpty()) {
            return Proverb(
                hausa_text = "Hakuri maganin duniya",
                english_translation = "Patience is the world's medicine",
                meaning_note = "Patience heals all troubles"
            )
        }
        if (cycleIndex >= shuffledOrder.size) {
            reshuffle()
        }
        val proverb = proverbs[shuffledOrder[cycleIndex]]
        cycleIndex++
        return proverb
    }

    fun getRandomProverb(): Proverb {
        if (proverbs.isEmpty()) {
            return Proverb(
                hausa_text = "Hakuri maganin duniya",
                english_translation = "Patience is the world's medicine",
                meaning_note = "Patience heals all troubles"
            )
        }
        return proverbs.random()
    }

    fun getCount(): Int = proverbs.size

    fun getAllProverbs(): List<Proverb> = proverbs.toList()
}