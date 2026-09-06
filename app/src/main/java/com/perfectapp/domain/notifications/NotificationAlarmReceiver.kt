package com.perfectapp.domain.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Turns an AlarmManager wake-up into a short, retryable unit of background work. */
class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationAlarmScheduler.ACTION_NOTIFICATION_ALARM) return

        val workManager = WorkManager.getInstance(context)
        when (val kind = intent.getStringExtra(NotificationAlarmScheduler.EXTRA_KIND)) {
            NotificationAlarmScheduler.KIND_DAILY -> {
                val hour = intent.getIntExtra(NotificationAlarmScheduler.EXTRA_HOUR, 8)
                val minute = intent.getIntExtra(NotificationAlarmScheduler.EXTRA_MINUTE, 0)
                // AlarmManager alarms are one-shot, so install tomorrow's wake-up first.
                NotificationAlarmScheduler(context).scheduleDaily(hour, minute)
                workManager.enqueueUniqueWork(
                    DailySummaryWorker.ALARM_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DailySummaryWorker>().build()
                )
            }

            NotificationAlarmScheduler.KIND_EVENT -> {
                val id = intent.getLongExtra(NotificationAlarmScheduler.EXTRA_ID, 0L)
                if (id == 0L) return
                if (intent.getIntExtra("ring", 0) == 1) {
                    try {
                        androidx.core.content.ContextCompat.startForegroundService(context,
                            Intent(context, EventAlarmService::class.java)
                                .putExtra("id", id).putExtra("event_time", intent.getStringExtra("event_time")))
                        return
                    } catch (_: IllegalStateException) {
                        // Without exact-alarm access Android may deny a background service.
                    }
                }
                val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                    .setInputData(Data.Builder().putLong(EventReminderWorker.KEY_EVENT_ID, id)
                        .putString("alert_time", intent.getStringExtra("event_time")).build())
                    .build()
                workManager.enqueueUniqueWork("event_alarm_delivery_$id", ExistingWorkPolicy.REPLACE, request)
            }

            else -> if (kind?.startsWith("due_") == true) {
                val id = intent.getLongExtra(NotificationAlarmScheduler.EXTRA_ID, 0L)
                val type = intent.getStringExtra(NotificationAlarmScheduler.EXTRA_TYPE) ?: return
                if (id == 0L) return
                val request = OneTimeWorkRequestBuilder<DueDateReminderWorker>()
                    .setInputData(
                        Data.Builder()
                            .putLong(DueDateReminderWorker.KEY_ID, id)
                            .putString(DueDateReminderWorker.KEY_TYPE, type)
                            .build()
                    )
                    .build()
                workManager.enqueueUniqueWork("due_alarm_delivery_${type}_$id", ExistingWorkPolicy.REPLACE, request)
            }
        }
    }
}
