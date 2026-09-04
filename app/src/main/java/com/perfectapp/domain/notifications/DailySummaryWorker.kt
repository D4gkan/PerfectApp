package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.perfectapp.AppContainer
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Runs once daily. Checks renewals due soon, vehicle maintenance due soon, and today's
 * next calendar event, and posts at most one notification per category — never a flood
 * of individual notifications.
 */
class DailySummaryWorker(
    context: Context,
    params: WorkerParameters,
    private val container: AppContainer
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = container.settingsRepository.settings.first()
            if (!settings.notificationsEnabled) return Result.success()
            val notificationHelper = NotificationHelper(applicationContext)
            val today = LocalDate.now()

            // Auto-charge any subscriptions whose billing date has arrived, then notify
            // about ones renewing in the next few days.
            container.wealthRepository.processDueSubscriptions()
            if (settings.subscriptionNotifications) checkSubscriptionsRenewingSoon(notificationHelper, today)
            if (settings.renewalNotifications) checkRenewals(notificationHelper, today)
            if (settings.carNotifications) checkCarMaintenance(notificationHelper)
            if (settings.calendarNotifications) checkCalendarToday(notificationHelper, today)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** Posts one notification per subscription renewing within 3 days — repeated daily
     *  until it renews, e.g. "Netflix will renew subscription on Sep 12, 2026". Each
     *  subscription gets its own notification ID so multiple upcoming renewals don't
     *  overwrite each other, and the same subscription's notification just updates in
     *  place on each subsequent day rather than stacking. */
    private suspend fun checkSubscriptionsRenewingSoon(helper: NotificationHelper, today: LocalDate) {
        val subscriptions = container.wealthRepository.activeSubscriptions.first()
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

        for (subscription in subscriptions) {
            if (!subscription.autoRenew) continue
            val daysUntil = ChronoUnit.DAYS.between(today, subscription.nextChargeDate)
            if (daysUntil in 0..3) {
                helper.showNotification(
                    id = NotificationHelper.subscriptionNotificationId(subscription.id),
                    title = "Upcoming subscription renewal",
                    text = "${subscription.name} will renew subscription on " +
                        subscription.nextChargeDate.format(dateFormatter)
                )
            }
        }
    }

    private suspend fun checkRenewals(helper: NotificationHelper, today: LocalDate) {
        val dueSoon = container.reminderRepository.upcoming.first().filter {
            ChronoUnit.DAYS.between(today, it.dueDate) in 0..3
        }
        if (dueSoon.isNotEmpty()) {
            helper.showNotification(
                id = NotificationHelper.NOTIFICATION_ID_RENEWALS,
                title = if (dueSoon.size == 1) "Renewal due soon" else "${dueSoon.size} renewals due soon",
                text = dueSoon.joinToString(", ") { it.title }
            )
        }
    }

    private suspend fun checkCarMaintenance(helper: NotificationHelper) {
        val car = container.carRepository.primaryCar.first() ?: return
        val remaining = container.carRepository.kmRemaining(car) ?: return
        if (remaining <= 300) {
            helper.showNotification(
                id = NotificationHelper.NOTIFICATION_ID_CAR,
                title = "Vehicle service due soon",
                text = "${car.name} has $remaining km left until the next service"
            )
        }
    }

    private suspend fun checkCalendarToday(helper: NotificationHelper, today: LocalDate) {
        val nextEvent = container.calendarRepository.nextEvent().first() ?: return
        if (nextEvent.dateTime.toLocalDate() == today) {
            helper.showNotification(
                id = NotificationHelper.NOTIFICATION_ID_CALENDAR,
                title = "Today: ${nextEvent.title}",
                text = nextEvent.dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
            )
        }
    }

    companion object {
        const val WORK_NAME = "daily_summary"
    }
}
