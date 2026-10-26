package com.maciejhetman.caffeinate.session

/**
 * Snapshot of every persisted user preference.
 */
data class CaffeineSettings(
    val lastDuration: DurationPreset = DurationPreset.Default,
    val widgetTimerDuration: DurationPreset.Timed = DurationPreset.DefaultTimed,
    val themeMode: ThemeMode = ThemeMode.Default,
    val dynamicColor: Boolean = true,
    val stopOnScreenOff: Boolean = false,
)
