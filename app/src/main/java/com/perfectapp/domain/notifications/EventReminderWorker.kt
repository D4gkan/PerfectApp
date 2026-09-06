package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.perfectapp.AppContainer
import kotlinx.coroutines.flow.first

/** Best-effort, one-time alert for a calendar item's user-selected lead time. */
class EventReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val container: AppContainer
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = container.settingsRepository.settings.first()
        if (!settings.notificationsEnabled || !settings.calendarNotifications) return Result.success()
        val id = inputData.getLong(KEY_EVENT_ID, 0L)
        val event = container.calendarRepository.eventById(id) ?: return Result.success()
        if (event.reminderMinutesBefore == null) return Result.success()
        val expected = inputData.getString("alert_time")
        if (expected != null && event.dateTime.minusMinutes(event.reminderMinutesBefore.toLong()).toString() != expected) return Result.success()
        NotificationHelper(applicationContext).showNotification(
            id = NotificationHelper.eventNotificationId(event.id),
            title = event.title,
            text = if (event.isAllDay) "Scheduled for today" else "Starts at ${event.dateTime.toLocalTime()}",
            channelId = NotificationHelper.CALENDAR_CHANNEL_ID
        )
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_EVENT_ID = "event_id"
    }
}
