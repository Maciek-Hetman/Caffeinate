package com.maciejhetman.caffeinate.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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

@Composable
fun CaffeinateNavHost(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<NavKey>(HomeKey) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { it / 4 }) togetherWith
                (fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(300)) { -it / 8 })
        },
        popTransitionSpec = {
            (fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { -it / 8 }) togetherWith
                (fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(300)) { it / 4 })
        },
        predictivePopTransitionSpec = {
            (fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { -it / 8 }) togetherWith
                (fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(300)) { it / 4 })
        },
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeScreen(
                    onOpenSettings = {
                        if (backStack.lastOrNull() != SettingsKey) {
                            backStack.add(SettingsKey)
                        }
                    },
                )
            }
            entry<SettingsKey> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
