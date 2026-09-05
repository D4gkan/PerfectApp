package com.perfectapp.domain.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules notification wake-ups with AlarmManager so a clock-time alert does not depend on
 * the app already being open. Exact delivery is used when Android allows it; otherwise the
 * idle-safe alarm API is used and Android may deliver the alert a little later.
 */
class NotificationAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleDaily(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        schedule(
            triggerAtMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent = pendingIntent(
                kind = KIND_DAILY,
                extras = mapOf(EXTRA_HOUR to hour, EXTRA_MINUTE to minute)
            )
        )
    }

    fun scheduleEvent(eventId: Long, alertAt: LocalDateTime) {
        schedule(
            triggerAtMillis = alertAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent = pendingIntent(KIND_EVENT, eventId)
        )
    }

    fun cancelEvent(eventId: Long) = alarmManager.cancel(pendingIntent(KIND_EVENT, eventId))

    fun scheduleDueDate(type: String, id: Long, alertAt: LocalDateTime) {
        schedule(
            triggerAtMillis = alertAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent = pendingIntent("due_$type", id, mapOf(EXTRA_TYPE to type))
        )
    }

    fun cancelDueDate(type: String, id: Long) = alarmManager.cancel(pendingIntent("due_$type", id))

    private fun schedule(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(
        kind: String,
        id: Long? = null,
        extras: Map<String, Any> = emptyMap()
    ): PendingIntent {
        val identity = if (id == null) kind else "${kind}_$id"
        val intent = Intent(appContext, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_ALARM
            data = Uri.parse("perfectapp://notification/$identity")
            if (id != null) putExtra(EXTRA_ID, id)
            extras.forEach { (key, value) ->
                when (value) {
                    is Int -> putExtra(key, value)
                    is String -> putExtra(key, value)
                }
            }
            putExtra(EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_NOTIFICATION_ALARM = "com.perfectapp.action.NOTIFICATION_ALARM"
        const val KIND_DAILY = "daily"
        const val KIND_EVENT = "event"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_TYPE = "type"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
    }
}
