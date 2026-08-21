package com.maciejhetman.caffeinate.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Pixel-like haptic patterns for toggle, chips, dialog, and session expiry.
 */
object CaffeinateHaptics {

    fun toggleOn(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        vibratePredefined(context, VibrationEffect.EFFECT_CLICK)
    }

    fun toggleOff(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
        vibratePredefined(context, VibrationEffect.EFFECT_TICK)
    }

    fun durationSelected(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        vibratePredefined(context, VibrationEffect.EFFECT_TICK)
    }

    fun sliderTick(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        vibratePredefined(context, VibrationEffect.EFFECT_TICK)
    }

    fun dialogOpen(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        vibratePredefined(context, VibrationEffect.EFFECT_CLICK)
    }

    fun sessionExpired(context: Context) {
        if (!hapticsEnabled(context)) return
        vibratePredefined(context, VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    private fun hapticsEnabled(context: Context): Boolean {
        return try {
            val vibrator = vibrator(context) ?: return false
            vibrator.hasVibrator()
        } catch (_: Exception) {
            true
        }
    }

    private fun vibratePredefined(context: Context, effectId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
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
