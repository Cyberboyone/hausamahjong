package com.nakudin.hausamahjong.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF1B5E20.toInt())
        }

        val title = TextView(this).apply {
            text = "Oops! App Crashed"
            textSize = 24f
            setTextColor(0xFFFFD54F.toInt())
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)

        val errorText = TextView(this).apply {
            text = intent.getStringExtra("error") ?: "Unknown error"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 32)
            setOnLongClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("crash", text)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(this@CrashActivity, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }
        layout.addView(errorText)

        val restartBtn = Button(this).apply {
            text = "Restart App"
            setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
                finish()
            }
        }
        layout.addView(restartBtn)

        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
