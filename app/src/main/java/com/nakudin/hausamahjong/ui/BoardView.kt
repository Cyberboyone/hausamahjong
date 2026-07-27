package com.nakudin.hausamahjong.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.nakudin.hausamahjong.audio.SoundManager
import com.nakudin.hausamahjong.data.TileSetRepository
import com.nakudin.hausamahjong.game.Board
import com.nakudin.hausamahjong.game.MatchEngine
import com.nakudin.hausamahjong.game.Tile
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnTileClickListener {
        fun onTileClicked(tile: Tile)
        fun onSlotTileClicked(tile: Tile)
    }

    var soundManager: SoundManager? = null

    private var board: Board? = null
    private var listener: OnTileClickListener? = null
    private var freeTiles: Set<Tile> = emptySet()

    private val slotTiles = mutableListOf<Tile>()
    private val maxSlotSize = 4

    private var tileWidth = 0f
    private var tileHeight = 0f
    private var tilePadding = 0f
    private var layerOffsetX = 0f
    private var layerOffsetY = 0f
    private var cornerRadius = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var slotRect = RectF()
    private var slotY = 0f

    private var selectedSlotTile: Tile? = null

    private var animatingTile: AnimatingTile? = null
    private var flippingTile: FlippingTile? = null
    private var breakingTiles: Pair<Tile, Tile>? = null
    private var breakAnimProgress = 0f

    private val particles = mutableListOf<Particle>()

    private val tileBitmapCache = HashMap<String, Bitmap?>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#A1887F")
    }
    private val tileSelectedBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#FF6F00")
    }
    private val tileHighlightBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#FFEB3B")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30000000")
        style = Paint.Style.FILL
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }
    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#6D4C41")
    }
    private val slotTilePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val tileBackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileBackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#8D6E63")
    }

    private var selectedPulse = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { selectedPulse = it.animatedValue as Float; invalidate() }
    }

    private var slotPulse = 0f
    private val slotPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { slotPulse = it.animatedValue as Float; invalidate() }
    }

    fun setBoard(board: Board) {
        this.board = board
        this.freeTiles = MatchEngine.getFreeTiles(board).toSet()
        slotTiles.clear()
        selectedSlotTile = null
        // Restore slot tiles from board state
        for (tile in board.tiles) {
            if (tile.isInSlot) {
                slotTiles.add(tile)
            }
        }
        calculateDimensions()
        invalidate()
    }

    fun setOnTileClickListener(listener: OnTileClickListener) {
        this.listener = listener
    }

    fun clearSelection() {
        selectedSlotTile = null
        invalidate()
    }

    fun highlightTiles(tiles: List<Tile>) {
        invalidate()
    }

    fun getSlotTiles(): List<Tile> = slotTiles.toList()

    fun addToSlot(tile: Tile): Boolean {
        if (slotTiles.size >= maxSlotSize) return false
        if (slotTiles.any { it.id == tile.id }) return false
        slotTiles.add(tile)
        selectedSlotTile = tile
        freeTiles = MatchEngine.getFreeTiles(board!!).toSet()
        invalidate()
        return true
    }

    fun findAndRemoveMatchingPair(symbolId: String): Pair<Tile, Tile>? {
        val matches = slotTiles.filter { it.symbolId == symbolId }
        if (matches.size >= 2) {
            val a = matches[0]
            val b = matches[1]
            slotTiles.remove(a)
            slotTiles.remove(b)
            selectedSlotTile = null
            freeTiles = MatchEngine.getFreeTiles(board!!).toSet()
            return a to b
        }
        return null
    }

    fun removeFromSlot(tile: Tile) {
        slotTiles.remove(tile)
        if (selectedSlotTile?.id == tile.id) selectedSlotTile = null
        freeTiles = MatchEngine.getFreeTiles(board!!).toSet()
        invalidate()
    }

    fun returnAllSlotTiles() {
        for (tile in slotTiles.toList()) {
            tile.isInSlot = false
        }
        slotTiles.clear()
        selectedSlotTile = null
        board?.flipUncoveredTiles()
        freeTiles = MatchEngine.getFreeTiles(board!!).toSet()
        invalidate()
    }

    private fun calculateDimensions() {
        val board = board ?: return
        if (width == 0 || height == 0) return

        val density = resources.displayMetrics.density
        val slotAreaHeight = 64f * density

        val usableWidth = width.toFloat()
        val usableHeight = height.toFloat() - slotAreaHeight

        val idealTileW = (usableWidth - 32f) / minOf(board.width + 1, 6)
        val minTileSize = 44f * density
        tileWidth = maxOf(idealTileW, minTileSize)
        tileHeight = tileWidth * 1.25f
        tilePadding = tileWidth * 0.1f
        cornerRadius = tileWidth * 0.08f
        layerOffsetX = tileWidth * 0.15f
        layerOffsetY = tileHeight * 0.15f

        val totalW = board.width * (tileWidth + tilePadding) + tilePadding
        val totalH = board.height * (tileHeight + tilePadding) + tilePadding + board.maxLayers * layerOffsetY
        boardLeft = (usableWidth - totalW) / 2f
        boardTop = (usableHeight - totalH) / 2f + layerOffsetY * board.maxLayers + slotAreaHeight

        if (boardLeft < 0f) boardLeft = 0f
        if (boardTop < slotAreaHeight) boardTop = slotAreaHeight + 8f

        val slotHeight = 52f * density
        val slotTop = 8f * density
        slotY = slotTop
        slotRect = RectF(
            usableWidth * 0.06f,
            slotTop,
            usableWidth * 0.94f,
            slotTop + slotHeight
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentW = MeasureSpec.getSize(widthMeasureSpec)
        val parentH = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(parentW, parentH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val board = board ?: run {
            drawBoardBackground(canvas)
            drawSlotArea(canvas)
            return
        }
        drawBoardBackground(canvas)
        drawSlotArea(canvas)

        for (layer in 0 until board.maxLayers) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    drawTile(canvas, tile, layer)
                }
            }
        }

        drawAnimatingTile(canvas)
        drawFlippingTile(canvas)
        drawParticles(canvas)

        if (!pulseAnimator.isRunning && selectedSlotTile != null) {
            pulseAnimator.start()
        } else if (selectedSlotTile == null && pulseAnimator.isRunning) {
            pulseAnimator.cancel()
            selectedPulse = 0f
        }

        if (!slotPulseAnimator.isRunning) {
            slotPulseAnimator.start()
        }
    }

    private fun drawBoardBackground(canvas: Canvas) {
        val board = board ?: return
        val totalW = board.width * (tileWidth + tilePadding) + tilePadding + 40f
        val totalH = board.height * (tileHeight + tilePadding) + tilePadding + 40f + board.maxLayers * layerOffsetY
        val left = boardLeft - 20f
        val top = boardTop - 20f - layerOffsetY * board.maxLayers
        val rect = RectF(left, top, left + totalW, top + totalH)

        bgPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            Color.parseColor("#2E7D32"),
            Color.parseColor("#1B5E20"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

        val innerRect = RectF(left + 8f, top + 8f, rect.right - 8f, rect.bottom - 8f)
        bgPaint.shader = LinearGradient(
            innerRect.left, innerRect.top, innerRect.right, innerRect.bottom,
            Color.parseColor("#33691E"),
            Color.parseColor("#2E7D32"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(innerRect, 12f, 12f, bgPaint)
        bgPaint.shader = null
    }

    private fun drawSlotArea(canvas: Canvas) {
        slotPaint.shader = LinearGradient(
            slotRect.left, slotRect.top, slotRect.left, slotRect.bottom,
            Color.parseColor("#5D4037"),
            Color.parseColor("#4E342E"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(slotRect, 12f, 12f, slotPaint)
        canvas.drawRoundRect(slotRect, 12f, 12f, slotBorderPaint)

        val innerSlot = RectF(
            slotRect.left + 4f, slotRect.top + 4f,
            slotRect.right - 4f, slotRect.bottom - 4f
        )
        slotPaint.shader = LinearGradient(
            innerSlot.left, innerSlot.top, innerSlot.left, innerSlot.bottom,
            Color.parseColor("#6D4C41"),
            Color.parseColor("#5D4037"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(innerSlot, 8f, 8f, slotPaint)
        slotPaint.shader = null

        val emptySlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4E342E")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
        }

        val slotTileW = (slotRect.width() - 20f) / maxSlotSize
        val slotTileH = slotTileW * 1.2f
        for (i in 0 until maxSlotSize) {
            val x = slotRect.left + 10f + i * slotTileW + slotTileW * 0.05f
            val y = slotRect.centerY() - slotTileH / 2f
            val emptyRect = RectF(x, y, x + slotTileW * 0.9f, y + slotTileH)
            canvas.drawRoundRect(emptyRect, 4f, 4f, emptySlotPaint)
        }

        for ((index, tile) in slotTiles.withIndex()) {
            val x = slotRect.left + 10f + index * slotTileW + slotTileW * 0.05f
            val y = slotRect.centerY() - slotTileH / 2f
            val tileRect = RectF(x, y, x + slotTileW * 0.9f, y + slotTileH)

            val isSelected = tile == selectedSlotTile
            val isPulsing = isSelected && selectedPulse > 0f

            val scale = if (isPulsing) 1f + selectedPulse * 0.05f else 1f
            canvas.save()
            canvas.scale(scale, scale, tileRect.centerX(), tileRect.centerY())

            shadowPaint.alpha = 60
            canvas.drawRoundRect(
                RectF(tileRect.left + 1f, tileRect.top + 2f, tileRect.right + 1f, tileRect.bottom + 2f),
                6f, 6f, shadowPaint
            )
            shadowPaint.alpha = 48

            tileBgPaint.shader = LinearGradient(
                tileRect.left, tileRect.top, tileRect.left, tileRect.bottom,
                Color.parseColor("#FFF8E1"),
                Color.parseColor("#F5E6D3"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(tileRect, 6f, 6f, tileBgPaint)
            tileBgPaint.shader = null

            tileBorderPaint.color = if (isSelected) Color.parseColor("#FF6F00") else Color.parseColor("#A1887F")
            canvas.drawRoundRect(tileRect, 6f, 6f, tileBorderPaint)

            if (isSelected) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF6F00")
                    style = Paint.Style.FILL
                    maskFilter = BlurMaskFilter(6f + selectedPulse * 3f, BlurMaskFilter.Blur.NORMAL)
                    alpha = (0.3f * 255).toInt()
                }
                canvas.drawRoundRect(tileRect, 6f, 6f, glowPaint)
            }

            val bitmap = getTileBitmap(tile.symbolId)
            if (bitmap != null) {
                val padding = slotTileW * 0.08f
                val imgSize = slotTileW * 0.9f - padding * 2
                val imgRect = RectF(
                    tileRect.centerX() - imgSize / 2,
                    tileRect.top + padding,
                    tileRect.centerX() + imgSize / 2,
                    tileRect.top + padding + imgSize
                )
                canvas.drawBitmap(bitmap, null, imgRect, slotTilePaint)
            } else {
                val miniIconSize = slotTileW * 0.5f
                drawCanvasIcon(canvas, tile.symbolId, tileRect.centerX(), tileRect.centerY() - slotTileH * 0.05f, miniIconSize)
            }

            canvas.restore()
        }
    }

    private fun drawTile(canvas: Canvas, tile: Tile, layer: Int) {
        if (tile.isMatched || tile.isInSlot) return

        val isFlipping = flippingTile?.tile?.id == tile.id
        val flipProgress = if (isFlipping) flippingTile?.progress ?: 0f else 0f

        val left = boardLeft + tilePadding + tile.x * (tileWidth + tilePadding) + layer * layerOffsetX
        val top = boardTop + tilePadding + tile.y * (tileHeight + tilePadding) - layer * layerOffsetY
        var renderWidth = tileWidth
        var renderLeft = left
        var showBack = false
        if (isFlipping) {
            val scaled = 1f - flipProgress * 2f
            showBack = scaled > 0f
            renderWidth = tileWidth * kotlin.math.abs(scaled)
            renderLeft = left + (tileWidth - renderWidth) / 2f
        }
        val rect = RectF(renderLeft, top, renderLeft + renderWidth, top + tileHeight)

        canvas.save()

        if (breakingTiles != null) {
            val (a, b) = breakingTiles!!
            if (tile.id == a.id || tile.id == b.id) {
                val scale = 1f + breakAnimProgress * 0.2f
                canvas.scale(scale, scale, rect.centerX(), rect.centerY())
                tileBgPaint.alpha = ((1f - breakAnimProgress) * 255).toInt().coerceIn(0, 255)
                tileBorderPaint.alpha = tileBgPaint.alpha
                iconPaint.alpha = tileBgPaint.alpha
                textPaint.alpha = tileBgPaint.alpha
            }
        }

        val shadowRect = RectF(left + 2f, top + 3f, left + tileWidth + 2f, top + tileHeight + 3f)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        val isFree = tile in freeTiles
        val isFaceUp = if (isFlipping) !showBack else tile.isFaceUp

        if (isFaceUp) {
            tileBgPaint.shader = LinearGradient(
                left, top, left, top + tileHeight,
                Color.parseColor("#FFF8E1"),
                Color.parseColor("#F5E6D3"),
                Shader.TileMode.CLAMP
            )
        } else {
            tileBgPaint.shader = null
            tileBgPaint.color = if (isFree) Color.parseColor("#D7CCC8") else Color.parseColor("#BCAAA4")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileBgPaint)
        tileBgPaint.shader = null

        tileEdgePaint.color = if (isFree) Color.parseColor("#D7CCC8") else Color.parseColor("#BCAAA4")
        val edgeRect = RectF(left, top + tileHeight - 4f, left + tileWidth, top + tileHeight + 3f)
        canvas.drawRoundRect(edgeRect, cornerRadius, cornerRadius, tileEdgePaint)

        tileBorderPaint.color = when {
            !isFree -> Color.parseColor("#8D6E63")
            isFaceUp -> Color.parseColor("#A1887F")
            else -> Color.parseColor("#8D6E63")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileBorderPaint)

        if (isFaceUp) {
            val innerRect = RectF(
                left + tileWidth * 0.06f,
                top + tileHeight * 0.06f,
                left + tileWidth - tileWidth * 0.06f,
                top + tileHeight - tileHeight * 0.12f
            )
            tileBgPaint.shader = LinearGradient(
                innerRect.left, innerRect.top, innerRect.right, innerRect.bottom,
                Color.parseColor("#FFECB3"),
                Color.parseColor("#FFE0B2"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(innerRect, cornerRadius * 0.6f, cornerRadius * 0.6f, tileBgPaint)
            tileBgPaint.shader = null

            drawTileIcon(canvas, tile, rect)
        } else {
            drawTileBack(canvas, rect)
        }

        if (isFree && !isFaceUp) {
            val freeGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4CAF50")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = (100 + selectedPulse * 50).toInt()
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, freeGlow)
        }

        tileBgPaint.alpha = 255
        tileBorderPaint.alpha = 255
        iconPaint.alpha = 255
        textPaint.alpha = 255

        canvas.restore()
    }

    private fun drawTileBack(canvas: Canvas, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()

        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8D6E63")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val innerRect = RectF(
            rect.left + tileWidth * 0.12f,
            rect.top + tileHeight * 0.1f,
            rect.right - tileWidth * 0.12f,
            rect.bottom - tileHeight * 0.1f
        )
        canvas.drawRoundRect(innerRect, 6f, 6f, patternPaint)

        val diamondSize = tileWidth * 0.15f
        val diamondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A1887F")
            style = Paint.Style.FILL
        }

        for (dy in -1..1) {
            for (dx in -1..1) {
                val px = cx + dx * tileWidth * 0.22f
                val py = cy + dy * tileHeight * 0.2f
                if (px > innerRect.left + 4f && px < innerRect.right - 4f &&
                    py > innerRect.top + 4f && py < innerRect.bottom - 4f) {
                    val dPath = Path()
                    dPath.moveTo(px, py - diamondSize)
                    dPath.lineTo(px + diamondSize, py)
                    dPath.lineTo(px, py + diamondSize)
                    dPath.lineTo(px - diamondSize, py)
                    dPath.close()
                    canvas.drawPath(dPath, diamondPaint)
                }
            }
        }

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6D4C41")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, tileWidth * 0.08f, centerPaint)
    }

    private fun drawTileIcon(canvas: Canvas, tile: Tile, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY() - tileHeight * 0.04f
        val iconSize = tileWidth * 0.5f
        val symbol = tile.symbolId

        val bitmap = getTileBitmap(symbol)
        if (bitmap != null) {
            val padding = tileWidth * 0.08f
            val imgSize = tileWidth - padding * 2
            val imgTop = cy - imgSize * 0.45f
            val imgRect = RectF(cx - imgSize / 2, imgTop, cx + imgSize / 2, imgTop + imgSize)
            canvas.drawBitmap(bitmap, null, imgRect, slotTilePaint)
        } else {
            iconPaint.color = getCategoryColor(symbol)
            drawCanvasIcon(canvas, symbol, cx, cy, iconSize)
        }


    }

    private fun drawCanvasIcon(canvas: Canvas, symbol: String, cx: Float, cy: Float, iconSize: Float) {
        iconPaint.color = getCategoryColor(symbol)
        when {
            symbol.contains("drum") || symbol == "kalangu" || symbol == "gurmi" || symbol == "shantu" -> drawDrum(canvas, cx, cy, iconSize)
            symbol.contains("shield") || symbol == "garkuwa" || symbol == "takobi" || symbol == "baka" -> drawWeapon(canvas, cx, cy, iconSize)
            symbol.contains("hill") || symbol == "dala_hill" || symbol == "kano_wall" || symbol.contains("wall") || symbol.contains("castle") -> drawBuilding(canvas, cx, cy, iconSize)
            symbol.contains("palace") || symbol.contains("masallaci") || symbol.contains("mosque") || symbol.contains("museum") || symbol.contains("minaret") -> drawPalace(canvas, cx, cy, iconSize)
            symbol.contains("sarki") || symbol.contains("waziri") || symbol.contains("hakimi") || symbol.contains("malam") || symbol.contains("mai_") || symbol.contains("liman") || symbol.contains("barde") || symbol.contains("magajin") || symbol.contains("sarkin") || symbol.contains("jakada") -> drawPerson(canvas, cx, cy, iconSize, symbol)
            symbol.contains("gimbiya") || symbol.contains("yarinya") -> drawWoman(canvas, cx, cy, iconSize)
            symbol.contains("yaro") || symbol.contains("malami") -> drawChild(canvas, cx, cy, iconSize)
            symbol.contains("doki") -> drawHorse(canvas, cx, cy, iconSize)
            symbol.contains("adire") || symbol.contains("ado") || symbol.contains("tulu") || symbol.contains("cloth") || symbol.contains("turmi") || symbol.contains("shirya") -> drawCloth(canvas, cx, cy, iconSize)
            symbol.contains("well") || symbol.contains("kusugu") || symbol.contains("dam") || symbol.contains("rake") -> drawWater(canvas, cx, cy, iconSize)
            symbol.contains("market") || symbol.contains("kasuwa") || symbol.contains("kurmi") -> drawMarket(canvas, cx, cy, iconSize)
            symbol.contains("fishing") || symbol.contains("dawa") || symbol.contains("gero") || symbol.contains("noma") -> drawNature(canvas, cx, cy, iconSize)
            symbol.contains("sulke") || symbol.contains("kwari") || symbol.contains("falo") || symbol.contains("lalle") -> drawNatureSmall(canvas, cx, cy, iconSize)
            symbol.contains("rijiya") || symbol.contains("danga") || symbol.contains("rumbu") || symbol.contains("kujera") || symbol.contains("haske") || symbol.contains("taguwa") -> drawHousehold(canvas, cx, cy, iconSize)
            symbol.contains("kaho") || symbol == "soro" -> drawTool(canvas, cx, cy, iconSize)
            symbol.contains("college") || symbol.contains("barewa") || symbol.contains("kanta") -> drawSchool(canvas, cx, cy, iconSize)
            symbol.contains("turunku") || symbol.contains("maje") -> drawNature(canvas, cx, cy, iconSize)
            symbol.contains("kofar") -> drawGate(canvas, cx, cy, iconSize)
            symbol.contains("gidan") || symbol.contains("rumfa") || symbol.contains("jemea") || symbol.contains("nok") || symbol.contains("gwandu") || symbol.contains("gohir") || symbol.contains("bimin") || symbol.contains("shehu") || symbol.contains("argungu") -> drawPalace(canvas, cx, cy, iconSize)
            else -> drawDefaultIcon(canvas, cx, cy, iconSize, symbol)
        }
    }

    private fun drawDrum(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val w = size * 0.7f
        val h = size * 0.5f
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawOval(RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), iconPaint)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037"); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawLine(cx - w * 0.3f, cy, cx + w * 0.3f, cy, linePaint)
        canvas.drawLine(cx, cy - h * 0.35f, cx, cy + h * 0.35f, linePaint)
    }

    private fun drawWeapon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#607D8B")
        val path = Path()
        path.moveTo(cx, cy - size * 0.4f)
        path.lineTo(cx + size * 0.08f, cy - size * 0.1f)
        path.lineTo(cx + size * 0.06f, cy + size * 0.35f)
        path.lineTo(cx - size * 0.06f, cy + size * 0.35f)
        path.lineTo(cx - size * 0.08f, cy - size * 0.1f)
        path.close()
        canvas.drawPath(path, iconPaint)
        iconPaint.color = Color.parseColor("#455A64")
        canvas.drawRect(RectF(cx - size * 0.15f, cy - size * 0.13f, cx + size * 0.15f, cy - size * 0.08f), iconPaint)
    }

    private fun drawBuilding(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#A1887F")
        canvas.drawRect(RectF(cx - size * 0.35f, cy - size * 0.1f, cx + size * 0.35f, cy + size * 0.4f), iconPaint)
        val triPath = Path()
        triPath.moveTo(cx, cy - size * 0.35f)
        triPath.lineTo(cx + size * 0.4f, cy - size * 0.1f)
        triPath.lineTo(cx - size * 0.4f, cy - size * 0.1f)
        triPath.close()
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawPath(triPath, iconPaint)
        iconPaint.color = Color.parseColor("#5D4037")
        canvas.drawRect(RectF(cx - size * 0.08f, cy + size * 0.1f, cx + size * 0.08f, cy + size * 0.4f), iconPaint)
    }

    private fun drawPalace(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#795548")
        canvas.drawRect(RectF(cx - size * 0.4f, cy, cx + size * 0.4f, cy + size * 0.4f), iconPaint)
        iconPaint.color = Color.parseColor("#A1887F")
        canvas.drawRect(RectF(cx - size * 0.45f, cy - size * 0.05f, cx + size * 0.45f, cy + size * 0.05f), iconPaint)
        canvas.drawCircle(cx, cy - size * 0.2f, size * 0.18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D6E63"); style = Paint.Style.FILL })
        iconPaint.color = Color.parseColor("#FFD54F")
        canvas.drawRect(RectF(cx - size * 0.04f, cy - size * 0.38f, cx + size * 0.04f, cy - size * 0.22f), iconPaint)
        canvas.drawCircle(cx, cy - size * 0.38f, size * 0.03f, iconPaint)
        iconPaint.color = Color.parseColor("#5D4037")
        canvas.drawRect(RectF(cx - size * 0.1f, cy + size * 0.15f, cx + size * 0.1f, cy + size * 0.4f), iconPaint)
    }

    private fun drawPerson(canvas: Canvas, cx: Float, cy: Float, size: Float, symbol: String) {
        val headColor = Color.parseColor("#8D6E63")
        val bodyColor = when {
            symbol.contains("sarki") || symbol.contains("magajin") -> Color.parseColor("#FFD54F")
            symbol.contains("waziri") -> Color.parseColor("#7B1FA2")
            symbol.contains("hakimi") || symbol.contains("liman") -> Color.parseColor("#1565C0")
            symbol.contains("malam") -> Color.parseColor("#2E7D32")
            symbol.contains("barde") || symbol.contains("jakada") -> Color.parseColor("#C62828")
            else -> Color.parseColor("#5D4037")
        }
        iconPaint.color = headColor
        canvas.drawCircle(cx, cy - size * 0.25f, size * 0.12f, iconPaint)
        iconPaint.color = bodyColor
        val bodyPath = Path()
        bodyPath.moveTo(cx, cy - size * 0.12f)
        bodyPath.lineTo(cx + size * 0.2f, cy + size * 0.35f)
        bodyPath.lineTo(cx - size * 0.2f, cy + size * 0.35f)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)
        if (symbol.contains("sarki")) {
            iconPaint.color = Color.parseColor("#FFD54F")
            val crownPath = Path()
            crownPath.moveTo(cx - size * 0.1f, cy - size * 0.35f)
            crownPath.lineTo(cx - size * 0.07f, cy - size * 0.42f)
            crownPath.lineTo(cx, cy - size * 0.37f)
            crownPath.lineTo(cx + size * 0.07f, cy - size * 0.42f)
            crownPath.lineTo(cx + size * 0.1f, cy - size * 0.35f)
            crownPath.close()
            canvas.drawPath(crownPath, iconPaint)
        }
    }

    private fun drawWoman(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawCircle(cx, cy - size * 0.25f, size * 0.1f, iconPaint)
        iconPaint.color = Color.parseColor("#E91E63")
        val bodyPath = Path()
        bodyPath.moveTo(cx - size * 0.05f, cy - size * 0.14f)
        bodyPath.quadTo(cx - size * 0.25f, cy + size * 0.1f, cx - size * 0.2f, cy + size * 0.38f)
        bodyPath.lineTo(cx + size * 0.2f, cy + size * 0.38f)
        bodyPath.quadTo(cx + size * 0.25f, cy + size * 0.1f, cx + size * 0.05f, cy - size * 0.14f)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)
        canvas.drawCircle(cx, cy - size * 0.3f, size * 0.06f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.FILL })
    }

    private fun drawChild(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#A1887F")
        canvas.drawCircle(cx, cy - size * 0.15f, size * 0.09f, iconPaint)
        iconPaint.color = Color.parseColor("#66BB6A")
        canvas.drawRect(RectF(cx - size * 0.12f, cy - size * 0.05f, cx + size * 0.12f, cy + size * 0.25f), iconPaint)
    }

    private fun drawHorse(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#8D6E63")
        val bodyPath = Path()
        bodyPath.moveTo(cx - size * 0.3f, cy)
        bodyPath.quadTo(cx - size * 0.35f, cy - size * 0.2f, cx - size * 0.1f, cy - size * 0.15f)
        bodyPath.quadTo(cx + size * 0.1f, cy - size * 0.2f, cx + size * 0.15f, cy - size * 0.3f)
        bodyPath.lineTo(cx + size * 0.2f, cy - size * 0.2f)
        bodyPath.quadTo(cx + size * 0.3f, cy - size * 0.1f, cx + size * 0.3f, cy + size * 0.15f)
        bodyPath.lineTo(cx + size * 0.2f, cy + size * 0.35f)
        bodyPath.lineTo(cx + size * 0.15f, cy + size * 0.35f)
        bodyPath.lineTo(cx + size * 0.18f, cy + size * 0.15f)
        bodyPath.lineTo(cx - size * 0.15f, cy + size * 0.15f)
        bodyPath.lineTo(cx - size * 0.18f, cy + size * 0.35f)
        bodyPath.lineTo(cx - size * 0.25f, cy + size * 0.35f)
        bodyPath.lineTo(cx - size * 0.22f, cy + size * 0.15f)
        bodyPath.quadTo(cx - size * 0.3f, cy + size * 0.05f, cx - size * 0.3f, cy)
        bodyPath.close()
        canvas.drawPath(bodyPath, iconPaint)
    }

    private fun drawCloth(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#1565C0")
        canvas.drawRect(RectF(cx - size * 0.3f, cy - size * 0.3f, cx + size * 0.3f, cy + size * 0.35f), iconPaint)
        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.STROKE; strokeWidth = 2f }
        for (i in -2..2) { canvas.drawLine(cx - size * 0.25f, cy + i * size * 0.12f, cx + size * 0.25f, cy + i * size * 0.12f, patternPaint) }
        for (i in -1..1) { canvas.drawLine(cx + i * size * 0.15f, cy - size * 0.25f, cx + i * size * 0.15f, cy + size * 0.3f, patternPaint) }
    }

    private fun drawWater(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#1E88E5")
        canvas.drawOval(RectF(cx - size * 0.3f, cy - size * 0.15f, cx + size * 0.3f, cy + size * 0.15f), iconPaint)
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#64B5F6"); style = Paint.Style.STROKE; strokeWidth = 2f }
        val wavePath = Path()
        wavePath.moveTo(cx - size * 0.2f, cy)
        wavePath.quadTo(cx - size * 0.1f, cy - size * 0.06f, cx, cy)
        wavePath.quadTo(cx + size * 0.1f, cy + size * 0.06f, cx + size * 0.2f, cy)
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawMarket(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#FF8F00")
        val awningPath = Path()
        awningPath.moveTo(cx - size * 0.35f, cy - size * 0.05f)
        awningPath.lineTo(cx + size * 0.35f, cy - size * 0.05f)
        awningPath.lineTo(cx + size * 0.4f, cy - size * 0.2f)
        awningPath.lineTo(cx - size * 0.4f, cy - size * 0.2f)
        awningPath.close()
        canvas.drawPath(awningPath, iconPaint)
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawRect(RectF(cx - size * 0.3f, cy - size * 0.05f, cx + size * 0.3f, cy + size * 0.35f), iconPaint)
    }

    private fun drawNature(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#4CAF50")
        canvas.drawCircle(cx, cy - size * 0.1f, size * 0.2f, iconPaint)
        canvas.drawCircle(cx - size * 0.15f, cy + size * 0.05f, size * 0.15f, iconPaint)
        canvas.drawCircle(cx + size * 0.15f, cy + size * 0.05f, size * 0.15f, iconPaint)
        iconPaint.color = Color.parseColor("#795548")
        canvas.drawRect(RectF(cx - size * 0.05f, cy + size * 0.15f, cx + size * 0.05f, cy + size * 0.35f), iconPaint)
    }

    private fun drawNatureSmall(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#66BB6A")
        for (i in 0..2) { canvas.drawCircle(cx + (i - 1) * size * 0.18f, cy - size * 0.05f, size * 0.1f, iconPaint) }
        iconPaint.color = Color.parseColor("#795548")
        canvas.drawRect(RectF(cx - size * 0.03f, cy + size * 0.1f, cx + size * 0.03f, cy + size * 0.25f), iconPaint)
    }

    private fun drawHousehold(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawRect(RectF(cx - size * 0.2f, cy - size * 0.1f, cx + size * 0.2f, cy + size * 0.25f), iconPaint)
        canvas.drawLine(cx - size * 0.2f, cy - size * 0.1f, cx + size * 0.2f, cy - size * 0.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037"); style = Paint.Style.STROKE; strokeWidth = 3f })
    }

    private fun drawTool(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#78909C")
        canvas.drawRect(RectF(cx - size * 0.04f, cy - size * 0.35f, cx + size * 0.04f, cy + size * 0.15f), iconPaint)
        iconPaint.color = Color.parseColor("#5D4037")
        val bladePath = Path()
        bladePath.moveTo(cx, cy + size * 0.15f)
        bladePath.lineTo(cx + size * 0.15f, cy + size * 0.35f)
        bladePath.lineTo(cx - size * 0.15f, cy + size * 0.35f)
        bladePath.close()
        canvas.drawPath(bladePath, iconPaint)
    }

    private fun drawSchool(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#1565C0")
        canvas.drawRect(RectF(cx - size * 0.35f, cy - size * 0.05f, cx + size * 0.35f, cy + size * 0.35f), iconPaint)
        iconPaint.color = Color.parseColor("#BBDEFB")
        canvas.drawRect(RectF(cx - size * 0.25f, cy + size * 0.05f, cx - size * 0.08f, cy + size * 0.2f), iconPaint)
        canvas.drawRect(RectF(cx + size * 0.08f, cy + size * 0.05f, cx + size * 0.25f, cy + size * 0.2f), iconPaint)
    }

    private fun drawGate(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#A1887F")
        canvas.drawRect(RectF(cx - size * 0.35f, cy - size * 0.35f, cx - size * 0.2f, cy + size * 0.35f), iconPaint)
        canvas.drawRect(RectF(cx + size * 0.2f, cy - size * 0.35f, cx + size * 0.35f, cy + size * 0.35f), iconPaint)
        iconPaint.color = Color.parseColor("#8D6E63")
        val archPath = Path()
        archPath.moveTo(cx - size * 0.2f, cy - size * 0.1f)
        archPath.quadTo(cx, cy - size * 0.35f, cx + size * 0.2f, cy - size * 0.1f)
        archPath.lineTo(cx + size * 0.2f, cy + size * 0.35f)
        archPath.lineTo(cx - size * 0.2f, cy + size * 0.35f)
        archPath.close()
        canvas.drawPath(archPath, iconPaint)
    }

    private fun drawDefaultIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, symbol: String) {
        iconPaint.color = getCategoryColor(symbol)
        canvas.drawRoundRect(RectF(cx - size * 0.3f, cy - size * 0.3f, cx + size * 0.3f, cy + size * 0.3f), 8f, 8f, iconPaint)
        val charPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = size * 0.35f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        val initial = symbol.replace("_", " ").split(" ").firstOrNull()?.firstOrNull()?.uppercase() ?: "?"
        canvas.drawText(initial, cx, cy + size * 0.12f, charPaint)
    }

    private fun getCategoryColor(symbol: String): Int {
        return when {
            symbol.contains("drum") || symbol.contains("music") || symbol.contains("kalangu") || symbol.contains("gurmi") || symbol.contains("shantu") -> Color.parseColor("#8D6E63")
            symbol.contains("cloth") || symbol.contains("adire") || symbol.contains("ado") || symbol.contains("tulu") || symbol.contains("shirya") || symbol.contains("turmi") -> Color.parseColor("#1565C0")
            symbol.contains("weapon") || symbol.contains("sword") || symbol.contains("takobi") || symbol.contains("garkuwa") || symbol.contains("baka") -> Color.parseColor("#546E7A")
            symbol.contains("building") || symbol.contains("palace") || symbol.contains("masallaci") || symbol.contains("mosque") -> Color.parseColor("#6D4C41")
            symbol.contains("person") || symbol.contains("sarki") || symbol.contains("waziri") || symbol.contains("hakimi") || symbol.contains("malam") || symbol.contains("mai_") || symbol.contains("liman") || symbol.contains("barde") || symbol.contains("magajin") || symbol.contains("sarkin") || symbol.contains("jakada") || symbol.contains("gimbiya") || symbol.contains("yarinya") || symbol.contains("yaro") || symbol.contains("malami") -> Color.parseColor("#388E3C")
            symbol.contains("water") || symbol.contains("well") || symbol.contains("kusugu") || symbol.contains("dam") || symbol.contains("rake") -> Color.parseColor("#1E88E5")
            symbol.contains("hill") || symbol.contains("wall") || symbol.contains("castle") || symbol.contains("kano") || symbol.contains("zaria") || symbol.contains("katsina") -> Color.parseColor("#A1887F")
            symbol.contains("market") || symbol.contains("kasuwa") || symbol.contains("kurmi") -> Color.parseColor("#FF8F00")
            symbol.contains("dala") || symbol.contains("nok") || symbol.contains("fishing") || symbol.contains("argungu") || symbol.contains("dawa") || symbol.contains("gero") || symbol.contains("noma") -> Color.parseColor("#43A047")
            symbol.contains("college") || symbol.contains("barewa") || symbol.contains("kanta") -> Color.parseColor("#1565C0")
            else -> Color.parseColor("#78909C")
        }
    }

    private fun drawAnimatingTile(canvas: Canvas) {
        val anim = animatingTile ?: return
        val bitmap = getTileBitmap(anim.tile.symbolId)

        canvas.save()
        val progress = anim.progress
        val x = anim.startX + (anim.endX - anim.startX) * progress
        val y = anim.startY + (anim.endY - anim.startY) * progress
        val scale = 1f + (1f - progress) * 0.3f
        canvas.scale(scale, scale, x, y)

        val tileRect = RectF(x - tileWidth / 2, y - tileHeight / 2, x + tileWidth / 2, y + tileHeight / 2)

        tileBgPaint.shader = LinearGradient(
            tileRect.left, tileRect.top, tileRect.left, tileRect.bottom,
            Color.parseColor("#FFF8E1"),
            Color.parseColor("#F5E6D3"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, tileBgPaint)
        tileBgPaint.shader = null

        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A1887F"); style = Paint.Style.STROKE; strokeWidth = 2f
        })

        if (bitmap != null) {
            val padding = tileWidth * 0.08f
            val imgSize = tileWidth - padding * 2
            val imgRect = RectF(x - imgSize / 2, y - imgSize / 2, x + imgSize / 2, y + imgSize / 2)
            canvas.drawBitmap(bitmap, null, imgRect, slotTilePaint)
        } else {
            drawCanvasIcon(canvas, anim.tile.symbolId, x, y, tileWidth * 0.5f)
        }

        canvas.restore()
    }

    private fun drawFlippingTile(canvas: Canvas) {
        val flip = flippingTile ?: return
        val tile = flip.tile
        val cx = flip.cx
        val cy = flip.cy
        val progress = flip.progress

        val scaleX = kotlin.math.abs(1f - progress * 2f)
        if (scaleX < 0.01f) return
        val halfW = tileWidth * scaleX / 2f
        val tileRect = RectF(cx - halfW, cy - tileHeight / 2, cx + halfW, cy + tileHeight / 2)

        val showBack = (1f - progress * 2f) > 0f

        canvas.save()
        if (showBack) {
            tileBgPaint.shader = null
            tileBgPaint.color = Color.parseColor("#D7CCC8")
            canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, tileBgPaint)
            val innerRect = RectF(tileRect.left + tileWidth * 0.1f, tileRect.top + tileHeight * 0.1f,
                tileRect.right - tileWidth * 0.1f, tileRect.bottom - tileHeight * 0.1f)
            val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8D6E63"); style = Paint.Style.STROKE; strokeWidth = 1.5f
            }
            canvas.drawRoundRect(innerRect, 4f, 4f, patternPaint)
        } else {
            tileBgPaint.shader = LinearGradient(
                tileRect.left, tileRect.top, tileRect.left, tileRect.bottom,
                Color.parseColor("#FFF8E1"), Color.parseColor("#F5E6D3"), Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, tileBgPaint)
            tileBgPaint.shader = null
            val bitmap = getTileBitmap(tile.symbolId)
            if (bitmap != null) {
                val imgSize = tileWidth * scaleX - tileWidth * 0.1f
                val imgRect = RectF(cx - imgSize / 2, cy - imgSize / 2, cx + imgSize / 2, cy + imgSize / 2)
                canvas.drawBitmap(bitmap, null, imgRect, slotTilePaint)
            } else {
                drawCanvasIcon(canvas, tile.symbolId, cx, cy, tileWidth * 0.4f * scaleX)
            }
        }
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A1887F"); style = Paint.Style.STROKE; strokeWidth = 2f
        })
        canvas.restore()
    }

    private fun drawParticles(canvas: Canvas) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.update()
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size * p.life, p.paint)
        }
        if (particles.isNotEmpty()) invalidate()
    }

    private fun getTileBitmap(symbolId: String): Bitmap? {
        tileBitmapCache[symbolId]?.let { return it }

        val index = TileSetRepository.getSymbolIndex(symbolId)
        if (index < 0) {
            tileBitmapCache[symbolId] = null
            return null
        }

        val resName = "tile_$index"
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId == 0) {
            tileBitmapCache[symbolId] = null
            return null
        }

        val drawable: Drawable? = try { context.getDrawable(resId) } catch (e: Exception) { null }
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawable?.setBounds(0, 0, 256, 256)
                drawable?.draw(c)
                bmp
            }
        }

        tileBitmapCache[symbolId] = bitmap
        return bitmap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (animatingTile != null || flippingTile != null) return false

                val slotTile = getSlotTileAtPosition(event.x, event.y)
                if (slotTile != null) {
                    soundManager?.tileClick()
                    listener?.onSlotTileClicked(slotTile)
                    return true
                }

                val boardTile = getTileAtPosition(event.x, event.y)
                if (boardTile != null && boardTile in freeTiles) {
                    if (!boardTile.isFaceUp) {
                        soundManager?.select()
                        flipTile(boardTile)
                        return true
                    }
                    soundManager?.tileClick()
                    listener?.onTileClicked(boardTile)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTileAtPosition(touchX: Float, touchY: Float): Tile? {
        val board = board ?: return null

        for (layer in board.maxLayers - 1 downTo 0) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    if (tile.isMatched) continue

                    val left = boardLeft + tilePadding + x * (tileWidth + tilePadding) + layer * layerOffsetX
                    val top = boardTop + tilePadding + y * (tileHeight + tilePadding) - layer * layerOffsetY
                    val rect = RectF(left, top, left + tileWidth, top + tileHeight)

                    if (rect.contains(touchX, touchY)) {
                        return tile
                    }
                }
            }
        }
        return null
    }

    private fun getSlotTileAtPosition(touchX: Float, touchY: Float): Tile? {
        if (!slotRect.contains(touchX, touchY)) return null
        val slotTileW = (slotRect.width() - 20f) / maxSlotSize
        val slotTileH = slotTileW * 1.2f

        for ((index, tile) in slotTiles.withIndex()) {
            val x = slotRect.left + 10f + index * slotTileW + slotTileW * 0.05f
            val y = slotRect.centerY() - slotTileH / 2f
            val tileRect = RectF(x, y, x + slotTileW * 0.9f, y + slotTileH)
            if (tileRect.contains(touchX, touchY)) return tile
        }
        return null
    }

    fun animateTileToSlot(tile: Tile, fromX: Float, fromY: Float, onComplete: () -> Unit) {
        val slotTileW = (slotRect.width() - 20f) / maxSlotSize
        val targetX = slotRect.left + 10f + slotTiles.size * slotTileW + slotTileW * 0.5f
        val targetY = slotRect.centerY()

        animatingTile = AnimatingTile(tile, fromX, fromY, targetX, targetY)
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                animatingTile?.progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animatingTile = null
                    invalidate()
                    onComplete()
                }
            })
            start()
        }
    }

    fun animateBreak(tileA: Tile, tileB: Tile, onComplete: () -> Unit) {
        val board = board ?: return
        var tileARect: RectF? = null
        var tileBRect: RectF? = null

        for (layer in 0 until board.maxLayers) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    if (tile.id == tileA.id) {
                        val left = boardLeft + tilePadding + x * (tileWidth + tilePadding) + layer * layerOffsetX
                        val top = boardTop + tilePadding + y * (tileHeight + tilePadding) - layer * layerOffsetY
                        tileARect = RectF(left, top, left + tileWidth, top + tileHeight)
                    }
                    if (tile.id == tileB.id) {
                        val left = boardLeft + tilePadding + x * (tileWidth + tilePadding) + layer * layerOffsetX
                        val top = boardTop + tilePadding + y * (tileHeight + tilePadding) - layer * layerOffsetY
                        tileBRect = RectF(left, top, left + tileWidth, top + tileHeight)
                    }
                }
            }
        }

        tileARect?.let { spawnBreakParticles(it) }
        tileBRect?.let { spawnBreakParticles(it) }

        breakingTiles = tileA to tileB
        breakAnimProgress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                breakAnimProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    breakingTiles = null
                    breakAnimProgress = 0f
                    invalidate()
                    onComplete()
                }
            })
            start()
        }
    }

    private fun spawnBreakParticles(rect: RectF) {
        val colors = intArrayOf(
            Color.parseColor("#FFD54F"),
            Color.parseColor("#8D6E63"),
            Color.parseColor("#A1887F"),
            Color.parseColor("#FFE0B2"),
            Color.parseColor("#BCAAA4")
        )
        val cx = rect.centerX()
        val cy = rect.centerY()

        for (i in 0 until 20) {
            val angle = Random.nextFloat() * 360f
            val speed = 2f + Random.nextFloat() * 5f
            val vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed
            val vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 3f
            val size = 3f + Random.nextFloat() * 6f
            val color = colors[Random.nextInt(colors.size)]

            particles.add(Particle(cx, cy, vx, vy, size, color))
        }
        invalidate()
    }

    fun flipTile(tile: Tile) {
        val left = boardLeft + tilePadding + tile.x * (tileWidth + tilePadding) + tile.layer * layerOffsetX
        val top = boardTop + tilePadding + tile.y * (tileHeight + tilePadding) - tile.layer * layerOffsetY
        val cx = left + tileWidth / 2
        val cy = top + tileHeight / 2

        flippingTile = FlippingTile(tile, cx, cy)
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                flippingTile?.progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tile.isFaceUp = true
                    flippingTile = null
                    refreshFreeTiles()
                    invalidate()
                }
            })
            start()
        }
    }

    fun refreshFreeTiles() {
        val b = board ?: return
        freeTiles = MatchEngine.getFreeTiles(b).toSet()
        invalidate()
    }

    fun getTileCenterX(tile: Tile): Float {
        val b = board ?: return 0f
        return boardLeft + tilePadding + tile.x * (tileWidth + tilePadding) + tile.layer * layerOffsetX + tileWidth / 2f
    }

    fun getTileCenterY(tile: Tile): Float {
        val b = board ?: return 0f
        return boardTop + tilePadding + tile.y * (tileHeight + tilePadding) - tile.layer * layerOffsetY + tileHeight / 2f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
        slotPulseAnimator.cancel()
    }

    data class FlippingTile(
        val tile: Tile,
        val cx: Float,
        val cy: Float,
        var progress: Float = 0f
    )

    data class AnimatingTile(
        val tile: Tile,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        var progress: Float = 0f
    )

    class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        color: Int
    ) {
        var life = 1f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        fun update() {
            x += vx
            y += vy
            vy += 0.15f
            life -= 0.02f
        }
    }
}
