package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

class ToggleCaffeineAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        com.maciejhetman.caffeinate.session.CaffeineController.get(context).toggleBlocking()
        CaffeineWidget().update(context, glanceId)
    }
}
