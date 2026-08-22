package com.maciejhetman.caffeinate.session

/**
 * User-selectable app appearance: follow the system setting, or force light/dark.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun serialize(): String = name

    companion object {
        val Default: ThemeMode = System

        fun fromSerialized(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
