package com.nakudin.hausamahjong.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.ProverbRepository

class SplashActivity : AppCompatActivity() {

    private lateinit var ivIcon: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var proverbSection: LinearLayout
    private lateinit var tvProverb: TextView
    private lateinit var tvProverbMeaning: TextView
    private lateinit var loadingBar: View
    private lateinit var loadingBarBg: View
    private lateinit var ivLoadingTile: ImageView
    private lateinit var tvLoading: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val TOTAL_SPLASH_MS = 5000L
    private val ENTRANCE_MS = 1500L
    private val PROVERB_DELAY_MS = 600L
    private val LOADING_MS = TOTAL_SPLASH_MS - ENTRANCE_MS - PROVERB_DELAY_MS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        ivIcon = findViewById(R.id.ivIcon)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        proverbSection = findViewById(R.id.proverbSection)
        tvProverb = findViewById(R.id.tvProverb)
        tvProverbMeaning = findViewById(R.id.tvProverbMeaning)
        loadingBar = findViewById(R.id.loadingBar)
        loadingBarBg = findViewById(R.id.loadingBarBg)
        ivLoadingTile = findViewById(R.id.ivLoadingTile)
        tvLoading = findViewById(R.id.tvLoading)

        animateEntrance()
    }

    private fun animateEntrance() {
        ivIcon.alpha = 0f
        ivIcon.scaleX = 0.5f
        ivIcon.scaleY = 0.5f
        tvAppName.alpha = 0f
        tvAppName.translationY = 30f
        tvTagline.alpha = 0f

        ivIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(300)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvTagline.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(500)
            .start()

        handler.postDelayed({
            showProverbAndLoad()
        }, ENTRANCE_MS)
    }

    private fun showProverbAndLoad() {
        val proverb = ProverbRepository.getNextProverb()
        tvProverb.text = "\"${proverb.hausa_text}\""
        tvProverbMeaning.text = proverb.meaning_note

        proverbSection.visibility = View.VISIBLE
        proverbSection.alpha = 0f
        proverbSection.translationY = 20f
        proverbSection.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        handler.postDelayed({
            startLoadingAnimation()
        }, PROVERB_DELAY_MS)
    }

    private fun startLoadingAnimation() {
        val parent = ivLoadingTile.parent as FrameLayout
        val parentWidth = parent.width.toFloat()

        if (parentWidth <= 0f) {
            parent.post { startLoadingAnimation() }
            return
        }

        val tileWidth = ivLoadingTile.width.toFloat()
        val tileTravel = parentWidth - tileWidth

        val tileAnim = ObjectAnimator.ofFloat(ivLoadingTile, "translationX", 0f, tileTravel)
        tileAnim.duration = LOADING_MS
        tileAnim.interpolator = LinearInterpolator()

        tileAnim.addUpdateListener {
            val fraction = it.animatedFraction
            val tileX = tileWidth / 2f + fraction * tileTravel
            val lp = loadingBar.layoutParams
            lp.width = tileX.toInt()
            loadingBar.requestLayout()
        }

        val shimmer = ObjectAnimator.ofFloat(ivLoadingTile, "rotation", 0f, 360f)
        shimmer.duration = 1400
        shimmer.repeatCount = (LOADING_MS / 1400).toInt()
        shimmer.interpolator = LinearInterpolator()

        val set = AnimatorSet()
        set.playTogether(tileAnim, shimmer)
        set.start()

        set.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                navigateToMenu()
            }
        })
    }

    private fun navigateToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handler.removeCallbacksAndMessages(null)
    }
}
