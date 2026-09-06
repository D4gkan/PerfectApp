package com.perfectapp.domain.notifications

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.perfectapp.PerfectApp
import com.perfectapp.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Owns ringing independently of the alarm screen, until the user presses Stop. */
class EventAlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var pendingLoads = 0
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            active.value = active.value - intent.getLongExtra("id", 0)
            if (active.value.isEmpty() && pendingLoads == 0) stopSelf() else if (active.value.isNotEmpty()) refresh()
            return START_NOT_STICKY
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Event alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Event-time alarms that ring until stopped"
            setSound(null, null)
        })
        startForeground(NOTIFICATION_ID, notification())
        val id = intent?.getLongExtra("id", 0) ?: 0
        val expectedTime = intent?.getStringExtra("event_time")
        pendingLoads++
        scope.launch {
            try {
                val container = (application as PerfectApp).container
                val event = container.calendarRepository.eventById(id)
                val settings = container.settingsRepository.settings.first()
                if (event != null && event.reminderMinutesBefore == 0 &&
                    event.dateTime.toString() == expectedTime && settings.notificationsEnabled && settings.calendarNotifications) {
                    active.value = active.value + (id to event.title)
                    refresh()
                    ring()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.e("EventAlarm", "Unable to load event alarm", error)
            } finally {
                pendingLoads--
                if (active.value.isEmpty() && pendingLoads == 0) stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val entry = active.value.entries.firstOrNull()
        val open = PendingIntent.getActivity(this, 0, Intent(this, EventAlarmActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(entry?.value ?: "Event alarm")
            .setContentText("Alarm ringing — tap Stop to silence")
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true).setAutoCancel(false).setContentIntent(open)
            .setFullScreenIntent(open, true)
        if (entry != null) {
            val stop = PendingIntent.getService(this, 0, Intent(this, EventAlarmService::class.java)
                .setAction(STOP).putExtra("id", entry.key), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Stop", stop)
        }
        return builder.build()
    }

    private fun refresh() = getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())

    private fun ring() {
        if (player != null) return
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(attributes)
            next.setDataSource(this, sound)
            next.isLooping = true
            next.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            next.prepare()
            next.start()
            player = next
        } catch (_: Exception) { next.release() }
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attributes)
    }

    override fun onDestroy() {
        scope.cancel()
        player?.release()
        vibrator?.cancel()
        active.value = emptyMap()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val STOP = "com.perfectapp.STOP_EVENT_ALARM"
        private const val CHANNEL = "event_alarms"
        private const val NOTIFICATION_ID = 900001
        val active = MutableStateFlow<Map<Long, String>>(emptyMap())
    }
}
