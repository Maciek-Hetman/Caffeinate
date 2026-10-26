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
import com.maciejhetman.caffeinate.session.DurationPreset
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Starts a timed keep-awake session using the length configured in Settings.
 */
class TimerCaffeineWidget : GlanceAppWidget() {

    override val sizeMode = WidgetSizeMode

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val controller = CaffeineController.get(context)
        val sessions = controller.session.map(WidgetSession::from).distinctUntilChanged()
        val initialSession = WidgetSession.from(controller.session.value)
        provideContent {
            val widgetSession by sessions.collectAsState(initial = initialSession)
            val settings by controller.settings.collectAsState()
            GlanceTheme(colors = widgetColors(settings.dynamicColor)) {
                TimerWidgetContent(
                    session = widgetSession,
                    timerDuration = settings.widgetTimerDuration,
                )
            }
        }
    }
}

@Composable
private fun TimerWidgetContent(
    session: WidgetSession,
    timerDuration: DurationPreset.Timed,
) {
    val context = LocalContext.current
    // This widget represents a timed session. When an infinite session is running instead
    // (e.g. started from the Caffeinate widget), it shows as inactive with an option to switch.
    val isTimedActive = session.isModeActive(timed = true)
    val isInfiniteActive = session.isModeActive(timed = false)
    val status = if (isTimedActive) {
        timedStatusLabel(context, session)
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
