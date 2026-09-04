package com.perfectapp.domain.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.perfectapp.PerfectApp

/** Restores the daily reminder schedule after Android clears scheduled work on reboot. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            (context.applicationContext as? PerfectApp)?.rescheduleDailySummaryWork()
        }
    }
}
