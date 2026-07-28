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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.ads.AdManager
import com.nakudin.hausamahjong.ads.PurchaseManager
import com.nakudin.hausamahjong.audio.SoundManager
import com.nakudin.hausamahjong.data.AchievementManager
import com.nakudin.hausamahjong.data.CoinManager
import com.nakudin.hausamahjong.data.CoinRewards
import com.nakudin.hausamahjong.data.LevelProgressManager
import com.nakudin.hausamahjong.data.LevelRepository
import com.nakudin.hausamahjong.game.Board
import com.nakudin.hausamahjong.game.GameState
import com.nakudin.hausamahjong.game.LevelLoader
import com.nakudin.hausamahjong.game.MatchEngine
import com.nakudin.hausamahjong.game.Tile

class GameActivity : AppCompatActivity(), BoardView.OnTileClickListener, CoinManager.OnCoinChangeListener, AchievementManager.OnAchievementUnlockListener {

    private lateinit var boardView: BoardView
    private lateinit var tvLevel: TextView
    private lateinit var tvLevelNumber: TextView
    private lateinit var tvMoves: TextView
    private lateinit var tvHints: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvCoins: TextView
    private lateinit var btnHint: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnMenuBack: ImageButton

    private var board: Board? = null
    private var gameState: GameState? = null
    private var levelNumber = 1

    private var adManager: AdManager? = null
    private var purchaseManager: PurchaseManager? = null
    private var soundManager: SoundManager? = null

