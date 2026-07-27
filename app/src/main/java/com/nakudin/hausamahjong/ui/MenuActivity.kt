package com.nakudin.hausamahjong.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.CoinManager
import com.nakudin.hausamahjong.data.DailyRewardManager
import com.nakudin.hausamahjong.data.LevelProgressManager

class MenuActivity : AppCompatActivity(), CoinManager.OnCoinChangeListener {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvCurrentLevel: TextView
    private lateinit var tvCoins: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnShop: ImageButton
    private lateinit var btnDailyReward: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var btnSettings: ImageButton

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

        setContentView(R.layout.activity_menu)

        initViews()
        setupClickListeners()
        updateLevelDisplay()
        updateCoinDisplay()
        CoinManager.addListener(this)
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel)
        tvCoins = findViewById(R.id.tvCoins)
        btnPlay = findViewById(R.id.btnPlay)
        btnShop = findViewById(R.id.btnShop)
        btnDailyReward = findViewById(R.id.btnDailyReward)
        btnProfile = findViewById(R.id.btnProfile)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupClickListeners() {
        btnPlay.setOnClickListener {
            if (LevelProgressManager.isGameComplete()) {
                LevelProgressManager.resetProgress()
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("LEVEL_NUMBER", 1)
                startActivity(intent)
            } else {
                val level = LevelProgressManager.getCurrentLevel()
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("LEVEL_NUMBER", level)
                startActivity(intent)
            }
        }

        btnShop.setOnClickListener {
            showShopDialog()
        }

        btnDailyReward.setOnClickListener {
            claimDailyReward()
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showShopDialog() {
        startActivity(Intent(this, ShopActivity::class.java))
    }

    private fun claimDailyReward() {
        if (DailyRewardManager.canClaimToday()) {
            val reward = DailyRewardManager.claimDailyReward()
            Toast.makeText(
                this,
                "Daily Reward: +${CoinManager.formatCoins(reward)}\nStreak: ${DailyRewardManager.getCurrentStreak()} days",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Daily reward already claimed today! Come back tomorrow.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateCoinDisplay() {
        tvCoins.text = CoinManager.formatCoins(CoinManager.getCoins())
    }

    override fun onCoinsChanged(newAmount: Int, change: Int) {
        runOnUiThread { updateCoinDisplay() }
    }

    private fun updateLevelDisplay() {
        val isComplete = LevelProgressManager.isGameComplete()
        if (isComplete) {
            tvCurrentLevel.text = getString(R.string.game_complete)
            tvSubtitle.text = getString(R.string.all_levels_done)
            btnPlay.text = getString(R.string.restart_game)
        } else {
            val currentLevel = LevelProgressManager.getCurrentLevel()
            val completed = LevelProgressManager.getHighestCompleted()
            tvCurrentLevel.text = getString(R.string.current_level, currentLevel)
            if (completed > 0) {
                tvSubtitle.text = getString(R.string.levels_completed, completed)
            }
            btnPlay.text = getString(R.string.play)
        }
    }

    private fun showSettingsDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.setCancelable(true)

        val btnEnglish = dialog.findViewById<Button>(R.id.btnEnglish)
        val btnHausa = dialog.findViewById<Button>(R.id.btnHausa)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        btnEnglish.setOnClickListener {
            setLanguage("en")
            dialog.dismiss()
        }

        btnHausa.setOnClickListener {
            setLanguage("ha")
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setLanguage(lang: String) {
        val config = resources.configuration
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        updateLevelDisplay()
        updateCoinDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        CoinManager.removeListener(this)
    }
}