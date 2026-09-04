package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.entities.SubscriptionEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DueDateReminderScheduler(private val context: Context) {
    fun scheduleRenewal(item: ReminderEntity) = schedule(item.id, DueDateReminderWorker.TYPE_RENEWAL, item.dueDate)
    fun scheduleSubscription(item: SubscriptionEntity) = schedule(item.id, DueDateReminderWorker.TYPE_SUBSCRIPTION, item.nextChargeDate)
    fun cancelRenewal(id: Long) = cancel(DueDateReminderWorker.TYPE_RENEWAL, id)
    fun cancelSubscription(id: Long) = cancel(DueDateReminderWorker.TYPE_SUBSCRIPTION, id)

    private fun schedule(id: Long, type: String, dueDate: LocalDate) {
        cancel(type, id)
        // Use 9 AM three days ahead; a missed lead time is handled by the daily safety net.
        val alertAt = dueDate.minusDays(3).atTime(LocalTime.of(9, 0))
        if (!alertAt.isAfter(LocalDateTime.now())) return
        val request = OneTimeWorkRequestBuilder<DueDateReminderWorker>()
            .setInputData(Data.Builder().putLong(DueDateReminderWorker.KEY_ID, id).putString(DueDateReminderWorker.KEY_TYPE, type).build())
            .setInitialDelay(Duration.between(LocalDateTime.now(), alertAt).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(type, id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancel(type: String, id: Long) = WorkManager.getInstance(context).cancelUniqueWork(workName(type, id))
    private fun workName(type: String, id: Long) = "${type}_reminder_$id"
}
