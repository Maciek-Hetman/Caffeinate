package com.maciejhetman.caffeinate.tile.dialog

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
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
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        setContent {
            CaffeinateTheme {
                DurationPickerContent(
                    onDismiss = { finish() },
                    onConfirm = { duration ->
                        CaffeineController.get(this).start(duration)
                        finish()
                    },
                    onStop = {
                        CaffeineController.get(this).stop()
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

@Composable
private fun DurationPickerContent(
    onDismiss: () -> Unit,
    onConfirm: (DurationPreset) -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val controller = CaffeineController.get(context)
    val session by controller.session.collectAsStateWithLifecycle()
    val lastDuration by controller.lastDuration.collectAsStateWithLifecycle(
        initialValue = DurationPreset.Default,
    )

    val isActive = session.isActive
    val initialDuration = when (val s = session) {
        is CaffeineSession.On -> s.duration
        CaffeineSession.Off -> lastDuration
    }
    var draft by remember { mutableStateOf(initialDuration) }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(initialDuration) {
        if (!seeded) {
            draft = initialDuration
            seeded = true
        }
    }

    LaunchedEffect(Unit) {
        CaffeinateHaptics.dialogOpen(haptic, context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* consume scrim dismiss */ },
                ),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = if (isActive) 2.dp else 6.dp,
            color = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ) {
            val onSurface = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val onVariant = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isActive) {
                            R.string.duration_picker_title_active
                        } else {
                            R.string.duration_picker_title
                        },
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = onSurface,
                )

                if (isActive) {
                    val active = session as CaffeineSession.On
                    val statusText = when (active.duration) {
                        DurationPreset.Infinite -> stringResource(R.string.status_on)
                        is DurationPreset.Timed -> stringResource(
                            R.string.duration_picker_status_active,
                            active.displayRemaining(),
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = onSurface,
                    )
                }

                Text(
                    text = stringResource(
                        if (isActive) {
                            R.string.duration_picker_body_active
                        } else {
                            R.string.duration_picker_body
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onVariant,
                )

                DurationSelector(
                    duration = draft,
                    onDurationChange = { draft = it },
                    animateTimerReveal = false,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isActive) {
                        TextButton(
                            onClick = {
                                CaffeinateHaptics.toggleOff(haptic, context)
                                onStop()
                            },
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            CaffeinateHaptics.toggleOn(haptic, context)
                            onConfirm(draft)
                        },
                    ) {
                        Text(
                            stringResource(
                                if (isActive) {
                                    R.string.duration_update
                                } else {
                                    R.string.duration_start
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}
