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
    val context = LocalContext.current
    // This widget represents the infinite session. When a timed session is running instead
    // (e.g. started from the Timer widget), it shows as inactive with an option to switch.
    val isInfiniteActive = isSessionModeActive(session, timed = false)
    val isTimedActive = isSessionModeActive(session, timed = true)
    CaffeinateWidgetChrome(
        isOn = isInfiniteActive,
        title = context.getString(R.string.app_name),
        status = if (isInfiniteActive) {
            context.getString(R.string.status_on)
        } else {
            context.getString(R.string.status_off)
        },
        hint = widgetHint(
            context = context,
            isOwnModeActive = isInfiniteActive,
            isOtherModeActive = isTimedActive,
            switchToModeRes = R.string.widget_mode_infinite,
        ),
        iconRes = R.drawable.ic_caffeine_notification,
        action = actionRunCallback<ToggleCaffeineAction>(),
    )
}
