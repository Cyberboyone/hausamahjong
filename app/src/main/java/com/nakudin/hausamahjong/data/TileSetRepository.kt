package com.nakudin.hausamahjong.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.nakudin.hausamahjong.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class TileSet(
    val id: String,
    val name_ha: String,
    val name_en: String,
    val category: String = "cultural",
    val drawable_ref: String = ""
)

object TileSetRepository {
    private var tileSets: List<TileSet> = emptyList()
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
        val inputStream = context.resources.openRawResource(R.raw.tilesets)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        tileSets = Json.decodeFromString<List<TileSet>>(jsonString)
    }

    fun getTileSet(id: String): TileSet? = tileSets.find { it.id == id }

    fun getTileSets(): List<TileSet> = tileSets.toList()

    fun getAllSymbolIds(): List<String> = tileSets.map { it.id }

    fun getRandomSymbolIds(count: Int): List<String> {
        val shuffled = tileSets.shuffled()
        return shuffled.take(count).map { it.id }
    }

    fun getCount(): Int = tileSets.size

    fun getDrawableResId(symbolId: String): Int {
        val index = tileSets.indexOfFirst { it.id == symbolId }
        if (index < 0) return R.drawable.tile_placeholder
        val resId = context?.resources?.getIdentifier("tile_$index", "drawable", context?.packageName) ?: 0
        return if (resId != 0) resId else R.drawable.tile_placeholder
    }

    fun getDrawable(context: Context, symbolId: String): Drawable? {
        val resId = getDrawableResId(symbolId)
        return if (resId != R.drawable.tile_placeholder) {
            context.getDrawable(resId)
        } else {
            createPlaceholderDrawable(context, symbolId)
        }
    }

    private fun createPlaceholderDrawable(context: Context, symbolId: String): Drawable {
        val tileSet = getTileSet(symbolId)
        val name = tileSet?.name_ha ?: symbolId

        val bitmap = android.graphics.Bitmap.createBitmap(120, 140, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F5E6D3")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(0f, 0f, 120f, 140f, 12f, 12f, bgPaint)

        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#8D6E63")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(0f, 0f, 120f, 140f, 12f, 12f, borderPaint)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#3E2723")
            textSize = 14f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(name, 60f, 80f, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun getSymbolNameHausa(symbolId: String): String {
        return getTileSet(symbolId)?.name_ha ?: symbolId
    }

    fun getSymbolNameEnglish(symbolId: String): String {
        return getTileSet(symbolId)?.name_en ?: symbolId
    }

    fun getSymbolIndex(symbolId: String): Int {
        return tileSets.indexOfFirst { it.id == symbolId }
    }

    fun getSymbolsByCategory(category: String): List<TileSet> {
        return tileSets.filter { it.category == category }
    }

    fun getCategories(): List<String> {
        return tileSets.map { it.category }.distinct()
    }
}