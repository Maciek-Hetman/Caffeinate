package com.maciejhetman.caffeinate.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Pixel-like haptic patterns. Uses a single vibration path to avoid double-fires
 * (Compose HapticFeedback + VibrationEffect stacking).
 */
object CaffeinateHaptics {

    fun toggleOn(haptic: HapticFeedback, context: Context) {
        perform(context, VibrationEffect.EFFECT_CLICK, haptic, HapticFeedbackType.Confirm)
    }

    fun toggleOff(haptic: HapticFeedback, context: Context) {
        perform(context, VibrationEffect.EFFECT_TICK, haptic, HapticFeedbackType.Reject)
    }

    fun durationSelected(haptic: HapticFeedback, context: Context) {
        perform(context, VibrationEffect.EFFECT_TICK, haptic, HapticFeedbackType.SegmentTick)
    }

    fun sliderTick(haptic: HapticFeedback, context: Context) {
        perform(context, VibrationEffect.EFFECT_TICK, haptic, HapticFeedbackType.SegmentTick)
    }

    fun dialogOpen(haptic: HapticFeedback, context: Context) {
        perform(context, VibrationEffect.EFFECT_CLICK, haptic, HapticFeedbackType.LongPress)
    }

    fun sessionExpired(context: Context) {
        vibratePredefined(context, VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    /**
     * Prefer platform VibrationEffect on API 29+ (Pixel-quality predefined effects).
     * Fall back to Compose haptics only when vibration is unavailable.
     */
    private fun perform(
        context: Context,
        effectId: Int,
        haptic: HapticFeedback,
        fallback: HapticFeedbackType,
    ) {
        if (!hapticsEnabled(context)) return
        if (vibratePredefined(context, effectId)) return
        haptic.performHapticFeedback(fallback)
    }

    private fun hapticsEnabled(context: Context): Boolean {
        return try {
            val vibrator = vibrator(context) ?: return false
            vibrator.hasVibrator()
        } catch (_: Exception) {
            true
        }
    }

    /** @return true if a vibration was successfully requested */
    private fun vibratePredefined(context: Context, effectId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val vibrator = vibrator(context) ?: return false
        if (!vibrator.hasVibrator()) return false
        return runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }.isSuccess
    }

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
}
