package com.perfectapp.domain.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.perfectapp.R

/** Wraps notification channel setup and posting so workers and UI both go through one place. */
class NotificationHelper(private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_ID, "Daily Summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Renewals due soon, vehicle maintenance, and subscription updates"
                },
                NotificationChannel(CALENDAR_CHANNEL_ID, "Calendar reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders scheduled for individual calendar events"
                }
            ))
        }
    }

    /** Posts a notification if permission allows it. Silently no-ops otherwise — the app's
     *  in-app screens remain the source of truth regardless of notification permission. */
    fun showNotification(id: Int, title: String, text: String, channelId: String = CHANNEL_ID) {
        ensureChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "daily_summary"
        const val CALENDAR_CHANNEL_ID = "calendar_reminders"

        const val NOTIFICATION_ID_RENEWALS = 1001
        const val NOTIFICATION_ID_CAR = 1002
        const val NOTIFICATION_ID_CALENDAR = 1003
        const val NOTIFICATION_ID_TEST = 1099

        private const val SUBSCRIPTION_ID_BASE = 2000

        /** Deterministic, stable-per-subscription notification ID so the same subscription's
         *  "renewing soon" notification updates in place across days instead of stacking. */
        fun subscriptionNotificationId(subscriptionId: Long): Int =
            SUBSCRIPTION_ID_BASE + (subscriptionId % 100_000).toInt()

        fun eventNotificationId(eventId: Long): Int =
            102_000 + (eventId % 100_000).toInt()

        fun renewalNotificationId(reminderId: Long): Int =
            202_000 + (reminderId % 100_000).toInt()
    }
}
