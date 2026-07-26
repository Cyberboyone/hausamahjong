package com.nakudin.hausamahjong.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    var isSoundEnabled = true
    var isVibrationEnabled = true

    fun init() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Throwable) {}

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun playTone(toneType: Int, durationMs: Int = 100) {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Throwable) {}
    }

    private fun playCustomTone(frequency: Double, durationMs: Int, volume: Float = 0.5f) {
        if (!isSoundEnabled) return
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = if (i < numSamples / 10) {
                    i.toFloat() / (numSamples / 10)
                } else if (i > numSamples * 8 / 10) {
                    (numSamples - i).toFloat() / (numSamples * 2 / 10)
                } else {
                    1f
                }
                samples[i] = (sin(2.0 * Math.PI * frequency * t) * Short.MAX_VALUE * volume * envelope).toInt().toShort()
            }

            val bufferSize = numSamples * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, numSamples)
            audioTrack.play()
            audioTrack.setNotificationMarkerPosition(numSamples)
        } catch (_: Throwable) {}
    }

    fun vibrate(ms: Long = 30) {
        if (!isVibrationEnabled) return
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {}
    }

    fun select() {
        playCustomTone(880.0, 60, 0.3f)
        vibrate(20)
    }

    fun tap() {
        playCustomTone(660.0, 40, 0.2f)
    }

    fun match() {
        playCustomTone(523.25, 80, 0.4f)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playCustomTone(659.25, 80, 0.4f)
        }, 80)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playCustomTone(783.99, 120, 0.5f)
        }, 160)
        vibrate(50)
    }

    fun mismatch() {
        playCustomTone(200.0, 150, 0.3f)
        vibrate(100)
    }

    fun win() {
        val notes = doubleArrayOf(523.25, 587.33, 659.25, 698.46, 783.99, 880.0, 1046.50)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        notes.forEachIndexed { index, freq ->
            handler.postDelayed({
                playCustomTone(freq, 150, 0.4f)
            }, (index * 120).toLong())
        }
        vibrate(200)
    }

    fun lose() {
        val notes = doubleArrayOf(440.0, 392.0, 349.23, 293.66)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        notes.forEachIndexed { index, freq ->
            handler.postDelayed({
                playCustomTone(freq, 200, 0.3f)
            }, (index * 200).toLong())
        }
    }

    fun button() {
        playCustomTone(1000.0, 30, 0.15f)
        vibrate(15)
    }

    fun hint() {
        playCustomTone(698.46, 100, 0.3f)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playCustomTone(880.0, 150, 0.3f)
        }, 100)
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Throwable) {}
    }
}
