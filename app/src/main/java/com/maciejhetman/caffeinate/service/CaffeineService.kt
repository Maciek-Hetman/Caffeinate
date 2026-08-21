package com.maciejhetman.caffeinate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.maciejhetman.caffeinate.MainActivity
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that holds a SCREEN_BRIGHT_WAKE_LOCK and runs timed countdowns.
 */
class CaffeineService : LifecycleService() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var countdownJob: Job? = null
    private var activeDuration: DurationPreset = DurationPreset.Default
    private var endsAtElapsedRealtime: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                endSession()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                val duration = DurationPreset.fromSerialized(intent?.getStringExtra(EXTRA_DURATION))
                beginSession(duration)
            }
        }
        return START_STICKY
    }

    private fun beginSession(duration: DurationPreset) {
        activeDuration = duration
        endsAtElapsedRealtime = duration.millis?.let { SystemClock.elapsedRealtime() + it }

        startAsForeground(remainingForNotification())
        acquireWakeLock()
        publishState()
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val endAt = endsAtElapsedRealtime ?: run {
            // Infinite: still refresh notification periodically with ∞
            countdownJob = lifecycleScope.launch {
                while (isActive) {
                    updateNotification(null)
                    delay(30_000L)
                }
            }
            return
        }

        countdownJob = lifecycleScope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics.sessionExpired(this@CaffeineService)
                    endSession()
                    return@launch
                }
                publishState(remaining)
                updateNotification(remaining)
                delay(1_000L)
            }
        }
    }

    private fun remainingForNotification(): Long? =
        endsAtElapsedRealtime?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }

    private fun publishState(remainingOverride: Long? = remainingForNotification()) {
        val session = CaffeineSession.On(
            duration = activeDuration,
            remainingMillis = if (activeDuration.isTimed) remainingOverride else null,
            endsAtEpochMillis = endsAtElapsedRealtime?.let {
                System.currentTimeMillis() + (it - SystemClock.elapsedRealtime())
            },
        )
        CaffeineController.get(this).publishSession(session)
    }

    private fun endSession() {
        countdownJob?.cancel()
        countdownJob = null
        endsAtElapsedRealtime = null
        releaseWakeLock()
        CaffeineController.get(this).publishSession(CaffeineSession.Off)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
            "Caffeinate::ScreenWakeLock",
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun startAsForeground(remainingMillis: Long?) {
        ensureChannel()
        val notification = buildNotification(remainingMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(remainingMillis: Long?) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remainingMillis))
    }

    private fun buildNotification(remainingMillis: Long?): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CaffeineService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val remainingText = when {
            activeDuration is DurationPreset.Infinite || remainingMillis == null -> "∞"
            else -> CaffeineSession.formatCountdown(remainingMillis)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_caffeine_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_remaining, remainingText))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        releaseWakeLock()
        if (CaffeineController.get(this).session.value.isActive) {
            CaffeineController.get(this).publishSession(CaffeineSession.Off)
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.maciejhetman.caffeinate.action.START"
        const val ACTION_STOP = "com.maciejhetman.caffeinate.action.STOP"
        const val EXTRA_DURATION = "extra_duration"
        private const val CHANNEL_ID = "caffeine_session"
        private const val NOTIFICATION_ID = 42
    }
}
