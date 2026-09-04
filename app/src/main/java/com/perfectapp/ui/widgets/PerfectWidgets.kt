package com.perfectapp.ui.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.text.Text
import com.perfectapp.PerfectApp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class PerfectWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }
    @androidx.compose.runtime.Composable abstract fun Content(context: Context)
}

class NextEventWidget : PerfectWidget() {
    @androidx.compose.runtime.Composable override fun Content(context: Context) {
        val event = androidx.compose.runtime.produceState<com.perfectapp.data.entities.CalendarEventEntity?>(null) {
            value = (context.applicationContext as PerfectApp).container.calendarRepository.nextEvent().first()
        }.value
        Column { Text("NEXT EVENT"); Text(event?.title ?: "No upcoming events"); event?.let { Text(it.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm"))) } }
    }
}
class NextEventWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = NextEventWidget() }

class TodayScheduleWidget : PerfectWidget() {
    @androidx.compose.runtime.Composable override fun Content(context: Context) {
        val events = androidx.compose.runtime.produceState(emptyList<com.perfectapp.data.entities.CalendarEventEntity>()) {
            value = (context.applicationContext as PerfectApp).container.calendarRepository.allEvents.first().filter { it.dateTime.toLocalDate() == LocalDate.now() }
        }.value
        Column { Text("TODAY'S SCHEDULE"); if (events.isEmpty()) Text("Nothing scheduled") else events.take(4).forEach { Text("${it.dateTime.toLocalTime()}  ${it.title}") } }
    }
}
class TodayScheduleWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = TodayScheduleWidget() }

class UpcomingRemindersWidget : PerfectWidget() {
    @androidx.compose.runtime.Composable override fun Content(context: Context) {
        val reminders = androidx.compose.runtime.produceState(emptyList<String>()) {
            val app = context.applicationContext as PerfectApp
            val renewals = app.container.reminderRepository.upcoming.first().map { it.title }
            val subscriptions = app.container.wealthRepository.activeSubscriptions.first().filter { it.autoRenew }.map { "${it.name} (subscription)" }
            value = (renewals + subscriptions).take(4)
        }.value
        Column { Text("UPCOMING REMINDERS"); if (reminders.isEmpty()) Text("No upcoming reminders") else reminders.forEach { Text(it) } }
    }
}
class UpcomingRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = UpcomingRemindersWidget() }

/** Compact launcher widget for the most useful at-a-glance health and hydration data. */
class HomeDashboardWidget : PerfectWidget() {
    @androidx.compose.runtime.Composable override fun Content(context: Context) {
        data class Snapshot(val weight: String, val water: String)
        val snapshot = androidx.compose.runtime.produceState(Snapshot("No measurement", "0 ml")) {
            val app = context.applicationContext as PerfectApp
            val latest = app.container.healthRepository.latestMeasurement.first()
            val water = app.container.waterRepository.totalForDate(LocalDate.now()).first()
            val goal = app.container.dietRepository.goal.first()?.waterGoalMl ?: 2500
            value = Snapshot(latest?.let { "${it.weightKg} kg" } ?: "No measurement", "$water / $goal ml")
        }.value
        Column {
            Text("PERFECT APP")
            Text("Weight  ${snapshot.weight}")
            Text("Water  ${snapshot.water}")
        }
    }
}
class HomeDashboardWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = HomeDashboardWidget() }
