package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.perfectapp.AppContainer
import kotlinx.coroutines.flow.first

/** One-time, best-effort advance reminder for a renewal or subscription. */
class DueDateReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val container: AppContainer
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val settings = container.settingsRepository.settings.first()
        val id = inputData.getLong(KEY_ID, 0L)
        val type = inputData.getString(KEY_TYPE)
        when (type) {
            TYPE_RENEWAL -> {
                val item = container.reminderRepository.all.first().firstOrNull { it.id == id && !it.isCompleted }
                if (settings.notificationsEnabled && settings.renewalNotifications && item != null) {
                    NotificationHelper(applicationContext).showNotification(NotificationHelper.renewalNotificationId(id), "Renewal due soon", "${item.title} is due on ${item.dueDate}")
                }
            }
            TYPE_SUBSCRIPTION -> {
                val item = container.wealthRepository.activeSubscriptions.first().firstOrNull { it.id == id }
                if (settings.notificationsEnabled && settings.subscriptionNotifications && item != null) {
                    NotificationHelper(applicationContext).showNotification(NotificationHelper.subscriptionNotificationId(id), "Upcoming subscription renewal", "${item.name} renews on ${item.nextChargeDate}")
                }
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TYPE = "type"
        const val TYPE_RENEWAL = "renewal"
        const val TYPE_SUBSCRIPTION = "subscription"
    }
}
