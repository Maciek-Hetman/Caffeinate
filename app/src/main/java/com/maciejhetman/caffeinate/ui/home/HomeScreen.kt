package com.maciejhetman.caffeinate.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.components.DurationSelector
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import java.text.DateFormat
import java.util.Date

private enum class SessionStatusKind {
    Off,
    InfiniteOn,
    TimedOn,
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val controller = CaffeineController.get(context)
    val session by controller.session.collectAsStateWithLifecycle()
    val lastDuration by controller.lastDuration.collectAsStateWithLifecycle(
        initialValue = DurationPreset.Default,
    )

    val isOn = session.isActive
    val selectedDuration = when (val s = session) {
        is CaffeineSession.On -> s.duration
        CaffeineSession.Off -> lastDuration
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            HeroToggle(
                isOn = isOn,
                onToggle = {
                    if (isOn) {
                        CaffeinateHaptics.toggleOff(haptic, context)
                        controller.stop()
                    } else {
                        CaffeinateHaptics.toggleOn(haptic, context)
                        controller.start(selectedDuration)
                    }
                },
            )

            SessionStatus(session = session)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                DurationSelector(
                    duration = selectedDuration,
                    onDurationChange = { duration ->
                        if (isOn) {
                            controller.start(duration)
                        } else {
                            controller.setLastDuration(duration)
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroToggle(
    isOn: Boolean,
    onToggle: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1.08f else 1f,
        label = "toggleScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "toggleColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "toggleContentColor",
    )
    val description = if (isOn) {
        stringResource(R.string.toggle_on_cd)
    } else {
        stringResource(R.string.toggle_off_cd)
    }

    FilledIconButton(
        onClick = onToggle,
        modifier = Modifier
            .size(168.dp)
            .scale(scale)
            .semantics { contentDescription = description },
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = if (isOn) Icons.Filled.Coffee else Icons.Outlined.Coffee,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun SessionStatus(session: CaffeineSession) {
    val statusKind = when (session) {
        CaffeineSession.Off -> SessionStatusKind.Off
        is CaffeineSession.On -> if (session.duration is DurationPreset.Infinite) {
            SessionStatusKind.InfiniteOn
        } else {
            SessionStatusKind.TimedOn
        }
    }

    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (statusKind) {
            SessionStatusKind.TimedOn -> {
                val active = session as CaffeineSession.On
                Text(
                    text = active.remainingMillis?.let { CaffeineSession.formatCountdown(it) }
                        ?: stringResource(R.string.status_on),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                AnimatedContent(
                    targetState = statusKind,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "statusKind",
                ) { kind ->
                    val primaryText = when (kind) {
                        SessionStatusKind.Off -> stringResource(R.string.status_off)
                        SessionStatusKind.InfiniteOn -> stringResource(R.string.status_infinite)
                        SessionStatusKind.TimedOn -> stringResource(R.string.status_on)
                    }
                    Text(
                        text = primaryText,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (statusKind != SessionStatusKind.Off) {
            val supportingText = when (val s = session) {
                is CaffeineSession.On -> when {
                    s.duration is DurationPreset.Infinite ->
                        stringResource(R.string.status_screen_stays_awake)
                    s.endsAtEpochMillis != null ->
                        stringResource(
                            R.string.status_until,
                            timeFormatter.format(Date(s.endsAtEpochMillis)),
                        )
                    else -> null
                }
                CaffeineSession.Off -> null
            }
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