    private var comboCount = 0
    private var lastMatchTime = 0L

    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            gameState?.let {
                val elapsed = it.getElapsedTime() / 1000
                val m = elapsed / 60
                val s = elapsed % 60
                tvTimer.text = "$m:${"%02d".format(s)}"
            }
            timerHandler.postDelayed(this, 1000)
        }
    }

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

        levelNumber = intent.getIntExtra("LEVEL_NUMBER", 0)
        if (levelNumber <= 0) {
            levelNumber = LevelProgressManager.getCurrentLevel()
        }
        initViews()
        boardView.soundManager = soundManager
        try {
            loadLevel()
        } catch (e: Throwable) {
            Toast.makeText(this, "Failed to load level: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setupClickListeners()
    }

    private fun initViews() {
        boardView = findViewById(R.id.boardView)
        tvLevel = findViewById(R.id.tvLevel)
        tvLevelNumber = findViewById(R.id.tvLevelNumber)
        tvMoves = findViewById(R.id.tvMoves)
        tvHints = findViewById(R.id.tvHints)
        tvTimer = findViewById(R.id.tvTimer)
        tvCoins = findViewById(R.id.tvCoins)
        btnHint = findViewById(R.id.btnHint)
        btnUndo = findViewById(R.id.btnUndo)
        btnMenuBack = findViewById(R.id.btnMenuBack)

        CoinManager.addListener(this)
        AchievementManager.addListener(this)
        updateCoinDisplay()
    }

    private fun loadLevel() {
        val levelData = LevelRepository.getLevel(levelNumber)
        if (levelData == null) {
            finish()
            return
        }

        board = LevelLoader.loadBoard(levelData)
        gameState = GameState(levelNumber = levelNumber)

        comboCount = 0
        lastMatchTime = 0L

        tvLevelNumber.text = levelNumber.toString()
        updateUI()

        boardView.setBoard(board!!)
        boardView.setOnTileClickListener(this)

        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)
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
        btnMenuBack.setOnClickListener {
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

    private fun updateCoinDisplay() {
        tvCoins.text = CoinManager.formatCoins(CoinManager.getCoins())
    }

    override fun onCoinsChanged(newAmount: Int, change: Int) {
        runOnUiThread {
            updateCoinDisplay()
            if (change > 0) {
                animateCoinGain(change)
            }
        }
    }

    private fun animateCoinGain(amount: Int) {
        tvCoins.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(150)
            .withEndAction {
                tvCoins.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()

        val popup = TextView(this).apply {
            text = "+${CoinManager.formatCoins(amount)}"
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#FFD54F"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
        val container = findViewById<android.view.ViewGroup>(R.id.coinContainer)
        container.addView(popup)
        popup.animate()
            .translationY(-80f)
            .alpha(0f)
            .setDuration(800)
            .withEndAction { container.removeView(popup) }
            .start()
    }

    override fun onTileClicked(tile: Tile) {
        if (!tile.isFaceUp) {
            gameState?.recordFlip()
            CoinManager.addCoins(CoinRewards.FLIP_BONUS, "flip_bonus")
            checkFlipAchievements()
            return
        }

        val slotTiles = boardView.getSlotTiles()
        val existingMatch = slotTiles.find { it.symbolId == tile.symbolId }

        if (existingMatch != null) {
            board!!.removeTiles(tile, existingMatch)
            gameState?.recordMatch(tile, existingMatch, board!!)
            updateUI()
            handleMatch()

            boardView.removeFromSlot(existingMatch)
            soundManager?.tileBreak()
            boardView.animateBreak(tile, existingMatch) {
                boardView.refreshFreeTiles()
                checkGameState()
            }
        } else {
            if (slotTiles.size >= 4) {
                showSlotOverflowDialog()
                return
            }

            boardView.addToSlot(tile)
            soundManager?.tilePlace()

            val boardX = boardLeftForTile(tile)
            val boardY = boardTopForTile(tile)
            boardView.animateTileToSlot(tile, boardX, boardY) {
                boardView.invalidate()
                if (boardView.getSlotTiles().size >= 4) {
                    checkSlotOverflow()
                }
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

            board!!.removeTiles(tile, otherMatch)
            gameState?.recordMatch(tile, otherMatch, board!!)
            updateUI()
            handleMatch()

            soundManager?.tileBreak()
            boardView.animateBreak(tile, otherMatch) {
                boardView.refreshFreeTiles()
                checkGameState()
            }
        } else {
            boardView.removeFromSlot(tile)
            soundManager?.button()
            boardView.invalidate()
        }
    }

    private fun handleMatch() {
        val now = System.currentTimeMillis()
        if (now - lastMatchTime < 2000) {
            comboCount++
        } else {
            comboCount = 1
        }
        lastMatchTime = now

        when (comboCount) {
            2 -> CoinManager.addCoins(CoinRewards.COMBO_2, "combo_x2")
            3 -> CoinManager.addCoins(CoinRewards.COMBO_3, "combo_x3")
            4 -> CoinManager.addCoins(CoinRewards.COMBO_4, "combo_x4")
            in 5..Int.MAX_VALUE -> CoinManager.addCoins(CoinRewards.COMBO_5_PLUS, "combo_x5+")
        }

        if (comboCount >= 2) {
            showComboPopup(comboCount)
        }
    }

    private fun showComboPopup(count: Int) {
        val popup = TextView(this).apply {
            text = "COMBO x$count!"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#FFD54F"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
        }
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        root.addView(popup, params)

        popup.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(1f)
            .setDuration(200)
            .withEndAction {
                popup.animate()
                    .translationY(-150f)
                    .alpha(0f)
                    .setDuration(800)
                    .withEndAction { root.removeView(popup) }
                    .start()
            }
            .start()
    }

    private fun boardLeftForTile(tile: Tile): Float = boardView.getTileCenterX(tile)

    private fun boardTopForTile(tile: Tile): Float = boardView.getTileCenterY(tile)

    private fun checkGameState() {
        val b = board ?: return

        if (b.isCleared()) {
            gameState?.isComplete = true
            soundManager?.win()
            showWinDialog()
            return
        }

        val slotTiles = boardView.getSlotTiles()
        val noFreeMoves = MatchEngine.findMatchingPair(b) == null
        val slotFull = slotTiles.size >= 4

        if (slotFull && noFreeMoves) {
            checkSlotOverflow()
            return
        }

        if (MatchEngine.getFreeTiles(b).isEmpty() && slotTiles.isEmpty()) {
            gameState?.isFailed = true
            soundManager?.lose()
            showLoseDialog()
            return
        }

        // Offer shuffle when stuck (no matching pairs on board)
        if (noFreeMoves && slotTiles.isNotEmpty()) {
            offerShuffleDialog()
        }
    }

    private fun checkSlotOverflow() {
        soundManager?.lose()
        showSlotOverflowDialog()
    }

    private var slotOverflowDialog: Dialog? = null

    private fun showSlotOverflowDialog() {
        if (slotOverflowDialog?.isShowing == true) return
        slotOverflowDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_slot_overflow)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val btnReturn = findViewById<Button>(R.id.btnReturnCoins)
            val btnWatch = findViewById<Button>(R.id.btnWatchAd)
            val btnGiveUp = findViewById<Button>(R.id.btnGiveUp)

            btnReturn.text = "Return All (₦${CoinRewards.SLOT_RETURN_COST})"
            btnReturn.isEnabled = CoinManager.canAfford(CoinRewards.SLOT_RETURN_COST)

            btnReturn.setOnClickListener {
                soundManager?.button()
                dismiss()
                gameState?.isFailed = false
                CoinManager.spendCoins(CoinRewards.SLOT_RETURN_COST)
                boardView.returnAllSlotTiles()
                boardView.invalidate()
                Toast.makeText(this@GameActivity, "Tiles returned! -${CoinManager.formatCoins(CoinRewards.SLOT_RETURN_COST)}", Toast.LENGTH_SHORT).show()
            }

            btnWatch.setOnClickListener {
                soundManager?.button()
                dismiss()
                if (adManager?.isRewardedAdReady() == true) {
                    adManager?.showRewardedAd(this@GameActivity) {
                        gameState?.isFailed = false
                        boardView.returnAllSlotTiles()
                        boardView.invalidate()
                        Toast.makeText(this@GameActivity, "Tiles returned! Thanks for watching.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@GameActivity, "Ad not ready", Toast.LENGTH_SHORT).show()
                    showSlotOverflowDialog()
                }
            }

            btnGiveUp.setOnClickListener {
                soundManager?.button()
                dismiss()
                gameState?.isFailed = true
                showLoseDialog()
            }

            show()
        }
    }

    private fun requestShuffle() {
        val b = board ?: return
        if (CoinManager.canAfford(CoinRewards.SHUFFLE_COST)) {
            CoinManager.spendCoins(CoinRewards.SHUFFLE_COST)
            b.shuffleRemaining()
            boardView.setBoard(b)
            boardView.refreshFreeTiles()
            Toast.makeText(this, "Board shuffled! -${CoinManager.formatCoins(CoinRewards.SHUFFLE_COST)}", Toast.LENGTH_SHORT).show()
        } else {
            // Try watch ad for shuffle
            if (adManager?.isRewardedAdReady() == true) {
                adManager?.showRewardedAd(this) {
                    b.shuffleRemaining()
                    boardView.setBoard(b)
                    boardView.refreshFreeTiles()
                    Toast.makeText(this, "Board shuffled! Thanks for watching.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Need ${CoinManager.formatCoins(CoinRewards.SHUFFLE_COST)} to shuffle or watch an ad", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var shuffleDialog: Dialog? = null

    private fun offerShuffleDialog() {
        if (shuffleDialog?.isShowing == true) return
        shuffleDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_shuffle)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val btnShuffle = findViewById<Button>(R.id.btnShuffleAction)
            val btnWatch = findViewById<Button>(R.id.btnWatchAd)
            val btnCancel = findViewById<Button>(R.id.btnCancel)

            btnShuffle.text = "Shuffle (₦${CoinRewards.SHUFFLE_COST})"
            btnShuffle.isEnabled = CoinManager.canAfford(CoinRewards.SHUFFLE_COST)

            btnShuffle.setOnClickListener {
                soundManager?.button()
                dismiss()
                requestShuffle()
            }

            btnWatch.setOnClickListener {
                soundManager?.button()
                dismiss()
                if (adManager?.isRewardedAdReady() == true) {
                    adManager?.showRewardedAd(this@GameActivity) {
                        val b = board ?: return@showRewardedAd
                        b.shuffleRemaining()
                        boardView.setBoard(b)
                        boardView.refreshFreeTiles()
                        Toast.makeText(this@GameActivity, "Board shuffled!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@GameActivity, "Ad not ready", Toast.LENGTH_SHORT).show()
                }
            }

            btnCancel.setOnClickListener {
                soundManager?.button()
                dismiss()
            }

            show()
        }
    }

    private fun performUndo() {
        if (gameState?.canUndo() == true) {
            val undone = gameState?.undo(board!!)
            if (undone != null) {
                gameState?.recordUndo()
                MatchEngine.invalidateCache()
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
            // Exclude slotted tiles from hint
            if (pair.first.isInSlot || pair.second.isInSlot) {
                val altPair = MatchEngine.findMatchingPair(board!!, false)
                if (altPair != null && !altPair.first.isInSlot && !altPair.second.isInSlot) {
                    gameState?.useHint()
                    updateUI()
                    boardView.highlightTiles(listOf(altPair.first, altPair.second))
                    return
                }
            }
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
                Toast.makeText(this, R.string.ad_not_ready, Toast.LENGTH_SHORT).show()
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
        dialog.setContentView(R.layout.dialog_win_rewards)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvLevelSub = dialog.findViewById<TextView>(R.id.tvLevelSub)
        val tvMoves = dialog.findViewById<TextView>(R.id.tvMoves)
        val tvScore = dialog.findViewById<TextView>(R.id.tvScore)
        val tvTime = dialog.findViewById<TextView>(R.id.tvTime)
        val btnNext = dialog.findViewById<Button>(R.id.btnNext)
        val btnMenu = dialog.findViewById<Button>(R.id.btnMenu)

        timerHandler.removeCallbacks(timerRunnable)

        val tvBaseReward = dialog.findViewById<TextView>(R.id.tvBaseReward)
        val tvComboReward = dialog.findViewById<TextView>(R.id.tvComboReward)
        val tvNoHintReward = dialog.findViewById<TextView>(R.id.tvNoHintReward)
        val tvNoUndoReward = dialog.findViewById<TextView>(R.id.tvNoUndoReward)
        val tvSpeedReward = dialog.findViewById<TextView>(R.id.tvSpeedReward)
        val tvTotalReward = dialog.findViewById<TextView>(R.id.tvTotalReward)

        val state = gameState ?: return
        val isGameComplete = LevelProgressManager.isGameComplete()

        if (isGameComplete) {
            tvTitle.text = "Game Complete!"
            btnNext.text = "Restart from Level 1"
            AchievementManager.unlockAchievement(this, "complete_all_levels")
            btnNext.setOnClickListener {
                soundManager?.button()
                dialog.dismiss()
                LevelProgressManager.resetProgress()
                levelNumber = 1
                loadLevel()
            }
        } else {
            tvTitle.text = getString(R.string.win)
            tvLevelSub.text = "Level $levelNumber"
            tvMoves.text = getString(R.string.moves) + ": ${state.moves}"
            tvScore.text = getString(R.string.score) + ": ${state.score}"

            val timeElapsed = state.getElapsedTime() / 1000
            val minutes = timeElapsed / 60
            val seconds = timeElapsed % 60
            tvTime.text = "$minutes:${"%02d".format(seconds)}"

            LevelProgressManager.setHighestCompleted(levelNumber)

            val baseReward = CoinRewards.LEVEL_BASE + (CoinRewards.LEVEL_PER_DIFFICULTY * levelNumber / 10)
            val comboReward = when (comboCount) {
                2 -> CoinRewards.COMBO_2
                3 -> CoinRewards.COMBO_3
                4 -> CoinRewards.COMBO_4
                in 5..Int.MAX_VALUE -> CoinRewards.COMBO_5_PLUS
                else -> 0
            }
            val flipReward = state.flipsUsed * CoinRewards.FLIP_BONUS
            var noHintReward = 0
            var noUndoReward = 0
            var speedReward = 0

            if (state.noHint) noHintReward = CoinRewards.NO_HINTS
            if (state.noUndo) noUndoReward = CoinRewards.NO_UNDO
            if (timeElapsed < 60) speedReward = CoinRewards.FAST_CLEAR_UNDER_60
            else if (timeElapsed < 120) speedReward = CoinRewards.FAST_CLEAR_UNDER_120

            val totalReward = baseReward + comboReward + flipReward + noHintReward + noUndoReward + speedReward

            tvBaseReward.text = "+${CoinManager.formatCoins(baseReward)}"
            tvComboReward.text = "+${CoinManager.formatCoins(comboReward)}"
            tvNoHintReward.text = "+${CoinManager.formatCoins(noHintReward)}"
            tvNoUndoReward.text = "+${CoinManager.formatCoins(noUndoReward)}"
            tvSpeedReward.text = "+${CoinManager.formatCoins(speedReward)}"
            tvTotalReward.text = CoinManager.formatCoins(totalReward)

            CoinManager.addCoins(totalReward, "level_complete_$levelNumber")

            checkLevelAchievements()
            checkWinAchievements(timeElapsed, state.noHint, state.noUndo)

            val rewardItems = listOf(
                dialog.findViewById<View>(R.id.rowBase),
                dialog.findViewById<View>(R.id.rowCombo),
                dialog.findViewById<View>(R.id.rowNoHint),
                dialog.findViewById<View>(R.id.rowNoUndo),
                dialog.findViewById<View>(R.id.rowSpeed),
                dialog.findViewById<View>(R.id.rowTotal)
            )

            rewardItems.forEachIndexed { index, item ->
                item.alpha = 0f
                item.translationY = 30f
                item.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(index * 150L)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
            }

            btnNext.setOnClickListener {
                soundManager?.button()
                dialog.dismiss()
                levelNumber++
                loadLevel()
            }
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

    override fun onAchievementUnlocked(achievement: AchievementManager.Achievement) {
        // Toast is shown by AchievementManager
    }

    private fun checkFlipAchievements() {
        val flips = gameState?.flipsUsed ?: 0
        AchievementManager.updateProgress(this, "flip_10", flips)
        AchievementManager.updateProgress(this, "flip_50", flips)
        AchievementManager.updateProgress(this, "flip_100", flips)
        AchievementManager.updateProgress(this, "flip_500", flips)
    }

    private fun checkLevelAchievements() {
        AchievementManager.updateProgress(this, "level_10", levelNumber)
        AchievementManager.updateProgress(this, "level_25", levelNumber)
        AchievementManager.updateProgress(this, "level_50", levelNumber)
        AchievementManager.updateProgress(this, "level_100", levelNumber)
        AchievementManager.updateProgress(this, "level_250", levelNumber)
        AchievementManager.updateProgress(this, "level_500", levelNumber)
    }

    private fun checkWinAchievements(timeElapsed: Long, noHint: Boolean, noUndo: Boolean) {
        val moves = gameState?.moves ?: 0
        AchievementManager.updateProgress(this, "match_100", moves)
        AchievementManager.updateProgress(this, "match_500", moves)
        AchievementManager.updateProgress(this, "match_1000", moves)

        if (comboCount >= 2) {
            AchievementManager.unlockAchievement(this, "combo_2")
            if (comboCount >= 5) AchievementManager.unlockAchievement(this, "combo_5")
            if (comboCount >= 10) AchievementManager.unlockAchievement(this, "combo_10")
        }

        if (timeElapsed < 60) {
            AchievementManager.unlockAchievement(this, "speed_60")
        }
        if (timeElapsed < 30) {
            AchievementManager.unlockAchievement(this, "speed_30")
        }

        if (noHint && noUndo) {
            AchievementManager.unlockAchievement(this, "perfect_clear")
            CoinManager.addCoins(CoinRewards.PERFECT_CLEAR, "perfect_clear_$levelNumber")
        }

        val totalCoins = CoinManager.getBalance()
        AchievementManager.updateProgress(this, "coin_1000", totalCoins)
        AchievementManager.updateProgress(this, "coin_5000", totalCoins)
        AchievementManager.updateProgress(this, "coin_10000", totalCoins)
        AchievementManager.updateProgress(this, "coin_50000", totalCoins)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        soundManager?.release()
    }
}