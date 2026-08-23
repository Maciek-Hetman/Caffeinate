package com.maciejhetman.caffeinate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.maciejhetman.caffeinate.session.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccentDark,
    onPrimary = OnBlueAccentDark,
    primaryContainer = BlueAccentContainerDark,
    onPrimaryContainer = OnBlueAccentContainerDark,
    secondary = GreyAccentDark,
    onSecondary = OnGreyAccentDark,
    secondaryContainer = GreyAccentContainerDark,
    onSecondaryContainer = OnGreyAccentContainerDark,
    tertiary = BlueTertiaryDark,
    onTertiary = OnBlueTertiaryDark,
    background = SurfaceDarkGrey,
    onBackground = OnSurfaceLightGrey,
    surface = SurfaceDarkGrey,
    onSurface = OnSurfaceLightGrey,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccentLight,
    onPrimary = OnBlueAccentLight,
    primaryContainer = BlueAccentContainerLight,
    onPrimaryContainer = OnBlueAccentContainerLight,
    secondary = GreyAccentLight,
    onSecondary = OnGreyAccentLight,
    secondaryContainer = GreyAccentContainerLight,
    onSecondaryContainer = OnGreyAccentContainerLight,
    tertiary = BlueTertiaryLight,
    onTertiary = OnBlueTertiaryLight,
    background = SurfaceWhite,
    onBackground = OnSurfaceDarkGrey,
    surface = SurfaceWhite,
    onSurface = OnSurfaceDarkGrey,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaffeinateTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Keep decorView in sync so any gap under Compose isn't pure white.
            window.decorView.setBackgroundColor(colorScheme.background.toArgb())
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}
