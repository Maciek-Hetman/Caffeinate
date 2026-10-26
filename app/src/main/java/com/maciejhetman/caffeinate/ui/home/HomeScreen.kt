package com.maciejhetman.caffeinate.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.components.DurationSelector
import com.maciejhetman.caffeinate.ui.components.MorphShape
import com.maciejhetman.caffeinate.ui.components.SectionCard
import com.maciejhetman.caffeinate.ui.components.TabularNumbers
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

private enum class SessionStatusKind {
    Off,
    InfiniteOn,
    TimedOn,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val controller = CaffeineController.get(context)
    val session by controller.session.collectAsStateWithLifecycle()
    val settings by controller.settings.collectAsStateWithLifecycle()

    val isOn = session.isActive
    val selectedDuration = (session as? CaffeineSession.On)?.duration ?: settings.lastDuration

    val onToggle = {
        if (isOn) {
            CaffeinateHaptics.toggleOff(haptic, context)
            controller.stop()
        } else {
            CaffeinateHaptics.toggleOn(haptic, context)
            controller.start(selectedDuration)
        }
    }
    val onDurationChange = { duration: DurationPreset ->
        if (isOn) controller.start(duration) else controller.setLastDuration(duration)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CaffeinateTheme.surfaces.page,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val twoPane = maxWidth >= 600.dp && maxWidth > maxHeight
            // Shrink the hero so it and the status both fit a phone held sideways.
            val twoPaneHeroSize = (maxHeight - TwoPaneStatusAllowance).coerceIn(HeroMinSize, HeroRingSize)
            if (twoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HeroToggle(session = session, onToggle = onToggle, size = twoPaneHeroSize)
                        SessionStatus(session = session)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                    ) {
                        DurationCard(selectedDuration, onDurationChange)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                ) {
                    HeroToggle(session = session, onToggle = onToggle)
                    SessionStatus(session = session)
                    DurationCard(
                        duration = selectedDuration,
                        onDurationChange = onDurationChange,
                        modifier = Modifier.widthIn(max = 560.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationCard(
    duration: DurationPreset,
    onDurationChange: (DurationPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        DurationSelector(
            duration = duration,
            onDurationChange = onDurationChange,
        )
    }
}

/**
 * The big on/off control: a Material cookie that morphs into a sunburst and slowly spins while
 * the screen is held awake, wrapped in a wavy ring that drains as a timed session runs out.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroToggle(
    session: CaffeineSession,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HeroRingSize,
) {
    val shapeSize = size * HeroShapeRatio
    val iconSize = size * HeroIconRatio
    val isOn = session.isActive
    val remainingFraction = (session as? CaffeineSession.On)?.remainingFraction
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny) }
    val morphProgress by animateFloatAsState(
        targetValue = if (isOn) 1f else 0f,
        animationSpec = motion.slowSpatialSpec(),
        label = "heroMorph",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = motion.fastSpatialSpec(),
        label = "heroPress",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isOn) colors.primary else colors.secondaryContainer,
        animationSpec = motion.defaultEffectsSpec(),
        label = "heroContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOn) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = motion.defaultEffectsSpec(),
        label = "heroContent",
    )
    val rotation by rememberSpin(spinning = isOn)
    val appName = stringResource(R.string.app_name)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = remainingFraction != null,
            enter = fadeIn(motion.defaultEffectsSpec()) +
                scaleIn(motion.defaultSpatialSpec(), initialScale = 0.85f),
            exit = fadeOut(motion.fastEffectsSpec()) +
                scaleOut(motion.defaultSpatialSpec(), targetScale = 0.85f),
        ) {
            // The service ticks once a second; glide between ticks instead of stepping.
            val fraction by animateFloatAsState(
                targetValue = remainingFraction ?: 0f,
                animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
                label = "heroRing",
            )
            val stroke = with(LocalDensity.current) {
                Stroke(width = HeroRingStroke.toPx(), cap = StrokeCap.Round)
            }
            CircularWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(size),
                color = colors.primary,
                trackColor = colors.secondaryContainer,
                stroke = stroke,
                trackStroke = stroke,
                wavelength = HeroRingWavelength,
            )
        }

        Box(
            modifier = Modifier
                .size(shapeSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .toggleable(
                    value = isOn,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                )
                .semantics { contentDescription = appName },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        rotationZ = rotation
                        shape = MorphShape(morph, morphProgress)
                        clip = true
                    }
                    .background(containerColor)
                    .indication(interactionSource, ripple(color = contentColor)),
            )
            AnimatedContent(
                targetState = isOn,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) +
                        scaleIn(motion.fastSpatialSpec(), initialScale = 0.7f)) togetherWith
                        (fadeOut(motion.fastEffectsSpec()) +
                            scaleOut(motion.fastSpatialSpec(), targetScale = 1.15f))
                },
                label = "heroIcon",
            ) { on ->
                Icon(
                    imageVector = if (on) Icons.Filled.Coffee else Icons.Outlined.Coffee,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

/**
 * Continuous slow rotation while [spinning]; when it stops, eases onto the nearest angle where
 * the 9-sided cookie looks upright. Skipped entirely when system animations are off.
 */
@Composable
private fun rememberSpin(spinning: Boolean): State<Float> {
    val rotation = remember { Animatable(0f) }
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    LaunchedEffect(spinning, animationsEnabled) {
        if (spinning && animationsEnabled) {
            while (isActive) {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = SpinPeriodMs, easing = LinearEasing),
                )
            }
        } else {
            val step = 360f / 9
            rotation.animateTo(
                targetValue = (rotation.value / step).roundToInt() * step,
                animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
            )
        }
    }
    return rotation.asState()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionStatus(session: CaffeineSession) {
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val statusKind = when (session) {
        CaffeineSession.Off -> SessionStatusKind.Off
        is CaffeineSession.On -> if (session.duration.isTimed) {
            SessionStatusKind.TimedOn
        } else {
            SessionStatusKind.InfiniteOn
        }
    }

    // Animate only when the kind changes; the countdown itself just updates in place.
    AnimatedContent(
        targetState = statusKind,
        transitionSpec = {
            (fadeIn(motion.defaultEffectsSpec()) +
                slideInVertically(motion.defaultSpatialSpec()) { it / 3 }) togetherWith
                (fadeOut(motion.fastEffectsSpec()) +
                    slideOutVertically(motion.defaultSpatialSpec()) { -it / 3 }) using
                SizeTransform(clip = false)
        },
        label = "statusKind",
    ) { kind ->
        val active = session as? CaffeineSession.On
        val headline = when (kind) {
            SessionStatusKind.Off -> stringResource(R.string.status_off)
            SessionStatusKind.InfiniteOn -> stringResource(R.string.status_infinite)
            SessionStatusKind.TimedOn -> active?.remainingMillis
                ?.let { CaffeineSession.formatCountdown(it) }
                ?: stringResource(R.string.status_on)
        }
        val supporting = when (kind) {
            SessionStatusKind.Off -> stringResource(R.string.status_tap_to_start)
            SessionStatusKind.InfiniteOn -> stringResource(R.string.status_screen_stays_awake)
            SessionStatusKind.TimedOn -> active?.endsAtEpochMillis?.let { endsAt ->
                val time = android.text.format.DateFormat.getTimeFormat(context).format(Date(endsAt))
                stringResource(R.string.status_until, time)
            } ?: ""
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displayLargeEmphasized.copy(
                    fontFeatureSettings = TabularNumbers,
                ),
                color = if (kind == SessionStatusKind.Off) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private val HeroRingSize = 280.dp
private val HeroMinSize = 160.dp
private val HeroRingStroke = 8.dp
private val HeroRingWavelength = 40.dp
private const val HeroShapeRatio = 216f / 280f
private const val HeroIconRatio = 88f / 280f

/** Room left under the hero in two-pane mode for the countdown, its caption and spacing. */
private val TwoPaneStatusAllowance = 136.dp
private const val SpinPeriodMs = 24_000
