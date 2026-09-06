package com.perfectapp.ui.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Refreshes Glance widgets immediately after local data changes, rather than waiting for launcher updates. */
object WidgetRefresh {
    suspend fun request(context: Context) {
        val appContext = context.applicationContext
        listOf(HomeDashboardWidget()).forEach { widget ->
            try { widget.updateAll(appContext) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { android.util.Log.e("WidgetRefresh", "Widget refresh failed", error) }
        }
    }
}
