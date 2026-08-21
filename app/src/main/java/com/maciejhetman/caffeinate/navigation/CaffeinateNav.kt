package com.maciejhetman.caffeinate.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.maciejhetman.caffeinate.ui.home.HomeScreen
import com.maciejhetman.caffeinate.ui.settings.SettingsScreen

data object HomeKey : NavKey
data object SettingsKey : NavKey

/** Material emphasized easing used by Google system predictive back. */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private const val NavDurationMs = 450

@Composable
fun CaffeinateNavHost(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<NavKey>(HomeKey) }

    // Opaque surface prevents white window flash during predictive back / fades.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (
                    slideInHorizontally(
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) { it } +
                        fadeIn(animationSpec = tween(NavDurationMs / 2, delayMillis = 45))
                    ) togetherWith (
                    slideOutHorizontally(
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) { -it / 4 } +
                        fadeOut(animationSpec = tween(90))
                    )
            },
            popTransitionSpec = {
                (
                    slideInHorizontally(
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) { -it / 4 } +
                        fadeIn(animationSpec = tween(NavDurationMs / 2, delayMillis = 45))
                    ) togetherWith (
                    slideOutHorizontally(
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) { it } +
                        fadeOut(animationSpec = tween(90))
                    )
            },
            // Predictive back: underlying screen scales up from behind; current follows the gesture.
            predictivePopTransitionSpec = {
                (
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) +
                        fadeIn(animationSpec = tween(NavDurationMs / 3))
                    ) togetherWith (
                    scaleOut(
                        targetScale = 1.0f,
                        animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                    ) +
                        slideOutHorizontally(
                            animationSpec = tween(NavDurationMs, easing = EmphasizedEasing),
                        ) { full -> (full * 0.15f).toInt() } +
                        fadeOut(animationSpec = tween(NavDurationMs / 2))
                    )
            },
            entryProvider = entryProvider {
                entry<HomeKey> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        HomeScreen(
                            onOpenSettings = {
                                if (backStack.lastOrNull() != SettingsKey) {
                                    backStack.add(SettingsKey)
                                }
                            },
                        )
                    }
                }
                entry<SettingsKey> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                }
            },
        )
    }
}
