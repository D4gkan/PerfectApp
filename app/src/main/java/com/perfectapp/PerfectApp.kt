package com.perfectapp

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.perfectapp.domain.notifications.AppWorkerFactory
import com.perfectapp.domain.notifications.DailySummaryWorker
import com.perfectapp.domain.notifications.NotificationAlarmScheduler
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

        val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val changes = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
        com.perfectapp.data.PerfectDatabase.getInstance(this).invalidationTracker.addObserver(
            object : androidx.room.InvalidationTracker.Observer("assets", "transactions", "exchange_rates", "gold_settings", "subscriptions", "calendar_events", "reminders") {
                override fun onInvalidated(tables: Set<String>) { changes.trySend(Unit) }
            }
        )
        widgetScope.launch {
            for (change in changes) {
                kotlinx.coroutines.delay(300)
                com.perfectapp.ui.widgets.WidgetRefresh.request(this@PerfectApp)
            }
        }
        changes.trySend(Unit)

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
        // Remove schedules created by older versions, then use a wake-up alarm. WorkManager is
        // intentionally deferrable and can otherwise wait until opening the app wakes it up.
        WorkManager.getInstance(this).cancelUniqueWork(DailySummaryWorker.WORK_NAME)
        NotificationAlarmScheduler(this).scheduleDaily(hour, minute)
    }
}
