package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
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
    val context = LocalContext.current
    // This widget represents a timed session. When an infinite session is running instead
    // (e.g. started from the Caffeinate widget), it shows as inactive with an option to switch.
    val isTimedActive = isSessionModeActive(session, timed = true)
    val isInfiniteActive = isSessionModeActive(session, timed = false)
    val status = if (isTimedActive) {
        sessionStatusLabel(context, session)
    } else {
        timerDuration.label
    }
    CaffeinateWidgetChrome(
        isOn = isTimedActive,
        title = context.getString(R.string.widget_timer_label),
        status = status,
        hint = widgetHint(
            context = context,
            isOwnModeActive = isTimedActive,
            isOtherModeActive = isInfiniteActive,
            switchToModeRes = R.string.widget_mode_timer,
        ),
        iconRes = R.drawable.ic_widget_timer,
        action = actionRunCallback<StartTimerCaffeineAction>(),
    )
}
