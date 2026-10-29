package com.maciejhetman.caffeinate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.navigation.CaffeinateNavHost
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme
import com.maciejhetman.caffeinate.ui.theme.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op: session still works; notification may be limited */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = CaffeineController.get(this)
        // Keeps the platform night mode in step with the saved theme (e.g. after an update).
        controller.settings.value.themeMode.applyToPlatform(this)
        maybeRequestNotificationPermission()
        setContent {
            val settings by controller.settings.collectAsStateWithLifecycle()
            val darkTheme = isAppInDarkTheme(settings.themeMode)

            // System bar icons follow the app's theme, not the system's, when one is forced.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LightNavigationScrim,
                        DarkNavigationScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            CaffeinateTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                val pageColor = CaffeinateTheme.surfaces.page.toArgb()
                // Keep the window in sync so no gap or transition ever shows a mismatched color.
                SideEffect { window.decorView.setBackgroundColor(pageColor) }
                CaffeinateNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences(PREFS_UI, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true) }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val PREFS_UI = "caffeinate_ui"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

        // Same scrims androidx.activity uses for three-button navigation by default.
        private val LightNavigationScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        private val DarkNavigationScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
