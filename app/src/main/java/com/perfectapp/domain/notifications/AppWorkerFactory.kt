package com.perfectapp.domain.notifications

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.perfectapp.AppContainer

/** Supplies AppContainer (and its repositories) to background workers, since there's no
 *  Hilt/DI framework here to do it automatically via the default reflection-based factory. */
class AppWorkerFactory(private val container: AppContainer) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DailySummaryWorker::class.java.name -> DailySummaryWorker(appContext, workerParameters, container)
            EventReminderWorker::class.java.name -> EventReminderWorker(appContext, workerParameters, container)
            DueDateReminderWorker::class.java.name -> DueDateReminderWorker(appContext, workerParameters, container)
            else -> null
        }
    }
}
