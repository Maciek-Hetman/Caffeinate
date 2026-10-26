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
    ) : CaffeineSession {
        /** Fraction of a timed session still left (1 → 0), or null for infinite sessions. */
        val remainingFraction: Float?
            get() {
                val total = duration.millis ?: return null
                val remaining = remainingMillis ?: return null
                return (remaining.toFloat() / total).coerceIn(0f, 1f)
            }
    }

    val isActive: Boolean get() = this is On

    companion object {
        fun formatCountdown(millis: Long): String {
            // Round up so a fresh 30m session reads 30:00, not 29:59, and 0:00 is never shown
            // while the session is still running.
            val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }

        /** Whole minutes left, rounded up, for surfaces that only refresh once a minute. */
        fun remainingWholeMinutes(millis: Long): Int =
            ((millis + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    }
}
