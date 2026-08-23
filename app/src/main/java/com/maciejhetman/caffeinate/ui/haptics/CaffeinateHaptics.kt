package com.maciejhetman.caffeinate.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Haptic feedback for toggle, chips, dialog, and session expiry.
 * Respects the system haptic setting and avoids double-firing compose + vibrator.
 */
object CaffeinateHaptics {

    fun toggleOn(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun toggleOff(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
    }

    fun durationSelected(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    fun sliderTick(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    fun dialogOpen(haptic: HapticFeedback, context: Context) {
        if (!hapticsEnabled(context)) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun sessionExpired(context: Context) {
        if (!hapticsEnabled(context)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }

    private fun hapticsEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1,
            ) == 1
        } catch (_: Exception) {
            false
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
