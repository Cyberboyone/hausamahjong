package com.nakudin.hausamahjong.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.TileSetRepository
import com.nakudin.hausamahjong.game.Board
import com.nakudin.hausamahjong.game.MatchEngine
import com.nakudin.hausamahjong.game.Tile

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnTileClickListener {
        fun onTileClicked(tile: Tile)
        fun onMatchAttempt(tileA: Tile, tileB: Tile)
    }

    private var board: Board? = null
    private var listener: OnTileClickListener? = null
    private var selectedTile: Tile? = null
    private var highlightedTiles: List<Tile> = emptyList()

    private val tileWidth = 100f
    private val tileHeight = 120f
    private val tilePadding = 6f
    private val layerOffsetX = 10f
    private val layerOffsetY = 10f
    private val cornerRadius = 10f

    private val woodBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 0f, tileHeight,
            Color.parseColor("#F5E6D3"),
            Color.parseColor("#E8D4B8"),
            Shader.TileMode.CLAMP
        )
    }

    private val tileSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE0B2")
        style = Paint.Style.FILL
    }

    private val tileBlockedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D7CCC8")
        style = Paint.Style.FILL
    }

    private val tileFreePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF8E1")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8D6E63")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val selectedBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F00")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E2723")
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E2723")
        textSize = 11f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5D4037")
        textSize = 10f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }

    private var boardLeft = 0f
    private var boardTop = 0f
    private var freeTiles: List<Tile> = emptyList()
    private val drawableCache = mutableMapOf<String, Drawable?>()

    fun setBoard(board: Board) {
        this.board = board
        this.freeTiles = MatchEngine.getFreeTiles(board)
        selectedTile = null
        calculateDimensions()
        invalidate()
    }

    fun setOnTileClickListener(listener: OnTileClickListener) {
        this.listener = listener
    }

    fun clearSelection() {
        selectedTile = null
        highlightedTiles = emptyList()
        invalidate()
    }

    fun highlightTiles(tiles: List<Tile>) {
        highlightedTiles = tiles
        invalidate()
    }

    private fun calculateDimensions() {
        val board = board ?: return
        val totalWidth = board.width * (tileWidth + tilePadding)
        val totalHeight = board.height * (tileHeight + tilePadding)
        boardLeft = (width - totalWidth) / 2f
        boardTop = (height - totalHeight) / 2f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 800
        val desiredHeight = 1000
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val board = board ?: return

        for (layer in 0 until board.maxLayers) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    drawTile(canvas, tile, layer)
                }
            }
        }
    }

    private fun drawTile(canvas: Canvas, tile: Tile, layer: Int) {
        if (tile.isMatched) return

        val left = boardLeft + tile.x * (tileWidth + tilePadding) + layer * layerOffsetX
        val top = boardTop + tile.y * (tileHeight + tilePadding) - layer * layerOffsetY
        val rect = RectF(left, top, left + tileWidth, top + tileHeight)

        canvas.save()

        val shadowRect = RectF(left + 2f, top + 2f, left + tileWidth + 2f, top + tileHeight + 2f)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, woodBgPaint)

        val fillPaint = when {
            tile == selectedTile -> tileSelectedPaint
            tile in highlightedTiles -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFEB3B")
                style = Paint.Style.FILL
                alpha = 200
            }
            tile in freeTiles -> tileFreePaint
            else -> tileBlockedPaint
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

        val border = if (tile == selectedTile) selectedBorderPaint else borderPaint
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, border)

        drawTileImage(canvas, tile, rect)

        val displayName = tile.symbolId.replace("_", " ")
        textPaint.textSize = 9f
        val textY = rect.bottom - 10f
        canvas.drawText(displayName, rect.centerX(), textY, textPaint)

        canvas.restore()
    }

    private fun drawTileImage(canvas: Canvas, tile: Tile, rect: RectF) {
        val drawable = getTileDrawable(tile.symbolId)
        if (drawable != null) {
            val imageRect = RectF(
                rect.left + 8f,
                rect.top + 8f,
                rect.right - 8f,
                rect.bottom - 24f
            )
            drawable.bounds = Rect(
                imageRect.left.toInt(),
                imageRect.top.toInt(),
                imageRect.right.toInt(),
                imageRect.bottom.toInt()
            )
            drawable.draw(canvas)
        } else {
            drawPlaceholderIcon(canvas, tile, rect)
        }
    }

    private fun getTileDrawable(symbolId: String): Drawable? {
        if (drawableCache.containsKey(symbolId)) {
            return drawableCache[symbolId]
        }

        val drawable = TileSetRepository.getDrawable(context, symbolId)
        drawableCache[symbolId] = drawable
        return drawable
    }

    private fun drawPlaceholderIcon(canvas: Canvas, tile: Tile, rect: RectF) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = getIconColor(tile.symbolId)
            style = Paint.Style.FILL
        }

        val iconRect = RectF(
            rect.left + 15f,
            rect.top + 10f,
            rect.right - 15f,
            rect.bottom - 28f
        )

        when {
            tile.symbolId.contains("drum") || tile.symbolId == "kalangu" -> {
                canvas.drawOval(iconRect, iconPaint)
            }
            tile.symbolId.contains("shield") || tile.symbolId == "garkuwa" -> {
                canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), iconRect.width() / 2, iconPaint)
            }
            tile.symbolId.contains("sword") || tile.symbolId == "takobi" -> {
                canvas.drawRect(iconRect, iconPaint)
            }
            tile.symbolId.contains("hill") || tile.symbolId == "dala_hill" -> {
                val path = Path()
                path.moveTo(iconRect.centerX(), iconRect.top)
                path.lineTo(iconRect.right, iconRect.bottom)
                path.lineTo(iconRect.left, iconRect.bottom)
                path.close()
                canvas.drawPath(path, iconPaint)
            }
            tile.symbolId.contains("mosque") || tile.symbolId.contains("palace") -> {
                canvas.drawCircle(iconRect.centerX(), iconRect.centerY() - 8f, 16f, iconPaint)
                canvas.drawRect(iconRect.left + 4f, iconRect.centerY(), iconRect.right - 4f, iconRect.bottom, iconPaint)
            }
            else -> {
                canvas.drawRoundRect(iconRect, 6f, 6f, iconPaint)
            }
        }
    }

    private fun getIconColor(symbolId: String): Int {
        return when {
            symbolId.contains("drum") || symbolId.contains("music") -> Color.parseColor("#8D6E63")
            symbolId.contains("cloth") || symbolId.contains("adire") -> Color.parseColor("#3F51B5")
            symbolId.contains("weapon") || symbolId.contains("sword") -> Color.parseColor("#607D8B")
            symbolId.contains("building") || symbolId.contains("palace") -> Color.parseColor("#795548")
            symbolId.contains("person") || symbolId.contains("sarki") -> Color.parseColor("#4CAF50")
            symbolId.contains("water") || symbolId.contains("well") -> Color.parseColor("#2196F3")
            else -> Color.parseColor("#8D6E63")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val tile = getTileAtPosition(event.x, event.y) ?: return false
                if (tile in freeTiles) {
                    handleTileClick(tile)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTileClick(tile: Tile) {
        if (selectedTile == null) {
            selectedTile = tile
            listener?.onTileClicked(tile)
            invalidate()
        } else {
            if (selectedTile == tile) {
                selectedTile = null
                invalidate()
                return
            }

            val firstTile = selectedTile!!
            if (MatchEngine.canMatch(firstTile, tile, board!!)) {
                listener?.onMatchAttempt(firstTile, tile)
            } else {
                selectedTile = tile
                listener?.onTileClicked(tile)
                invalidate()
            }
        }
    }

    private fun getTileAtPosition(touchX: Float, touchY: Float): Tile? {
        val board = board ?: return null

        for (layer in board.maxLayers - 1 downTo 0) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    if (tile.isMatched) continue

                    val left = boardLeft + x * (tileWidth + tilePadding) + layer * layerOffsetX
                    val top = boardTop + y * (tileHeight + tilePadding) - layer * layerOffsetY
                    val rect = RectF(left, top, left + tileWidth, top + tileHeight)

                    if (rect.contains(touchX, touchY)) {
                        return tile
                    }
                }
            }
        }
        return null
    }

    fun animateMatch(tileA: Tile, tileB: Tile, onComplete: () -> Unit) {
        postDelayed({
            invalidate()
            onComplete()
        }, 300)
    }

    fun animateWrongMatch(tileA: Tile, tileB: Tile, onComplete: () -> Unit) {
        postDelayed({
            invalidate()
            onComplete()
        }, 500)
    }

    fun showHint(tiles: List<Tile>) {
        highlightTiles(tiles)
    }
}