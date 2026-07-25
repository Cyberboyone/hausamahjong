package com.nakudin.hausamahjong.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nakudin.hausamahjong.R
import com.nakudin.hausamahjong.data.ProverbRepository

class ProverbDialog(
    activity: AppCompatActivity,
    private val onDismiss: () -> Unit
) : Dialog(activity) {

    private var showEnglish = false

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_proverb)
        setCancelable(true)

        val tvProverbHausa = findViewById<TextView>(R.id.tvProverbHausa)
        val tvProverbEnglish = findViewById<TextView>(R.id.tvProverbEnglish)
        val tvMeaning = findViewById<TextView>(R.id.tvMeaning)
        val btnToggleEnglish = findViewById<Button>(R.id.btnToggleEnglish)
        val btnNext = findViewById<Button>(R.id.btnNext)

        val proverb = ProverbRepository.getNextProverb()

        tvProverbHausa.text = proverb.hausa_text
        tvProverbEnglish.text = proverb.english_translation
        tvMeaning.text = proverb.meaning_note

        tvProverbEnglish.visibility = View.GONE
        tvMeaning.visibility = View.GONE

        btnToggleEnglish.setOnClickListener {
            showEnglish = !showEnglish
            if (showEnglish) {
                tvProverbEnglish.visibility = View.VISIBLE
                tvMeaning.visibility = View.VISIBLE
                btnToggleEnglish.text = context.getString(R.string.hide_english)
            } else {
                tvProverbEnglish.visibility = View.GONE
                tvMeaning.visibility = View.GONE
                btnToggleEnglish.text = context.getString(R.string.show_english)
            }
        }

        btnNext.setOnClickListener {
            dismiss()
            onDismiss()
        }

        setOnDismissListener { onDismiss() }
    }
}