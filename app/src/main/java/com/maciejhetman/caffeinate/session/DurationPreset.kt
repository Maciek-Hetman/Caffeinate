package com.maciejhetman.caffeinate.session

/**
 * User-selectable keep-awake durations. [millis] is null for Infinite.
 */
enum class DurationPreset(
    val label: String,
    val millis: Long?,
) {
    Infinite("∞", null),
    Minutes5("5m", 5 * 60_000L),
    Minutes15("15m", 15 * 60_000L),
    Minutes30("30m", 30 * 60_000L),
    Hour1("1h", 60 * 60_000L),
    Hour2("2h", 2 * 60 * 60_000L);

    val isTimed: Boolean get() = millis != null

    companion object {
        val Default: DurationPreset = Infinite

        fun fromName(name: String?): DurationPreset =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
