package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset

/**
 * Starts a timed keep-awake session using the length configured in Settings.
 */
class TimerCaffeineWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val controller = CaffeineController.get(context)
        provideContent {
            val session by controller.session.collectAsState()
            val timerDuration by controller.widgetTimerDuration.collectAsState(
                initial = DurationPreset.DefaultTimed,
            )
            GlanceTheme {
                TimerWidgetContent(
                    session = session,
                    timerDuration = timerDuration,
                )
            }
        }
    }
}

@Composable
private fun TimerWidgetContent(
    session: CaffeineSession,
    timerDuration: DurationPreset.Timed,
) {
    val isOn = session.isActive
    val status = if (isOn) {
        sessionStatusLabel(session)
    } else {
        timerDuration.label
    }
    CaffeinateWidgetChrome(
        isOn = isOn,
        title = "Timer",
        status = status,
        hint = if (isOn) "Tap to stop" else "Tap to start",
        iconRes = R.drawable.ic_widget_timer,
        action = actionRunCallback<StartTimerCaffeineAction>(),
    )
}
