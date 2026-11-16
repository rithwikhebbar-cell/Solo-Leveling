package com.example.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class SystemCueType {
    AWAKENING_CHIME,
    QUEST_COMPLETED,
    LEVEL_UP,
    RANK_PROMOTION,
    ALERT_WARNING,
    VOICE_COMMAND_ACK
}

/**
 * Optional, lightweight native synth cue & haptic feedback engine.
 * Strictly honors user sound, haptic, and reduced-motion preferences.
 */
object SystemAudioHapticFeedback {

    fun triggerCue(
        context: Context,
        cue: SystemCueType,
        soundEnabled: Boolean,
        hapticEnabled: Boolean,
        reducedMotionEnabled: Boolean
    ) {
        if (soundEnabled && !reducedMotionEnabled) {
            playToneSafe(cue)
        }
        if (hapticEnabled && !reducedMotionEnabled) {
            vibrateSafe(context, cue)
        }
    }

    private fun playToneSafe(cue: SystemCueType) {
        runCatching {
            val toneType = when (cue) {
                SystemCueType.AWAKENING_CHIME -> ToneGenerator.TONE_PROP_BEEP2
                SystemCueType.QUEST_COMPLETED -> ToneGenerator.TONE_PROP_ACK
                SystemCueType.LEVEL_UP -> ToneGenerator.TONE_PROP_BEEP2
                SystemCueType.RANK_PROMOTION -> ToneGenerator.TONE_SUP_CONFIRM
                SystemCueType.ALERT_WARNING -> ToneGenerator.TONE_PROP_NACK
                SystemCueType.VOICE_COMMAND_ACK -> ToneGenerator.TONE_PROP_BEEP
            }
            val durationMs = when (cue) {
                SystemCueType.RANK_PROMOTION, SystemCueType.LEVEL_UP -> 220
                SystemCueType.ALERT_WARNING -> 160
                else -> 110
            }
            val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 42)
            generator.startTone(toneType, durationMs)
            generator.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateSafe(context: Context, cue: SystemCueType) {
        runCatching {
            val durationMs = when (cue) {
                SystemCueType.RANK_PROMOTION -> 140L
                SystemCueType.LEVEL_UP -> 95L
                SystemCueType.ALERT_WARNING -> 80L
                else -> 35L
            }
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}
