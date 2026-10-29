package com.maciejhetman.caffeinate.widget

import com.maciejhetman.caffeinate.session.CaffeineSession

/**
 * What a widget renders for the running session, at minute granularity so the per-second
 * countdown does not recompose the widget (each recomposition ships new RemoteViews).
 */
internal data class WidgetSession(
    val isOn: Boolean,
    val isTimed: Boolean,
    val remainingMinutes: Int?,
) {
    companion object {
        fun from(session: CaffeineSession): WidgetSession = when (session) {
            CaffeineSession.Off -> WidgetSession(isOn = false, isTimed = false, remainingMinutes = null)
            is CaffeineSession.On -> WidgetSession(
                isOn = true,
                isTimed = session.duration.isTimed,
                remainingMinutes = session.remainingMillis?.let {
                    CaffeineSession.remainingWholeMinutes(it)
                },
            )
        }
    }
}

/** Whether the running session (if any) matches a widget's own mode (timed vs infinite). */
internal fun WidgetSession.isModeActive(timed: Boolean): Boolean = isOn && isTimed == timed
