package com.nakudin.hausamahjong.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
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
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.data.ProverbRepository
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
    private lateinit var tvTime: TextView
    private lateinit var tvSelectedTile: TextView
    private lateinit var btnHint: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnMenu: ImageButton

    private var board: Board? = null
    private var gameState: GameState? = null
    private var levelNumber = 1
    private var timer: CountDownTimer? = null
    private var timeRemaining = 0L

    private lateinit var adManager: AdManager
    private lateinit var purchaseManager: PurchaseManager

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

        adManager = (application as com.nakudin.hausamahjong.HausaMahjongApplication).adManager
        purchaseManager = (application as com.nakudin.hausamahjong.HausaMahjongApplication).purchaseManager

        levelNumber = intent.getIntExtra("LEVEL_NUMBER", 1)
        initViews()
        loadLevel()
        setupClickListeners()
    }

    private fun initViews() {
        boardView = findViewById(R.id.boardView)
        tvLevel = findViewById(R.id.tvLevel)
        tvMoves = findViewById(R.id.tvMoves)
        tvHints = findViewById(R.id.tvHints)
        tvTime = findViewById(R.id.tvTime)
        tvSelectedTile = findViewById(R.id.tvSelectedTile)
        btnHint = findViewById(R.id.btnHint)
        btnUndo = findViewById(R.id.btnUndo)
        btnMenu = findViewById(R.id.btnMenu)
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
        tvSelectedTile.text = ""
        updateUI()

        boardView.setBoard(board!!)
        boardView.setOnTileClickListener(this)

        startTimer()
    }

    private fun setupClickListeners() {
        btnHint.setOnClickListener { showHintDialog() }
        btnUndo.setOnClickListener { performUndo() }
        btnMenu.setOnClickListener { showPauseDialog() }
    }

    private fun startTimer() {
        timeRemaining = 180000L
        timer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                tvTime.text = String.format("%d:%02d", seconds / 60, seconds % 60)
            }

            override fun onFinish() {
                showLoseDialog()
            }
        }.start()
    }

    private fun updateUI() {
        gameState?.let { state ->
            tvMoves.text = state.moves.toString()
            tvHints.text = (3 - state.hintsUsed).toString()
        }
    }

    override fun onTileClicked(tile: Tile) {
        tvSelectedTile.text = tile.symbolId.replace("_", " ").uppercase()
    }

    override fun onMatchAttempt(tileA: Tile, tileB: Tile) {
        if (MatchEngine.canMatch(tileA, tileB, board!!)) {
            board!!.removeTiles(tileA, tileB)
            gameState?.recordMatch(tileA, tileB, board!!)
            updateUI()
            tvSelectedTile.text = ""

            boardView.animateMatch(tileA, tileB) {
                boardView.setBoard(board!!)
                checkGameState()
            }
        } else {
            boardView.animateWrongMatch(tileA, tileB) {
                boardView.clearSelection()
            }
        }
    }

    private fun checkGameState() {
        if (board!!.isCleared()) {
            timer?.cancel()
            gameState?.isComplete = true
            showWinDialog()
        } else if (MatchEngine.getFreeTiles(board!!).isEmpty()) {
            timer?.cancel()
            gameState?.isFailed = true
            showLoseDialog()
        } else if (MatchEngine.findMatchingPair(board!!) == null) {
            timer?.cancel()
            gameState?.isFailed = true
            showLoseDialog()
        }
    }

    private fun performUndo() {
        if (gameState?.canUndo() == true) {
            val undone = gameState?.undo(board!!)
            if (undone != null) {
                updateUI()
                boardView.setBoard(board!!)
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
            boardView.showHint(listOf(pair.first, pair.second))
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
            if (adManager.isRewardedAdReady()) {
                adManager.showRewardedAd(this) {
                    val pair = MatchEngine.getHint(board!!)
                    if (pair != null) {
                        boardView.showHint(listOf(pair.first, pair.second))
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
        timer?.cancel()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pause)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnResume = dialog.findViewById<Button>(R.id.btnResume)
        val btnRestart = dialog.findViewById<Button>(R.id.btnRestart)
        val btnMenu = dialog.findViewById<Button>(R.id.btnMenu)

        btnResume.setOnClickListener {
            dialog.dismiss()
            startTimer()
        }

        btnRestart.setOnClickListener {
            dialog.dismiss()
            restartLevel()
        }

        btnMenu.setOnClickListener {
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
        val seconds = timeRemaining / 1000
        tvTime.text = String.format("%d:%02d", seconds / 60, seconds % 60)

        btnNext.setOnClickListener {
            dialog.dismiss()
            levelNumber++
            loadLevel()
        }

        btnMenu.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()

        if (adManager.shouldShowInterstitialAfterLevel() && adManager.isInterstitialAdReady()) {
            adManager.showInterstitialAd(this)
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
            dialog.dismiss()
            restartLevel()
        }

        btnMenu.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun restartLevel() {
        timer?.cancel()
        loadLevel()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}