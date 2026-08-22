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

class CaffeineWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val controller = CaffeineController.get(context)
        provideContent {
            // Observe inside composition — update/updateAll do not re-run provideGlance
            // while a Glance session is already alive.
            val session by controller.session.collectAsState()
            GlanceTheme {
                ToggleWidgetContent(session = session)
            }
        }
    }
}

@Composable
private fun ToggleWidgetContent(session: CaffeineSession) {
    // This widget represents the infinite session. When a timed session is running instead
    // (e.g. started from the Timer widget), it shows as inactive with an option to switch.
    val isInfiniteActive = isSessionModeActive(session, timed = false)
    val isTimedActive = isSessionModeActive(session, timed = true)
    CaffeinateWidgetChrome(
        isOn = isInfiniteActive,
        title = "Caffeinate",
        status = if (isInfiniteActive) "On" else DurationPreset.Infinite.label,
        hint = widgetHint(isInfiniteActive, isTimedActive, switchToLabel = "infinite"),
        iconRes = R.drawable.ic_caffeine_notification,
        action = actionRunCallback<ToggleCaffeineAction>(),
    )
}
