package com.maciejhetman.caffeinate.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.maciejhetman.caffeinate.MainActivity
import com.maciejhetman.caffeinate.R
import com.maciejhetman.caffeinate.session.CaffeineController
import com.maciejhetman.caffeinate.session.CaffeineSession
import com.maciejhetman.caffeinate.session.DurationPreset
import com.maciejhetman.caffeinate.ui.haptics.CaffeinateHaptics
import java.text.DateFormat
import java.util.Date
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
    private var screenOffReceiverRegistered = false

    /** True while this instance owns a running session that has not been ended yet. */
    private var sessionRunning = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_OFF) return
            if (CaffeineController.get(context).settings.value.stopOnScreenOff) {
                endSession()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != ACTION_START) {
            // ACTION_STOP, or an unexpected restart without a session to resume.
            endSession()
            return START_NOT_STICKY
        }
        val duration = DurationPreset.fromSerialized(intent.getStringExtra(EXTRA_DURATION))
        val endsAtEpoch = intent.getLongExtra(EXTRA_ENDS_AT_EPOCH, -1L).takeIf { it > 0L }
        beginSession(duration, endsAtEpoch)
        return START_REDELIVER_INTENT
    }

    private fun beginSession(duration: DurationPreset, endsAtEpochMillis: Long?) {
        activeDuration = duration
        endsAtElapsedRealtime = when {
            endsAtEpochMillis != null && duration.isTimed -> {
                val remaining = endsAtEpochMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    // Redelivered after the session would already have ended.
                    endSession()
                    return
                }
                SystemClock.elapsedRealtime() + remaining
            }
            else -> duration.millis?.let { SystemClock.elapsedRealtime() + it }
        }

        if (!startAsForeground()) {
            endSession()
            return
        }
        sessionRunning = true
        acquireWakeLock()
        registerScreenOffReceiver()
        publishState()
        startCountdown()
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenOffReceiverRegistered = true
    }

    private fun unregisterScreenOffReceiver() {
        if (!screenOffReceiverRegistered) return
        unregisterReceiver(screenOffReceiver)
        screenOffReceiverRegistered = false
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        // Infinite sessions have nothing to count; the notification never changes.
        val endAt = endsAtElapsedRealtime ?: return

        countdownJob = lifecycleScope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    CaffeinateHaptics.sessionExpired(this@CaffeineService)
                    endSession()
                    return@launch
                }
                publishState(remaining)
                // Wake on whole-second boundaries so the countdown never skips a digit.
                delay((remaining % 1_000L).takeIf { it > 0L } ?: 1_000L)
            }
        }
    }

    private fun remainingMillis(): Long? =
        endsAtElapsedRealtime?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }

    private fun endsAtEpochMillis(): Long? =
        endsAtElapsedRealtime?.let { System.currentTimeMillis() + (it - SystemClock.elapsedRealtime()) }

    private fun publishState(remainingOverride: Long? = remainingMillis()) {
        val session = CaffeineSession.On(
            duration = activeDuration,
            remainingMillis = if (activeDuration.isTimed) remainingOverride else null,
            endsAtEpochMillis = endsAtEpochMillis(),
        )
        CaffeineController.get(this).publishSession(session)
    }

    private fun endSession() {
        countdownJob?.cancel()
        countdownJob = null
        endsAtElapsedRealtime = null
        sessionRunning = false
        unregisterScreenOffReceiver()
        releaseWakeLock()
        CaffeineController.get(this).publishSession(CaffeineSession.Off)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // No timeout on purpose: an infinite session must hold the lock until the user stops it,
    // and the system drops it anyway if this process dies.
    @SuppressLint("WakelockTimeout")
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

    /** Returns false when the platform refuses the foreground start (e.g. restarted in background). */
    private fun startAsForeground(): Boolean {
        ensureChannel()
        val notification = buildNotification()
        return try {
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
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Minimal, low-priority notification — required by Android to keep the foreground service
     * alive. Timed sessions use a system-rendered countdown chronometer, so the notification is
     * posted once per session instead of every second.
     */
    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, CaffeineService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val endsAt = endsAtEpochMillis()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_caffeine_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(openApp)
            .addAction(
                R.drawable.ic_caffeine_notification,
                getString(R.string.action_stop),
                stopPending,
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (endsAt != null && activeDuration.isTimed) {
            val endTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAt))
            builder
                .setContentText(getString(R.string.status_until, endTime))
                .setWhen(endsAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder
                .setContentText(getString(R.string.notification_infinite))
                .setShowWhen(false)
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        unregisterScreenOffReceiver()
        releaseWakeLock()
        // Only report Off if this instance was killed mid-session. After endSession() a new
        // session may already be starting, and publishing Off here would clobber it.
        if (sessionRunning) {
            sessionRunning = false
            CaffeineController.get(this).publishSession(CaffeineSession.Off)
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.maciejhetman.caffeinate.action.START"
        const val ACTION_STOP = "com.maciejhetman.caffeinate.action.STOP"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_ENDS_AT_EPOCH = "extra_ends_at_epoch"
        private const val CHANNEL_ID = "caffeine_session"
        private const val NOTIFICATION_ID = 42
    }
}
