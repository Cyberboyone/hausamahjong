package com.nakudin.hausamahjong.data

import android.content.Context
import com.nakudin.hausamahjong.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class Proverb(
    val hausa_text: String,
    val english_translation: String,
    val meaning_note: String
)

object ProverbRepository {
    private var proverbs: List<Proverb> = emptyList()
    private var currentIndex = 0

    fun init(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.proverbs)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        proverbs = Json.decodeFromString<List<Proverb>>(jsonString)
    }

    fun getNextProverb(): Proverb {
        if (proverbs.isEmpty()) {
            return Proverb(
                hausa_text = "Hakuri maganin duniya",
                english_translation = "Patience is the world's medicine",
                meaning_note = "Patience heals all troubles"
            )
        }
        val proverb = proverbs[currentIndex % proverbs.size]
        currentIndex++
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