package com.nakudin.hausamahjong.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.AchievementManager
import com.nakudin.hausamahjong.data.CoinManager
import com.nakudin.hausamahjong.data.DailyRewardManager
import com.nakudin.hausamahjong.data.LevelProgressManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvLevel: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var tvStreak: TextView
    private lateinit var layoutAchievements: LinearLayout

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

        setContentView(R.layout.activity_profile)

        btnBack = findViewById(R.id.btnBack)
        tvLevel = findViewById(R.id.tvLevel)
        tvCoins = findViewById(R.id.tvCoins)
        tvCompleted = findViewById(R.id.tvCompleted)
        tvStreak = findViewById(R.id.tvStreak)
        layoutAchievements = findViewById(R.id.layoutAchievements)

        btnBack.setOnClickListener { finish() }

        updateStats()
        populateAchievements()
    }

    private fun updateStats() {
        val currentLevel = LevelProgressManager.getCurrentLevel()
        val completed = LevelProgressManager.getHighestCompleted()
        val coins = CoinManager.getCoins()
        val streak = DailyRewardManager.getStreak()

        tvLevel.text = currentLevel.toString()
        tvCoins.text = CoinManager.formatCoins(coins)
        tvCompleted.text = "$completed / 500"
        tvStreak.text = "$streak days"
    }

    private fun populateAchievements() {
        layoutAchievements.removeAllViews()

        for (achievement in AchievementManager.getAllAchievements()) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_achievement, layoutAchievements, false)
            val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
            val tvName = view.findViewById<TextView>(R.id.tvName)
            val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
            val tvProgress = view.findViewById<TextView>(R.id.tvProgress)
            val tvReward = view.findViewById<TextView>(R.id.tvReward)

            val unlocked = AchievementManager.isUnlocked(achievement)
            val progress = AchievementManager.getProgress(achievement)

            tvName.text = achievement.title
            tvDescription.text = achievement.description

            if (unlocked) {
                tvProgress.text = "✓"
                tvProgress.setTextColor(0xFF4CAF50.toInt())
                view.alpha = 0.6f
            } else {
                tvProgress.text = "$progress / ${achievement.target}"
                tvProgress.setTextColor(0xFFFFD54F.toInt())
            }
            tvReward.text = "+${CoinManager.formatCoins(achievement.reward)}"
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
        populateAchievements()
    }
}