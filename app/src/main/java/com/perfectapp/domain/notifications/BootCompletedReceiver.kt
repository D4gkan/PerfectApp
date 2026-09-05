package com.perfectapp.domain.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.perfectapp.PerfectApp

/** Restores notification alarms after reboot and after wall-clock or time-zone changes. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            (context.applicationContext as? PerfectApp)?.rescheduleDailySummaryWork()
        }
    }
}
