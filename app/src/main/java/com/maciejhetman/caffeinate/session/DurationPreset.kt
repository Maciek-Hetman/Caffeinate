package com.maciejhetman.caffeinate.session

/**
 * Keep-awake duration: infinite or a timed length in whole minutes.
 */
sealed class DurationPreset {
    data object Infinite : DurationPreset()

    data class Timed(val minutes: Int) : DurationPreset() {
        init {
            require(minutes in MIN_MINUTES..MAX_MINUTES) {
                "minutes must be in $MIN_MINUTES..$MAX_MINUTES"
            }
        }
    }

    val millis: Long?
        get() = when (this) {
            Infinite -> null
            is Timed -> minutes * 60_000L
        }

    val isTimed: Boolean get() = this is Timed

    val label: String
        get() = when (this) {
            Infinite -> "∞"
            is Timed -> formatMinutes(minutes)
        }

    fun serialize(): String = when (this) {
        Infinite -> SERIAL_INFINITE
        is Timed -> "$SERIAL_TIMED_PREFIX$minutes"
    }

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 240 // 4 hours
        const val DEFAULT_TIMER_MINUTES = 30

        const val SERIAL_INFINITE = "Infinite"
        private const val SERIAL_TIMED_PREFIX = "Timed:"

        val Default: DurationPreset = Infinite
        val DefaultTimed: DurationPreset = Timed(DEFAULT_TIMER_MINUTES)

        fun formatMinutes(minutes: Int): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return when {
                hours == 0 -> "${mins}m"
                mins == 0 -> "${hours}h"
                else -> "${hours}h ${mins}m"
            }
        }

        fun fromSerialized(value: String?): DurationPreset {
            if (value.isNullOrBlank()) return Default
            if (value == SERIAL_INFINITE || value == "Infinite") return Infinite
            if (value.startsWith(SERIAL_TIMED_PREFIX)) {
                val minutes = value.removePrefix(SERIAL_TIMED_PREFIX).toIntOrNull()
                    ?: return DefaultTimed
                return Timed(minutes.coerceIn(MIN_MINUTES, MAX_MINUTES))
            }
            // Migrate legacy enum names from the chip-based UI.
            return when (value) {
                "Minutes5" -> Timed(5)
                "Minutes15" -> Timed(15)
                "Minutes30" -> Timed(30)
                "Hour1" -> Timed(60)
                "Hour2" -> Timed(120)
                else -> Default
            }
        }

        /** @deprecated Use [fromSerialized]. */
        fun fromName(name: String?): DurationPreset = fromSerialized(name)
    }
}
