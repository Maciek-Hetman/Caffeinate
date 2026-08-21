package com.maciejhetman.caffeinate.tile.dialog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.components.DurationSelector
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme

/**
 * Dialog-themed activity launched via ACTION_QS_TILE_PREFERENCES (QS long-press).
 */
class DurationPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaffeinateTheme {
                DurationPickerDialog(
                    onDismiss = { finish() },
                    onStart = { duration ->
                        CaffeineController.get(this).start(duration)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun DurationPickerDialog(
    onDismiss: () -> Unit,
    onStart: (DurationPreset) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val controller = CaffeineController.get(context)
    val lastDuration by controller.lastDuration.collectAsStateWithLifecycle(
        initialValue = DurationPreset.Default,
    )
    var draft by remember(lastDuration) { mutableStateOf(lastDuration) }

    LaunchedEffect(Unit) {
        CaffeinateHaptics.dialogOpen(haptic, context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.duration_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.duration_picker_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DurationSelector(
                    duration = draft,
                    onDurationChange = { draft = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            CaffeinateHaptics.toggleOn(haptic, context)
                            onStart(draft)
                        },
                    ) {
                        Text(stringResource(R.string.duration_start))
                    }
                }
            }
        }
    }
}
