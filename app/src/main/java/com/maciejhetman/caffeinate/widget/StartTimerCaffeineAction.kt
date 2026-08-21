package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.maciejhetman.caffeinate.session.CaffeineController

class StartTimerCaffeineAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        CaffeineController.get(context).toggleWidgetTimerBlocking()
        TimerCaffeineWidget().update(context, glanceId)
    }
}
