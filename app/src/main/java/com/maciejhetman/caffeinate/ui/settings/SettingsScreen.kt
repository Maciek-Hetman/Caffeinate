package com.maciejhetman.caffeinate.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.session.ThemeMode
import com.maciejhetman.caffeinate.ui.components.AppCard
import com.maciejhetman.caffeinate.ui.components.DurationSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = CaffeineController.get(context)
    val widgetTimerDuration by controller.widgetTimerDuration.collectAsStateWithLifecycle(
        initialValue = DurationPreset.DefaultTimed,
    )
    val themeMode by controller.themeMode.collectAsStateWithLifecycle(
        initialValue = ThemeMode.System,
    )
    val dynamicColorEnabled by controller.dynamicColorEnabled.collectAsStateWithLifecycle(
        initialValue = true,
    )
    val stopOnScreenOff by controller.stopOnScreenOff.collectAsStateWithLifecycle(
        initialValue = false,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                .padding(24.dp),
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SettingsGroupTitle(stringResource(R.string.settings_theme_title))
                    ThemeModeSelector(
                        themeMode = themeMode,
                        onThemeModeChange = { controller.setThemeMode(it) },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_dynamic_color_title),
                            checked = dynamicColorEnabled,
                            onCheckedChange = { controller.setDynamicColorEnabled(it) },
                        )
                    }

                    SettingsDivider()

                    SettingsGroupTitle(stringResource(R.string.settings_session_title))
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_stop_on_screen_off_title),
                        checked = stopOnScreenOff,
                        onCheckedChange = { controller.setStopOnScreenOff(it) },
                    )

                    SettingsDivider()

                    SettingsGroupTitle(stringResource(R.string.settings_widget_timer_title))
                    DurationSelector(
                        duration = widgetTimerDuration,
                        onDurationChange = { duration ->
                            if (duration is DurationPreset.Timed) {
                                controller.setWidgetTimerDuration(duration)
                            }
                        },
                        timerOnly = true,
                        animateTimerReveal = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Switch }
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = themeMode == mode,
                onClick = { onThemeModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(stringResource(mode.labelRes()))
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_mode_system
    ThemeMode.Light -> R.string.theme_mode_light
    ThemeMode.Dark -> R.string.theme_mode_dark
}
