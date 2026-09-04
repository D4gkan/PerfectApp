package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import com.perfectapp.data.entities.CalendarEventEntity

class EventReminderScheduler(private val context: Context) {
    fun schedule(event: CalendarEventEntity) {
        cancel(event.id)
        val minutes = event.reminderMinutesBefore ?: return
        val alertAt = event.dateTime.minusMinutes(minutes.toLong())
        if (!alertAt.isAfter(LocalDateTime.now())) return
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInputData(Data.Builder().putLong(EventReminderWorker.KEY_EVENT_ID, event.id).build())
            .setInitialDelay(Duration.between(LocalDateTime.now(), alertAt).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(event.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(eventId: Long) = WorkManager.getInstance(context).cancelUniqueWork(workName(eventId))

    private fun workName(eventId: Long) = "calendar_reminder_$eventId"
}
