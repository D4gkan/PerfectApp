package com.perfectapp

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.perfectapp.domain.notifications.AppWorkerFactory
import com.perfectapp.domain.notifications.DailySummaryWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PerfectApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Manual WorkManager init with a custom factory (see the manifest's provider override,
        // which disables WorkManager's own androidx-startup auto-init so this doesn't clash).
        val workConfig = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(container))
            .build()
        WorkManager.initialize(this, workConfig)

        rescheduleDailySummaryWork()
    }

    fun rescheduleDailySummaryWork() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val settings = container.settingsRepository.settings.first()
            scheduleDailySummaryWork(settings.notificationHour, settings.notificationMinute)
            container.calendarRepository.rescheduleReminders()
            container.reminderRepository.rescheduleReminders()
            container.wealthRepository.rescheduleSubscriptionReminders()
        }
    }

    fun scheduleDailySummaryWork(hour: Int = 8, minute: Int = 0) {
        val now = LocalDateTime.now()
        var nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, nextRun).toMinutes().coerceAtLeast(0)

        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
