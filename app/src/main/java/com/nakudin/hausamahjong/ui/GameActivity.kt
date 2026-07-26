package com.nakudin.hausamahjong.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.ads.AdManager
import com.nakudin.hausamahjong.ads.PurchaseManager
import com.nakudin.hausamahjong.audio.SoundManager
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.game.Board
import com.nakudin.hausamahjong.game.GameState
import com.nakudin.hausamahjong.game.LevelLoader
import com.nakudin.hausamahjong.game.MatchEngine
import com.nakudin.hausamahjong.game.Tile

class GameActivity : AppCompatActivity(), BoardView.OnTileClickListener {

    private lateinit var boardView: BoardView
    private lateinit var tvLevel: TextView
    private lateinit var tvMoves: TextView
    private lateinit var tvHints: TextView
    private lateinit var btnHint: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnSettings: ImageButton

    private var board: Board? = null
    private var gameState: GameState? = null
    private var levelNumber = 1

    private var adManager: AdManager? = null
    private var purchaseManager: PurchaseManager? = null
    private var soundManager: SoundManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_game)

        val app = application as com.nakudin.hausamahjong.HausaMahjongApplication
        try { app.ensureAdsInitialized() } catch (_: Throwable) {}
        try { app.ensureBillingInitialized() } catch (_: Throwable) {}
        adManager = app.adManager
        purchaseManager = app.purchaseManager

        soundManager = SoundManager(this).apply { init() }

        levelNumber = intent.getIntExtra("LEVEL_NUMBER", 1)
        initViews()
        boardView.soundManager = soundManager
        try {
            loadLevel()
        } catch (e: Throwable) {
            android.widget.Toast.makeText(this, "Failed to load level: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setupClickListeners()
    }

    private fun initViews() {
        boardView = findViewById(R.id.boardView)
        tvLevel = findViewById(R.id.tvLevel)
        tvMoves = findViewById(R.id.tvMoves)
        tvHints = findViewById(R.id.tvHints)
        btnHint = findViewById(R.id.btnHint)
        btnUndo = findViewById(R.id.btnUndo)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun loadLevel() {
        val levelData = LevelRepository.getLevel(levelNumber)
        if (levelData == null) {
            finish()
            return
        }

        board = LevelLoader.loadBoard(levelData)
        gameState = GameState(levelNumber = levelNumber)

        tvLevel.text = getString(R.string.level) + " $levelNumber"
        updateUI()

        boardView.setBoard(board!!)
        boardView.setOnTileClickListener(this)
    }

    private fun setupClickListeners() {
        btnHint.setOnClickListener {
            soundManager?.button()
            showHintDialog()
        }
        btnUndo.setOnClickListener {
            soundManager?.button()
            performUndo()
        }
        btnSettings.setOnClickListener {
            soundManager?.button()
            showPauseDialog()
        }
    }

    private fun updateUI() {
        gameState?.let { state ->
            tvMoves.text = state.moves.toString()
            tvHints.text = (3 - state.hintsUsed).toString()
        }
    }

    override fun onTileClicked(tile: Tile) {
        if (!tile.isFaceUp) return

        val slotTiles = boardView.getSlotTiles()
        val existingMatch = slotTiles.find { it.symbolId == tile.symbolId }

        if (existingMatch != null) {
            val boardX = boardLeftForTile(tile)
            val boardY = boardTopForTile(tile)

            board!!.removeTiles(tile, existingMatch)
            gameState?.recordMatch(tile, existingMatch, board!!)
            updateUI()

            boardView.removeFromSlot(existingMatch)
            soundManager?.tileBreak()
            boardView.animateBreak(tile, existingMatch) {
                boardView.refreshFreeTiles()
                checkGameState()
            }
        } else {
            if (slotTiles.size >= 7) {
                android.widget.Toast.makeText(this, "Slot is full!", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            boardView.addToSlot(tile)
            soundManager?.tilePlace()

            val boardX = boardLeftForTile(tile)
            val boardY = boardTopForTile(tile)
            boardView.animateTileToSlot(tile, boardX, boardY) {
                boardView.invalidate()
            }
        }
    }

    override fun onSlotTileClicked(tile: Tile) {
        val otherMatch = boardView.getSlotTiles().find {
            it.id != tile.id && it.symbolId == tile.symbolId
        }

        if (otherMatch != null) {
            boardView.removeFromSlot(tile)
            boardView.removeFromSlot(otherMatch)

            val boardTileA = board!!.getAllTiles().find { it.id == tile.id }
            val boardTileB = board!!.getAllTiles().find { it.id == otherMatch.id }
            if (boardTileA != null && boardTileB != null) {
                board!!.removeTiles(boardTileA, boardTileB)
                gameState?.recordMatch(boardTileA, boardTileB, board!!)
                updateUI()

                soundManager?.tileBreak()
                boardView.animateBreak(boardTileA, boardTileB) {
                    boardView.refreshFreeTiles()
                    checkGameState()
                }
            }
        } else {
            boardView.removeFromSlot(tile)
            soundManager?.button()
            boardView.invalidate()
        }
    }

    private fun boardLeftForTile(tile: Tile): Float {
        val board = board ?: return 0f
        val hudTopHeight = 56f * resources.displayMetrics.density
        val hudBottomHeight = 80f * resources.displayMetrics.density
        val slotAreaHeight = 80f * resources.displayMetrics.density
        val usableWidth = boardView.width.toFloat()
        val usableHeight = boardView.height.toFloat() - hudTopHeight - hudBottomHeight - slotAreaHeight
        val tw = (usableWidth - (board.width + 1) * 4f) / board.width
        val th = tw * 1.25f
        val tp = tw * 0.06f
        val lox = tw * 0.07f
        val loy = th * 0.07f
        val totalW = board.width * (tw + tp) + tp
        val totalH = board.height * (th + tp) + tp + board.maxLayers * loy
        val bl = (usableWidth - totalW) / 2f
        val bt = hudTopHeight + slotAreaHeight + (usableHeight - totalH) / 2f + loy * board.maxLayers
        return bl + tp + tile.x * (tw + tp) + tile.layer * lox + tw / 2f
    }

    private fun boardTopForTile(tile: Tile): Float {
        val board = board ?: return 0f
        val hudTopHeight = 56f * resources.displayMetrics.density
        val hudBottomHeight = 80f * resources.displayMetrics.density
        val slotAreaHeight = 80f * resources.displayMetrics.density
        val usableWidth = boardView.width.toFloat()
        val usableHeight = boardView.height.toFloat() - hudTopHeight - hudBottomHeight - slotAreaHeight
        val tw = (usableWidth - (board.width + 1) * 4f) / board.width
        val th = tw * 1.25f
        val tp = tw * 0.06f
        val lox = tw * 0.07f
        val loy = th * 0.07f
        val totalW = board.width * (tw + tp) + tp
        val totalH = board.height * (th + tp) + tp + board.maxLayers * loy
        val bl = (usableWidth - totalW) / 2f
        val bt = hudTopHeight + slotAreaHeight + (usableHeight - totalH) / 2f + loy * board.maxLayers
        return bt + tp + tile.y * (th + tp) - tile.layer * loy + th / 2f
    }

    private fun checkGameState() {
        if (board!!.isCleared()) {
            gameState?.isComplete = true
            soundManager?.win()
            showWinDialog()
        } else if (boardView.getSlotTiles().size >= 7 && MatchEngine.findMatchingPair(board!!) == null) {
            gameState?.isFailed = true
            soundManager?.lose()
            showLoseDialog()
        } else if (MatchEngine.getFreeTiles(board!!).isEmpty() && boardView.getSlotTiles().isEmpty()) {
            gameState?.isFailed = true
            soundManager?.lose()
            showLoseDialog()
        }
    }

    private fun performUndo() {
        if (gameState?.canUndo() == true) {
            val undone = gameState?.undo(board!!)
            if (undone != null) {
                updateUI()
                boardView.setBoard(board!!)
                soundManager?.tap()
            }
        }
    }

    private fun showHintDialog() {
        if (gameState?.canUseHint() != true) {
            showAdForHintDialog()
            return
        }

        val pair = MatchEngine.getHint(board!!)
        if (pair != null) {
            gameState?.useHint()
            updateUI()
            boardView.highlightTiles(listOf(pair.first, pair.second))
        }
    }

    private fun showAdForHintDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ad_hint)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        val btnWatch = dialog.findViewById<Button>(R.id.btnWatch)
        val btnNoThanks = dialog.findViewById<Button>(R.id.btnNoThanks)

        tvMessage.text = getString(R.string.watch_ad_for_hint)

        btnWatch.setOnClickListener {
            dialog.dismiss()
            if (adManager?.isRewardedAdReady() == true) {
                adManager?.showRewardedAd(this) {
                    val pair = MatchEngine.getHint(board!!)
                    if (pair != null) {
                        boardView.highlightTiles(listOf(pair.first, pair.second))
                    }
                }
            } else {
                android.widget.Toast.makeText(this, R.string.ad_not_ready, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        btnNoThanks.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPauseDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pause)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnResume = dialog.findViewById<Button>(R.id.btnResume)
        val btnRestart = dialog.findViewById<Button>(R.id.btnRestart)
        val btnMenu = dialog.findViewById<Button>(R.id.btnMenu)

        btnResume.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
        }

        btnRestart.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            restartLevel()
        }

        btnMenu.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showWinDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_win)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvMoves = dialog.findViewById<TextView>(R.id.tvMoves)
        val tvScore = dialog.findViewById<TextView>(R.id.tvScore)
        val tvTime = dialog.findViewById<TextView>(R.id.tvTime)
        val btnNext = dialog.findViewById<Button>(R.id.btnNext)
        val btnMenu = dialog.findViewById<Button>(R.id.btnMenu)

        tvTitle.text = getString(R.string.win)
        tvMoves.text = getString(R.string.moves) + ": ${gameState?.moves}"
        tvScore.text = getString(R.string.score) + ": ${gameState?.score}"
        tvTime.visibility = View.GONE

        btnNext.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            levelNumber++
            loadLevel()
        }

        btnMenu.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            finish()
        }

        dialog.show()

        if (adManager?.shouldShowInterstitialAfterLevel() == true && adManager?.isInterstitialAdReady() == true) {
            adManager?.showInterstitialAd(this)
        }
    }

    private fun showLoseDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_lose)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val btnTryAgain = dialog.findViewById<Button>(R.id.btnTryAgain)
        val btnMenu = dialog.findViewById<Button>(R.id.btnMenu)

        tvTitle.text = getString(R.string.lose)

        btnTryAgain.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            restartLevel()
        }

        btnMenu.setOnClickListener {
            soundManager?.button()
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun restartLevel() {
        loadLevel()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.release()
    }
}
