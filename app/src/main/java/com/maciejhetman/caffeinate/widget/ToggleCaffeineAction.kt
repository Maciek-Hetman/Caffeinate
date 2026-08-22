package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.maciejhetman.caffeinate.session.CaffeineController

class ToggleCaffeineAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        // Optimistic On is published in the controller before the FGS binds; Off
        // follows when the service ends the session and notifySurfaces refreshes.
        CaffeineController.get(context).toggleInfiniteBlocking()
        CaffeineWidget().update(context, glanceId)
    }
}
