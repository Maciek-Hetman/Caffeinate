package com.maciejhetman.caffeinate.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.maciejhetman.caffeinate.ui.home.HomeScreen
import com.maciejhetman.caffeinate.ui.settings.SettingsScreen
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme

sealed interface Screen : NavKey {
    val route: String

    data object Home : Screen {
        override val route = "home"
    }

    data object Settings : Screen {
        override val route = "settings"
    }

    companion object {
        val all: List<Screen> = listOf(Home, Settings)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val NavMotion = MotionScheme.standard()

/** Saves the back stack as route names so rotation / process death keep the open screen. */
private val BackStackSaver = listSaver<SnapshotStateList<NavKey>, String>(
    save = { stack -> stack.filterIsInstance<Screen>().map { it.route } },
    restore = { routes ->
        routes.mapNotNull { route -> Screen.all.firstOrNull { it.route == route } }
            .ifEmpty { listOf(Screen.Home) }
            .toMutableStateList()
    },
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaffeinateNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberSaveable(saver = BackStackSaver) {
        mutableListOf<NavKey>(Screen.Home).toMutableStateList()
    }
    // Standard (non-bouncy) springs: an overshooting full-screen slide would expose the edge.
    val spatial: FiniteAnimationSpec<IntOffset> = NavMotion.defaultSpatialSpec()
    val spatialFloat: FiniteAnimationSpec<Float> = NavMotion.defaultSpatialSpec()
    val fadeInSpec: FiniteAnimationSpec<Float> = NavMotion.defaultEffectsSpec()
    val fadeOutSpec: FiniteAnimationSpec<Float> = NavMotion.fastEffectsSpec()

    // Opaque surface under NavDisplay so fades never reveal the window background.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CaffeinateTheme.surfaces.page,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (slideInHorizontally(spatial) { it } + fadeIn(fadeInSpec)) togetherWith
                    (slideOutHorizontally(spatial) { -it / 4 } + fadeOut(fadeOutSpec))
            },
            popTransitionSpec = {
                (slideInHorizontally(spatial) { -it / 4 } + fadeIn(fadeInSpec)) togetherWith
                    (slideOutHorizontally(spatial) { it } + fadeOut(fadeOutSpec))
            },
            // Predictive back: underlying screen scales up from behind; current follows the gesture.
            predictivePopTransitionSpec = {
                (scaleIn(spatialFloat, initialScale = 0.92f) + fadeIn(fadeInSpec)) togetherWith
                    (slideOutHorizontally(spatial) { full -> (full * 0.15f).toInt() } +
                        fadeOut(fadeOutSpec))
            },
            entryProvider = entryProvider {
                entry<Screen.Home> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = CaffeinateTheme.surfaces.page,
                    ) {
                        HomeScreen(
                            onOpenSettings = {
                                if (backStack.lastOrNull() != Screen.Settings) {
                                    backStack.add(Screen.Settings)
                                }
                            },
                        )
                    }
                }
                entry<Screen.Settings> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = CaffeinateTheme.surfaces.page,
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
