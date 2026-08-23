package com.maciejhetman.caffeinate.widget

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset

@Composable
internal fun CaffeinateWidgetChrome(
    isOn: Boolean,
    title: String,
    status: String,
    hint: String,
    iconRes: Int,
    action: Action,
) {
    val container = if (isOn) {
        GlanceTheme.colors.primaryContainer
    } else {
        GlanceTheme.colors.widgetBackground
    }
    val onContainer = if (isOn) {
        GlanceTheme.colors.onPrimaryContainer
    } else {
        GlanceTheme.colors.onSurface
    }
    val muted = if (isOn) {
        GlanceTheme.colors.onPrimaryContainer
    } else {
        GlanceTheme.colors.onSurfaceVariant
    }
    val iconBg = if (isOn) {
        GlanceTheme.colors.primary
    } else {
        GlanceTheme.colors.secondaryContainer
    }
    val iconTint = if (isOn) {
        GlanceTheme.colors.onPrimary
    } else {
        GlanceTheme.colors.onSecondaryContainer
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(container)
            .cornerRadius(20.dp)
            .clickable(action)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(iconTint),
            )
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = status,
                style = TextStyle(
                    color = onContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = hint,
                style = TextStyle(
                    color = muted,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

internal fun sessionStatusLabel(context: Context, session: CaffeineSession): String = when (val s = session) {
    CaffeineSession.Off -> context.getString(R.string.status_off)
    is CaffeineSession.On -> when {
        s.duration is DurationPreset.Infinite || s.remainingMillis == null ->
            context.getString(R.string.status_on)
        else -> CaffeineSession.formatCountdown(s.remainingMillis)
    }
}

/** Whether the running session (if any) matches a widget's own mode (timed vs infinite). */
internal fun isSessionModeActive(session: CaffeineSession, timed: Boolean): Boolean =
    session is CaffeineSession.On && session.duration.isTimed == timed

/**
 * Hint text for a widget whose own mode may differ from the currently running session:
 * - own mode active -> offer to stop
 * - the other mode is running -> offer to switch modes instead of stopping
 * - nothing running -> offer to start
 */
internal fun widgetHint(
    context: Context,
    isOwnModeActive: Boolean,
    isOtherModeActive: Boolean,
    @StringRes switchToModeRes: Int,
): String = when {
    isOwnModeActive -> context.getString(R.string.widget_hint_stop)
    isOtherModeActive -> context.getString(
        R.string.widget_hint_switch,
        context.getString(switchToModeRes),
    )
    else -> context.getString(R.string.widget_hint_start)
}
