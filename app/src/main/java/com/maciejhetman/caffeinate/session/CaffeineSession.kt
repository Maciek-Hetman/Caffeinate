package com.maciejhetman.caffeinate.session

/**
 * Live keep-awake session state shared by UI, tile, widget, and notification.
 */
sealed interface CaffeineSession {
    data object Off : CaffeineSession

    data class On(
        val duration: DurationPreset,
        val remainingMillis: Long?,
        val endsAtEpochMillis: Long?,
    ) : CaffeineSession

    val isActive: Boolean get() = this is On

    fun displayRemaining(): String = when (this) {
        Off -> "Off"
        is On -> when {
            duration == DurationPreset.Infinite || remainingMillis == null -> "∞"
            else -> formatCountdown(remainingMillis)
        }
    }

    companion object {
        fun formatCountdown(millis: Long): String {
            val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }
}
