package com.gymbrosbro.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.gymbrosbro.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/** Moments that get a sound and a buzz (SPEC §7.11). */
enum class Cue { SET_DONE, HIGH_SCORE, LEVEL_UP }

/** Plays [Cue]s as chiptune blips and vibrations, each behind its own setting. */
class Feedback(context: Context, settings: SettingsRepository, scope: CoroutineScope) {
    private val sound = settings.sound.stateIn(scope, SharingStarted.Eagerly, true)
    private val haptics = settings.haptics.stateIn(scope, SharingStarted.Eagerly, true)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(cue: Cue) {
        if (sound.value) Chiptune.play(cue)
        if (haptics.value) buzz(cue)
    }

    private fun buzz(cue: Cue) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = when (cue) {
            Cue.SET_DONE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            Cue.HIGH_SCORE -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40, 60, 90), -1)
            Cue.LEVEL_UP -> VibrationEffect.createWaveform(longArrayOf(0, 60, 50, 60, 50, 60, 50, 160), -1)
        }
        v.vibrate(effect)
    }
}

/**
 * A tiny square-wave synth, so the sounds need no audio files: each cue is a
 * few notes with a quick decay, rendered once and played on a background thread.
 */
private object Chiptune {
    private const val RATE = 22_050
    private const val VOLUME = 0.16

    private class Note(val hz: Double, val ms: Int, val duty: Double = 0.5)

    private val tunes = mapOf(
        Cue.SET_DONE to listOf(Note(C6, 35, 0.25), Note(G6, 60, 0.25)),
        Cue.HIGH_SCORE to listOf(Note(C5, 70), Note(E5, 70), Note(G5, 70), Note(C6, 70), Note(E6, 240)),
        Cue.LEVEL_UP to listOf(
            Note(G4, 60), Note(C5, 60), Note(E5, 60), Note(G5, 60),
            Note(C6, 60), Note(E6, 60), Note(G6, 60), Note(C7, 320),
        ),
    )

    private val rendered = mutableMapOf<Cue, ShortArray>()
    private val player = Executors.newSingleThreadExecutor()

    fun play(cue: Cue) {
        player.execute {
            val pcm = rendered.getOrPut(cue) { render(tunes.getValue(cue)) }
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / RATE + 50)
                track.release()
            }
        }
    }

    private fun render(notes: List<Note>): ShortArray {
        val out = ArrayList<Short>()
        for (note in notes) {
            val n = RATE * note.ms / 1000
            val period = RATE / note.hz
            for (i in 0 until n) {
                val phase = (i % period) / period
                val square = if (phase < note.duty) 1.0 else -1.0
                // Quick attack, linear decay, so notes pluck instead of click.
                val envelope = minOf(1.0, i / 40.0) * (1.0 - 0.6 * i / n)
                out += (square * envelope * VOLUME * Short.MAX_VALUE).toInt().toShort()
            }
        }
        // Fade the tail to avoid a pop when the track stops.
        val fade = minOf(200, out.size)
        for (i in 0 until fade) {
            val idx = out.size - fade + i
            out[idx] = (out[idx] * sin((fade - i) / fade.toDouble() * PI / 2)).toInt().toShort()
        }
        return out.toShortArray()
    }

    private const val G4 = 392.00
    private const val C5 = 523.25
    private const val E5 = 659.25
    private const val G5 = 783.99
    private const val C6 = 1046.50
    private const val E6 = 1318.51
    private const val G6 = 1567.98
    private const val C7 = 2093.00
}
