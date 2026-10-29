package com.maciejhetman.caffeinate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.maciejhetman.caffeinate.session.ThemeMode

/**
 * App-level surface roles on top of the M3 scheme: a page background and the card / list
 * containers placed on it, picked per brightness so cards always stand out from the page
 * (white cards on grey in light, lifted grey cards on near-black in dark).
 */
@Immutable
data class SurfaceRoles(
    val page: Color,
    val card: Color,
)

private val LocalSurfaceRoles = staticCompositionLocalOf {
    SurfaceRoles(page = Color.Unspecified, card = Color.Unspecified)
}

object CaffeinateTheme {
    val surfaces: SurfaceRoles
        @Composable
        @ReadOnlyComposable
        get() = LocalSurfaceRoles.current
}

@Composable
fun isAppInDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaffeinateTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = isAppInDarkTheme(themeMode)
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val surfaces = baseScheme.surfaceRoles(darkTheme)
    val colorScheme = baseScheme.copy(background = surfaces.page)

    CompositionLocalProvider(LocalSurfaceRoles provides surfaces) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

private fun ColorScheme.surfaceRoles(isDark: Boolean): SurfaceRoles = if (isDark) {
    SurfaceRoles(page = surfaceContainerLowest, card = surfaceContainerHigh)
} else {
    SurfaceRoles(page = surfaceContainer, card = surfaceContainerLowest)
}
