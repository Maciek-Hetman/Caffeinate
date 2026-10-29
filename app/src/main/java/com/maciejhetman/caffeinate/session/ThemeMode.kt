package com.maciejhetman.caffeinate.session

import android.app.UiModeManager
import android.content.Context
import android.os.Build

/**
 * User-selectable app appearance: follow the system setting, or force light/dark.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun serialize(): String = name

    /**
     * Mirrors the choice into the platform's per-app night mode (API 31+), so the splash screen,
     * window background and `-night` resources match a forced theme instead of flashing the
     * system one before Compose draws.
     */
    fun applyToPlatform(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return
        uiModeManager.setApplicationNightMode(
            when (this) {
                System -> UiModeManager.MODE_NIGHT_AUTO
                Light -> UiModeManager.MODE_NIGHT_NO
                Dark -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }

    companion object {
        val Default: ThemeMode = System

        fun fromSerialized(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
