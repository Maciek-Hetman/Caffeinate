package com.maciejhetman.caffeinate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import kotlin.math.roundToInt

/**
 * Infinite ↔ Timer connected button group with a stepped length slider when Timer is selected.
 * The slider commits on release so dragging does not spam session restarts.
 *
 * Set [timerOnly] to hide the mode switch and always show the length slider (e.g. settings).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationSelector(
    duration: DurationPreset,
    onDurationChange: (DurationPreset) -> Unit,
    modifier: Modifier = Modifier,
    timerOnly: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isTimed = timerOnly || duration.isTimed
    val sliderState = rememberSliderState(
        value = DurationPreset.snapToSliderStep(duration.timedMinutesOrDefault()).toFloat(),
        steps = DurationPreset.SLIDER_STEPS,
        trackRange = DurationPreset.SLIDER_MIN_MINUTES.toFloat()..DurationPreset.MAX_MINUTES.toFloat(),
    )
    val sliderMinutes = sliderState.value.roundToInt()

    // Follow external changes (QS tile, widget, other screen) unless the user is mid-drag.
    LaunchedEffect(duration) {
        if (duration is DurationPreset.Timed && !sliderState.isDragging) {
            sliderState.value = DurationPreset.snapToSliderStep(duration.minutes).toFloat()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!timerOnly) {
            Row(
                modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ModeToggleButton(
                    checked = !isTimed,
                    label = stringResource(R.string.duration_mode_infinite),
                    icon = Icons.Outlined.AllInclusive,
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    onClick = {
                        CaffeinateHaptics.durationSelected(haptic, context)
                        onDurationChange(DurationPreset.Infinite)
                    },
                    modifier = Modifier.weight(1f),
                )
                ModeToggleButton(
                    checked = isTimed,
                    label = stringResource(R.string.duration_mode_timer),
                    icon = Icons.Outlined.Timer,
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    onClick = {
                        CaffeinateHaptics.durationSelected(haptic, context)
                        onDurationChange(DurationPreset.Timed(sliderMinutes))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(
            visible = isTimed,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.duration_length),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = DurationPreset.formatMinutes(sliderMinutes),
                        style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                            fontFeatureSettings = TabularNumbers,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                val formattedLength = DurationPreset.formatMinutes(sliderMinutes)
                val sliderCd = stringResource(R.string.duration_slider_cd, formattedLength)
                Slider(
                    state = sliderState,
                    onValueChange = { value ->
                        val minutes = DurationPreset.snapToSliderStep(value.roundToInt())
                        if (minutes != sliderState.value.roundToInt()) {
                            CaffeinateHaptics.sliderTick(haptic, context)
                        }
                        sliderState.value = minutes.toFloat()
                    },
                    onValueChangeFinished = {
                        onDurationChange(DurationPreset.Timed(sliderState.value.roundToInt()))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = sliderCd
                            stateDescription = formattedLength
                        },
                    track = { state ->
                        SliderDefaults.Track(
                            sliderState = state,
                            trackCornerSize = SliderTrackCorner,
                            modifier = Modifier.height(SliderTrackHeight),
                            // 46 stops would be visual noise; the thumb snaps regardless.
                            drawTick = { _, _ -> },
                        )
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = DurationPreset.formatMinutes(DurationPreset.SLIDER_MIN_MINUTES),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DurationPreset.formatMinutes(DurationPreset.MAX_MINUTES),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeToggleButton(
    checked: Boolean,
    label: String,
    icon: ImageVector,
    shapes: ToggleButtonShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleButton(
        checked = checked,
        // Radio semantics: tapping the selected option keeps it selected.
        onCheckedChange = { if (!checked) onClick() },
        modifier = modifier.semantics { role = Role.RadioButton },
        shapes = shapes,
        icon = {
            Icon(imageVector = icon, contentDescription = null)
        },
    ) {
        Text(label, maxLines = 1)
    }
}

private fun DurationPreset.timedMinutesOrDefault(): Int = when (this) {
    is DurationPreset.Timed -> minutes
    DurationPreset.Infinite -> DurationPreset.DEFAULT_TIMER_MINUTES
}

private val SliderTrackHeight = 24.dp
private val SliderTrackCorner = 8.dp
