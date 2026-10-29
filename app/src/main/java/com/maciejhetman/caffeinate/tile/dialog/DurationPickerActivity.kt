package com.maciejhetman.caffeinate.tile.dialog

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.components.DurationSelector
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme

/**
 * Translucent activity launched via ACTION_QS_TILE_PREFERENCES (QS long-press).
 *
 * Uses [android.R.style.Theme_Translucent] (not Material Light) so the window never paints a
 * white preview frame. System activity transitions are disabled; the scrim is drawn immediately.
 */
class DurationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppressSystemTransitions()
        // Light status/navigation icons: they always sit on the dark scrim.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        window.setBackgroundDrawable(AndroidColor.TRANSPARENT.toDrawable())
        val controller = CaffeineController.get(this)
        setContent {
            val settings by controller.settings.collectAsStateWithLifecycle()
            CaffeinateTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                DurationPickerContent(
                    lastDuration = settings.lastDuration,
                    onDismiss = { finish() },
                    onConfirm = { duration ->
                        controller.start(duration)
                        finish()
                    },
                    onStop = {
                        controller.stop()
                        finish()
                    },
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        suppressSystemTransitions()
    }

    private fun suppressSystemTransitions() {
        // Avoid system window fade blending against a light theme frame (white flash).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DurationPickerContent(
    lastDuration: DurationPreset,
    onDismiss: () -> Unit,
    onConfirm: (DurationPreset) -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val controller = CaffeineController.get(context)
    val session by controller.session.collectAsStateWithLifecycle()
    val motion = MaterialTheme.motionScheme

    val active = session as? CaffeineSession.On
    // Seeded once from real values: settings are already loaded and the session is live state.
    var draft by remember { mutableStateOf(active?.duration ?: lastDuration) }
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    LaunchedEffect(Unit) {
        CaffeinateHaptics.dialogOpen(haptic, context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(motion.fastEffectsSpec()) +
                scaleIn(motion.defaultSpatialSpec(), initialScale = 0.88f),
        ) {
            Surface(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { /* consume scrim dismiss */ },
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (active != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = MaterialShapes.Cookie9Sided.toShape(),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Coffee,
                            contentDescription = null,
                            tint = if (active != null) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.duration_picker_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        if (active != null) {
                            Text(
                                text = active.remainingMillis
                                    ?.let { CaffeineSession.formatCountdown(it) }
                                    ?.let { stringResource(R.string.duration_picker_status_active, it) }
                                    ?: stringResource(R.string.status_on),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    DurationSelector(
                        duration = draft,
                        onDurationChange = { draft = it },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (active != null) {
                            OutlinedButton(
                                onClick = {
                                    CaffeinateHaptics.toggleOff(haptic, context)
                                    onStop()
                                },
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Text(stringResource(R.string.action_stop))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = onDismiss,
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                CaffeinateHaptics.toggleOn(haptic, context)
                                onConfirm(draft)
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(
                                stringResource(
                                    if (active != null) R.string.duration_update else R.string.duration_start,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
