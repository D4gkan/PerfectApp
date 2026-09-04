package com.perfectapp.ui.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes Glance widgets immediately after local data changes, rather than waiting for launcher updates. */
object WidgetRefresh {
    suspend fun request(context: Context) {
        val appContext = context.applicationContext
        NextEventWidget().updateAll(appContext)
        TodayScheduleWidget().updateAll(appContext)
        UpcomingRemindersWidget().updateAll(appContext)
        HomeDashboardWidget().updateAll(appContext)
    }
}
