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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import kotlin.math.roundToInt

/**
 * Infinite ↔ Timer mode switch with a minute slider when Timer is selected.
 * Slider commits on release so dragging does not spam session restarts.
 *
 * Set [timerOnly] to hide Infinite and always show the length slider (e.g. settings).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationSelector(
    duration: DurationPreset,
    onDurationChange: (DurationPreset) -> Unit,
    modifier: Modifier = Modifier,
    animateTimerReveal: Boolean = true,
    timerOnly: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isTimed = timerOnly || duration.isTimed
    var sliderMinutes by remember {
        mutableIntStateOf(
            when (duration) {
                is DurationPreset.Timed -> duration.minutes
                DurationPreset.Infinite -> DurationPreset.DEFAULT_TIMER_MINUTES
            },
        )
    }
    var sliderValue by remember {
        mutableFloatStateOf(sliderMinutes.toFloat())
    }

    LaunchedEffect(duration) {
        if (duration is DurationPreset.Timed) {
            sliderMinutes = duration.minutes
            sliderValue = duration.minutes.toFloat()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.duration_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!timerOnly) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isTimed,
                    onClick = {
                        if (isTimed) {
                            CaffeinateHaptics.durationSelected(haptic, context)
                            onDurationChange(DurationPreset.Infinite)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(stringResource(R.string.duration_mode_infinite))
                }
                SegmentedButton(
                    selected = isTimed,
                    onClick = {
                        if (!isTimed) {
                            CaffeinateHaptics.durationSelected(haptic, context)
                            onDurationChange(DurationPreset.Timed(sliderMinutes))
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(stringResource(R.string.duration_mode_timer))
                }
            }
        }

        AnimatedVisibility(
            visible = isTimed,
            enter = if (animateTimerReveal) {
                fadeIn() + expandVertically()
            } else {
                fadeIn()
            },
            exit = if (animateTimerReveal) {
                fadeOut() + shrinkVertically()
            } else {
                fadeOut()
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.duration_timer_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DurationPreset.formatMinutes(sliderMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val sliderCd = stringResource(
                    R.string.duration_slider_cd,
                    DurationPreset.formatMinutes(sliderMinutes),
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        val minutes = value.roundToInt()
                            .coerceIn(DurationPreset.MIN_MINUTES, DurationPreset.MAX_MINUTES)
                        sliderValue = minutes.toFloat()
                        sliderMinutes = minutes
                    },
                    onValueChangeFinished = {
                        CaffeinateHaptics.sliderTick(haptic, context)
                        onDurationChange(DurationPreset.Timed(sliderMinutes))
                    },
                    valueRange = DurationPreset.MIN_MINUTES.toFloat()..
                        DurationPreset.MAX_MINUTES.toFloat(),
                    steps = DurationPreset.MAX_MINUTES - DurationPreset.MIN_MINUTES - 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = sliderCd },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = DurationPreset.formatMinutes(DurationPreset.MIN_MINUTES),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DurationPreset.formatMinutes(DurationPreset.MAX_MINUTES),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
