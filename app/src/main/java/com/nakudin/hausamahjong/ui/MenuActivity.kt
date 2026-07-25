package com.nakudin.hausamahjong.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.LevelRepository

class MenuActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnLevelSelect: Button
    private lateinit var btnSettings: Button

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
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        btnPlay = findViewById(R.id.btnPlay)
        btnLevelSelect = findViewById(R.id.btnLevelSelect)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupClickListeners() {
        btnPlay.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("LEVEL_NUMBER", 1)
            startActivity(intent)
        }

        btnLevelSelect.setOnClickListener {
            showLevelSelectDialog()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showLevelSelectDialog() {
        try {
            val dialog = android.app.Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_level_select)
            dialog.setCancelable(true)

            val rvLevels = dialog.findViewById<RecyclerView>(R.id.rvLevels)
            val btnClose = dialog.findViewById<Button>(R.id.btnClose)

            val adapter = LevelSelectAdapter { levelNumber ->
                dialog.dismiss()
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("LEVEL_NUMBER", levelNumber)
                startActivity(intent)
            }

            rvLevels.layoutManager = GridLayoutManager(this, 4)
            rvLevels.adapter = adapter

            btnClose.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Could not load levels", android.widget.Toast.LENGTH_SHORT).show()
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
}

class LevelSelectAdapter(
    private val onLevelClick: (Int) -> Unit
) : RecyclerView.Adapter<LevelSelectAdapter.LevelViewHolder>() {

    private val levels = LevelRepository.getLevels()

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LevelViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]
        holder.bind(level.levelNumber)
    }

    override fun getItemCount() = levels.size

    inner class LevelViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevelNumber: TextView = itemView.findViewById(R.id.tvLevelNumber)
        private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)

        fun bind(levelNumber: Int) {
            tvLevelNumber.text = levelNumber.toString()
            val level = LevelRepository.getLevel(levelNumber)
            tvLevelName.text = level?.name_ha ?: "Mataki $levelNumber"
            itemView.setOnClickListener { onLevelClick(levelNumber) }
        }
    }
}