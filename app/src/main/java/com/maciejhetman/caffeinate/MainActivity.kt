package com.maciejhetman.caffeinate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciejhetman.caffeinate.navigation.CaffeinateNavHost
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.ThemeMode
import com.maciejhetman.caffeinate.ui.theme.CaffeinateTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op: session still works; notification may be limited */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        setContent {
            val controller = CaffeineController.get(this)
            val themeMode by controller.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.System,
            )
            val dynamicColor by controller.dynamicColorEnabled.collectAsStateWithLifecycle(
                initialValue = true,
            )
            CaffeinateTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                CaffeinateNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
