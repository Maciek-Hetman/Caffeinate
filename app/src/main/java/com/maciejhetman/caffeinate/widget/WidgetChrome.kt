package com.maciejhetman.caffeinate.widget

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.theme.DarkColorScheme
import com.maciejhetman.caffeinate.ui.theme.LightColorScheme

/** Below this width only the icon and status fit; title and hint are dropped. */
private val CompactWidth = 160.dp

internal val WidgetSizeMode = SizeMode.Responsive(
    setOf(DpSize(110.dp, 48.dp), DpSize(CompactWidth, 48.dp)),
)

/** The app's fallback palette, used when Material You is off or unavailable. */
private val AppWidgetColors: ColorProviders = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

/** Wallpaper colors when the user keeps Material You on (API 31+), else the app palette. */
@Composable
internal fun widgetColors(dynamicColor: Boolean): ColorProviders =
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme.colors
    } else {
        AppWidgetColors
    }

@Composable
internal fun CaffeinateWidgetChrome(
    isOn: Boolean,
    title: String,
    status: String,
    hint: String,
    iconRes: Int,
    action: Action,
) {
    val compact = LocalSize.current.width < CompactWidth
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
            .widgetCornerRadius()
            .clickable(action)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                // Circle at rest, squircle while running — echoes the in-app shape morph.
                .cornerRadius(if (isOn) 14.dp else 22.dp)
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
            if (!compact) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                text = status,
                style = TextStyle(
                    color = onContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                maxLines = 1,
            )
            if (!compact) {
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
}

/** Matches the launcher's own widget corner radius where the platform exposes it. */
private fun GlanceModifier.widgetCornerRadius(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        cornerRadius(24.dp)
    }

internal fun timedStatusLabel(context: Context, session: WidgetSession): String =
    session.remainingMinutes
        ?.let { DurationPreset.formatMinutes(it) }
        ?: context.getString(R.string.status_on)

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
