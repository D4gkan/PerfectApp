package com.perfectapp.ui.widgets

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.perfectapp.AppContainer
import com.perfectapp.MainActivity
import com.perfectapp.PerfectApp
import com.perfectapp.domain.calendar.CalendarOccurrences
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WidgetSection(val title: String, val lines: List<String>)

abstract class PerfectWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    abstract fun sections(container: AppContainer): Flow<List<WidgetSection>>
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = sections((context.applicationContext as PerfectApp).container)
        val initial = source.first()
        provideContent {
            val sections by source.collectAsState(initial)
            val compact = LocalSize.current.height < 320.dp
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF132A2B))
                .clickable(actionStartActivity<MainActivity>()).padding(16.dp)) {
                sections.forEach { section ->
                    Text(section.title, style = TextStyle(color = ColorProvider(Color(0xFF79DDC6)),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.padding(top = 6.dp, bottom = 4.dp), maxLines = 1)
                    section.lines.take(if (compact && sections.size > 1) 1 else 3).forEach { line ->
                        Text(line, style = TextStyle(color = ColorProvider(Color(0xFFF1F8F5)), fontSize = 14.sp),
                            modifier = GlanceModifier.padding(bottom = 4.dp), maxLines = 2)
                    }
                }
            }
        }
    }
}

private val eventFormat = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm")
private fun upcoming(container: AppContainer) = container.calendarRepository.allEvents.map { events ->
    CalendarOccurrences.upcoming(events, LocalDateTime.now(), 3).map {
        "${it.event.title} · ${it.occurrenceDateTime.format(eventFormat)}"
    }
}
class NextEventWidget : PerfectWidget() {
    override fun sections(container: AppContainer) = upcoming(container).map {
        listOf(WidgetSection("NEXT EVENT", it.take(1).ifEmpty { listOf("No upcoming events") }))
    }
}
class NextEventWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = NextEventWidget() }

class TodayScheduleWidget : PerfectWidget() {
    override fun sections(container: AppContainer) = container.calendarRepository.allEvents.map { events ->
        val today = LocalDate.now()
        val lines = CalendarOccurrences.expand(events.filter { !it.isCompleted }, today.atStartOfDay(), today.atTime(23, 59, 59)).map {
            "${it.occurrenceDateTime.toLocalTime()} · ${it.event.title}"
        }
        listOf(WidgetSection("TODAY'S SCHEDULE", lines.ifEmpty { listOf("Nothing scheduled today") }))
    }
}
class TodayScheduleWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = TodayScheduleWidget() }

class UpcomingRemindersWidget : PerfectWidget() {
    override fun sections(container: AppContainer) = combine(container.reminderRepository.upcoming,
        container.wealthRepository.activeSubscriptions) { renewals, subscriptions ->
        val lines = (renewals.filter { !it.isCompleted }.map { it.dueDate to it.title } +
            subscriptions.filter { it.autoRenew }.map { it.nextChargeDate to it.name })
            .sortedBy { it.first }.take(3).map { "${it.second} · ${it.first}" }
        listOf(WidgetSection("UPCOMING REMINDERS", lines.ifEmpty { listOf("You're all caught up") }))
    }
}
class UpcomingRemindersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = UpcomingRemindersWidget() }

class HomeDashboardWidget : PerfectWidget() {
    override fun sections(container: AppContainer): Flow<List<WidgetSection>> {
        val wealth = container.wealthRepository
        val worth = combine(wealth.assets, wealth.exchangeRates, wealth.goldSettings) { assets, rates, gold ->
            val exchange = rates.associate { it.currencyCode to it.rateToBase }
            wealth.calculateNetWorth(assets, exchange, "USD") + (gold?.let { wealth.goldValueUsd(it, exchange) } ?: 0.0)
        }
        return combine(upcoming(container), worth, wealth.transactions) { events, total, transactions ->
            listOf(
                WidgetSection("UP NEXT", events.ifEmpty { listOf("No upcoming events") }),
                WidgetSection("TOTAL NET WORTH", listOf("USD %,.2f".format(total))),
                WidgetSection("RECENT TRANSACTIONS", transactions.sortedWith(compareByDescending<com.perfectapp.data.entities.TransactionEntity> { it.date }.thenByDescending { it.time })
                    .take(3).map { "${it.category} · ${if (it.type == com.perfectapp.data.entities.TransactionType.INCOME) "+" else "−"}${it.currencyCode} %,.2f".format(it.amount) }
                    .ifEmpty { listOf("No transactions yet") })
            )
        }
    }
}
class HomeDashboardWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = HomeDashboardWidget() }
