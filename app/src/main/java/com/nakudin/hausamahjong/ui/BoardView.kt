package com.nakudin.hausamahjong.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.nakudin.hausamahjong.audio.SoundManager
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

    var soundManager: SoundManager? = null

    private var board: Board? = null
    private var listener: OnTileClickListener? = null
    private var selectedTile: Tile? = null
    private var highlightedTiles: List<Tile> = emptyList()
    private var freeTiles: List<Tile> = emptyList()

    private var tileWidth = 0f
    private var tileHeight = 0f
    private var tilePadding = 0f
    private var layerOffsetX = 0f
    private var layerOffsetY = 0f
    private var cornerRadius = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardScale = 1f

    private var matchAnimTiles: Pair<Tile, Tile>? = null
    private var matchAnimProgress = 0f
    private var selectedPulse = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { selectedPulse = it.animatedValue as Float; invalidate() }
    }

    private val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val tileEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#B0BEC5")
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

    private val tileBlockedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ECEFF1")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20000000")
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

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
        if (width == 0 || height == 0) return

        val usableWidth = width.toFloat()
        val usableHeight = height.toFloat()

        tileWidth = (usableWidth - (board.width + 1) * 4f) / board.width
        tileHeight = tileWidth * 1.25f
        tilePadding = tileWidth * 0.06f
        cornerRadius = tileWidth * 0.08f
        layerOffsetX = tileWidth * 0.07f
        layerOffsetY = tileHeight * 0.07f

        boardScale = 1f

        val totalW = board.width * (tileWidth + tilePadding) + tilePadding
        val totalH = board.height * (tileHeight + tilePadding) + tilePadding + board.maxLayers * layerOffsetY
        boardLeft = (usableWidth - totalW) / 2f
        boardTop = (usableHeight - totalH) / 2f + layerOffsetY * board.maxLayers
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
        drawBoardBackground(canvas)

        val board = board ?: return
        calculateDimensions()

        for (layer in 0 until board.maxLayers) {
            for (y in 0 until board.height) {
                for (x in 0 until board.width) {
                    val tile = board.getTileAt(x, y, layer) ?: continue
                    drawTile(canvas, tile, layer)
                }
            }
        }

        if (!pulseAnimator.isRunning && selectedTile != null) {
            pulseAnimator.start()
        } else if (selectedTile == null && pulseAnimator.isRunning) {
            pulseAnimator.cancel()
            selectedPulse = 0f
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
    }

    private fun drawTile(canvas: Canvas, tile: Tile, layer: Int) {
        if (tile.isMatched) return

        val left = boardLeft + tilePadding + tile.x * (tileWidth + tilePadding) + layer * layerOffsetX
        val top = boardTop + tilePadding + tile.y * (tileHeight + tilePadding) - layer * layerOffsetY
        val rect = RectF(left, top, left + tileWidth, top + tileHeight)

        canvas.save()

        if (matchAnimTiles != null) {
            val (a, b) = matchAnimTiles!!
            if (tile.id == a.id || tile.id == b.id) {
                val scale = 1f + matchAnimProgress * 0.15f
                canvas.scale(scale, scale, rect.centerX(), rect.centerY())
                tileBgPaint.alpha = ((1f - matchAnimProgress) * 255).toInt().coerceIn(0, 255)
                tileBorderPaint.alpha = tileBgPaint.alpha
                iconPaint.alpha = tileBgPaint.alpha
                textPaint.alpha = tileBgPaint.alpha
            }
        }

        val shadowRect = RectF(left + 2f, top + 3f, left + tileWidth + 2f, top + tileHeight + 3f)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        val isFree = tile in freeTiles
        val isSelected = tile == selectedTile
        val isHighlighted = tile in highlightedTiles

        if (isFree) {
            tileBgPaint.shader = LinearGradient(
                left, top, left, top + tileHeight,
                Color.parseColor("#FAFAFA"),
                Color.parseColor("#F5F5F5"),
                Shader.TileMode.CLAMP
            )
        } else {
            tileBgPaint.shader = null
            tileBgPaint.color = Color.parseColor("#E0E0E0")
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileBgPaint)

        tileEdgePaint.color = if (isFree) Color.parseColor("#E8E8E8") else Color.parseColor("#D0D0D0")
        val edgeRect = RectF(left, top + tileHeight * 0.85f, left + tileWidth, top + tileHeight + 2f)
        canvas.drawRoundRect(edgeRect, cornerRadius, cornerRadius, tileEdgePaint)

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileBorderPaint)

        if (isHighlighted) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileHighlightBorder)
        }

        if (isSelected) {
            val glowAlpha = (0.4f + selectedPulse * 0.3f)
            tileSelectedBorder.alpha = (glowAlpha * 255).toInt()
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tileSelectedBorder)

            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6F00")
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(8f + selectedPulse * 4f, BlurMaskFilter.Blur.NORMAL)
                alpha = (glowAlpha * 100).toInt()
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
        }

        drawTileIcon(canvas, tile, rect)

        tileBgPaint.alpha = 255
        tileBorderPaint.alpha = 255
        iconPaint.alpha = 255
        textPaint.alpha = 255

        canvas.restore()
    }

    private fun drawTileIcon(canvas: Canvas, tile: Tile, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY() - tileHeight * 0.04f
        val iconSize = tileWidth * 0.55f
        val symbol = tile.symbolId

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

        textPaint.textSize = tileWidth * 0.1f
        textPaint.color = Color.parseColor("#78909C")
        val label = symbol.replace("_", " ")
        if (label.length > 12) textPaint.textSize = tileWidth * 0.08f
        canvas.drawText(label, cx, rect.bottom - tileHeight * 0.06f, textPaint)
    }

    private fun drawDrum(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val w = size * 0.7f
        val h = size * 0.5f
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawOval(RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), iconPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5D4037")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawLine(cx - w * 0.3f, cy, cx + w * 0.3f, cy, linePaint)
        canvas.drawLine(cx, cy - h * 0.35f, cx, cy + h * 0.35f, linePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5D4037")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - w * 0.25f, cy - h * 0.15f, 2f, dotPaint)
        canvas.drawCircle(cx + w * 0.25f, cy + h * 0.15f, 2f, dotPaint)
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

        val domePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8D6E63")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy - size * 0.2f, size * 0.18f, domePaint)

        iconPaint.color = Color.parseColor("#FFD54F")
        canvas.drawRect(RectF(cx - size * 0.04f, cy - size * 0.38f, cx + size * 0.04f, cy - size * 0.22f), iconPaint)
        canvas.drawCircle(cx, cy - size * 0.38f, size * 0.03f, iconPaint)

        iconPaint.color = Color.parseColor("#5D4037")
        canvas.drawRect(RectF(cx - size * 0.1f, cy + size * 0.15f, cx + size * 0.1f, cy + size * 0.4f), iconPaint)

        for (i in -1..1 step 2) {
            canvas.drawRect(RectF(cx + i * size * 0.25f - size * 0.04f, cy + size * 0.08f, cx + i * size * 0.25f + size * 0.04f, cy + size * 0.16f), iconPaint)
        }
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

        val headWrap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy - size * 0.3f, size * 0.06f, headWrap)
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
        val clothPath = Path()
        clothPath.moveTo(cx - size * 0.3f, cy - size * 0.3f)
        clothPath.lineTo(cx + size * 0.3f, cy - size * 0.3f)
        clothPath.lineTo(cx + size * 0.35f, cy + size * 0.35f)
        clothPath.lineTo(cx - size * 0.35f, cy + size * 0.35f)
        clothPath.close()
        canvas.drawPath(clothPath, iconPaint)

        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        for (i in -2..2) {
            val y = cy + i * size * 0.12f
            canvas.drawLine(cx - size * 0.25f, y, cx + size * 0.25f, y, patternPaint)
        }
        for (i in -1..1) {
            val x = cx + i * size * 0.15f
            canvas.drawLine(x, cy - size * 0.25f, x, cy + size * 0.3f, patternPaint)
        }
    }

    private fun drawWater(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#1E88E5")
        val ovalRect = RectF(cx - size * 0.3f, cy - size * 0.15f, cx + size * 0.3f, cy + size * 0.15f)
        canvas.drawOval(ovalRect, iconPaint)

        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64B5F6")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val wavePath = Path()
        wavePath.moveTo(cx - size * 0.2f, cy)
        wavePath.quadTo(cx - size * 0.1f, cy - size * 0.06f, cx, cy)
        wavePath.quadTo(cx + size * 0.1f, cy + size * 0.06f, cx + size * 0.2f, cy)
        canvas.drawPath(wavePath, wavePaint)

        iconPaint.color = Color.parseColor("#A1887F")
        canvas.drawRect(RectF(cx - size * 0.06f, cy - size * 0.35f, cx + size * 0.06f, cy - size * 0.15f), iconPaint)
        canvas.drawRect(RectF(cx - size * 0.15f, cy - size * 0.35f, cx + size * 0.15f, cy - size * 0.3f), iconPaint)
    }

    private fun drawMarket(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val awningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF8F00")
            style = Paint.Style.FILL
        }
        val awningPath = Path()
        awningPath.moveTo(cx - size * 0.35f, cy - size * 0.05f)
        awningPath.lineTo(cx + size * 0.35f, cy - size * 0.05f)
        awningPath.lineTo(cx + size * 0.4f, cy - size * 0.2f)
        awningPath.lineTo(cx - size * 0.4f, cy - size * 0.2f)
        awningPath.close()
        canvas.drawPath(awningPath, awningPaint)

        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawRect(RectF(cx - size * 0.3f, cy - size * 0.05f, cx + size * 0.3f, cy + size * 0.35f), iconPaint)

        iconPaint.color = Color.parseColor("#FFD54F")
        canvas.drawRect(RectF(cx - size * 0.2f, cy + size * 0.05f, cx - size * 0.05f, cy + size * 0.25f), iconPaint)
        canvas.drawRect(RectF(cx + size * 0.05f, cy + size * 0.05f, cx + size * 0.2f, cy + size * 0.25f), iconPaint)
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
        for (i in 0..2) {
            val x = cx + (i - 1) * size * 0.18f
            canvas.drawCircle(x, cy - size * 0.05f, size * 0.1f, iconPaint)
        }
        iconPaint.color = Color.parseColor("#795548")
        canvas.drawRect(RectF(cx - size * 0.03f, cy + size * 0.1f, cx + size * 0.03f, cy + size * 0.25f), iconPaint)
    }

    private fun drawHousehold(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        iconPaint.color = Color.parseColor("#8D6E63")
        canvas.drawRect(RectF(cx - size * 0.2f, cy - size * 0.1f, cx + size * 0.2f, cy + size * 0.25f), iconPaint)

        val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5D4037")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(cx - size * 0.2f, cy - size * 0.1f, cx + size * 0.2f, cy - size * 0.1f, lidPaint)

        iconPaint.color = Color.parseColor("#FF8F00")
        canvas.drawRect(RectF(cx + size * 0.15f, cy - size * 0.02f, cx + size * 0.28f, cy + size * 0.02f), iconPaint)
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

        iconPaint.color = Color.parseColor("#E53935")
        val flagPath = Path()
        flagPath.moveTo(cx, cy - size * 0.05f)
        flagPath.lineTo(cx, cy - size * 0.35f)
        canvas.drawPath(flagPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#795548"); style = Paint.Style.STROKE; strokeWidth = 2f })
        canvas.drawRect(RectF(cx, cy - size * 0.35f, cx + size * 0.15f, cy - size * 0.25f), iconPaint)
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
        canvas.drawRoundRect(
            RectF(cx - size * 0.3f, cy - size * 0.3f, cx + size * 0.3f, cy + size * 0.3f),
            8f, 8f, iconPaint
        )

        val charPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.35f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
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
        soundManager?.tap()
        if (selectedTile == null) {
            selectedTile = tile
            soundManager?.select()
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
                soundManager?.mismatch()
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

    fun animateMatch(tileA: Tile, tileB: Tile, onComplete: () -> Unit) {
        matchAnimTiles = tileA to tileB
        matchAnimProgress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                matchAnimProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    matchAnimTiles = null
                    matchAnimProgress = 0f
                    selectedTile = null
                    highlightedTiles = emptyList()
                    freeTiles = MatchEngine.getFreeTiles(board!!)
                    invalidate()
                    onComplete()
                }
            })
            start()
        }
    }

    fun animateWrongMatch(tileA: Tile, tileB: Tile, onComplete: () -> Unit) {
        val origX = tileA.x
        postDelayed({
            invalidate()
            onComplete()
        }, 400)
    }

    fun showHint(tiles: List<Tile>) {
        highlightTiles(tiles)
        soundManager?.hint()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
}
