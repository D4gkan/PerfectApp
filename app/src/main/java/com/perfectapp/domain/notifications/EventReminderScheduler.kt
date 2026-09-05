package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.WorkManager
import java.time.LocalDateTime
import com.perfectapp.data.entities.CalendarEventEntity

class EventReminderScheduler(private val context: Context) {
    fun schedule(event: CalendarEventEntity) {
        cancel(event.id)
        val minutes = event.reminderMinutesBefore ?: return
        val alertAt = event.dateTime.minusMinutes(minutes.toLong())
        if (!alertAt.isAfter(LocalDateTime.now())) return
        NotificationAlarmScheduler(context).scheduleEvent(event.id, alertAt)
    }

    fun cancel(eventId: Long) {
        NotificationAlarmScheduler(context).cancelEvent(eventId)
        WorkManager.getInstance(context).cancelUniqueWork(workName(eventId))
    }

    private fun workName(eventId: Long) = "calendar_reminder_$eventId"
}
