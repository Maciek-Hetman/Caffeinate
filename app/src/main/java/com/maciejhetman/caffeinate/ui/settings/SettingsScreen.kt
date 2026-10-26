package com.maciejhetman.caffeinate.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.maciejhetman.caffeinate.ui.components.DurationSelector
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = CaffeineController.get(context)
    val settings by controller.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val page = CaffeinateTheme.surfaces.page
    val card = CaffeinateTheme.surfaces.card
    val onCard = MaterialTheme.colorScheme.onSurface
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    // A switch row is "checked", not selected: keep it on the card color like its neighbours.
    val itemColors = ListItemDefaults.segmentedColors(
        containerColor = card,
        contentColor = onCard,
        leadingContentColor = iconColor,
        selectedContainerColor = card,
        selectedContentColor = onCard,
        selectedLeadingContentColor = iconColor,
    )
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = page,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = page,
                    scrolledContainerColor = page,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val appearanceCount = if (supportsDynamicColor) 2 else 1
                SettingsSection(title = stringResource(R.string.settings_appearance_title)) {
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = appearanceCount),
                        colors = itemColors,
                        verticalAlignment = Alignment.Top,
                        leadingContent = { SettingsIcon(Icons.Outlined.DarkMode) },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.settings_theme_title))
                            ThemeModeSelector(
                                themeMode = settings.themeMode,
                                onThemeModeChange = { controller.setThemeMode(it) },
                            )
                        }
                    }
                    if (supportsDynamicColor) {
                        SwitchItem(
                            title = stringResource(R.string.settings_dynamic_color_title),
                            icon = Icons.Outlined.Palette,
                            checked = settings.dynamicColor,
                            onCheckedChange = { controller.setDynamicColorEnabled(it) },
                            index = 1,
                            count = appearanceCount,
                            colors = itemColors,
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_session_title)) {
                    SwitchItem(
                        title = stringResource(R.string.settings_stop_on_screen_off_title),
                        icon = Icons.Outlined.PowerSettingsNew,
                        checked = settings.stopOnScreenOff,
                        onCheckedChange = { controller.setStopOnScreenOff(it) },
                        index = 0,
                        count = 1,
                        colors = itemColors,
                    )
                }

                SettingsSection(title = stringResource(R.string.settings_widget_timer_title)) {
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                        colors = itemColors,
                        verticalAlignment = Alignment.Top,
                        leadingContent = { SettingsIcon(Icons.Outlined.Timer) },
                    ) {
                        DurationSelector(
                            duration = settings.widgetTimerDuration,
                            onDurationChange = { duration ->
                                if (duration is DurationPreset.Timed) {
                                    controller.setWidgetTimerDuration(duration)
                                }
                            },
                            timerOnly = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    colors: ListItemColors,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = colors,
        leadingContent = { SettingsIcon(icon) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        },
    ) {
        Text(title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val modes = ThemeMode.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = themeMode == mode,
                onCheckedChange = { if (themeMode != mode) onThemeModeChange(mode) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(stringResource(mode.labelRes()), maxLines = 1)
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_mode_system
    ThemeMode.Light -> R.string.theme_mode_light
    ThemeMode.Dark -> R.string.theme_mode_dark
}
