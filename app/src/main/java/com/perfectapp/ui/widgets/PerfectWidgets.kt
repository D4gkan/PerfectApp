package com.perfectapp.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.perfectapp.AppContainer
import com.perfectapp.MainActivity
import com.perfectapp.PerfectApp
import com.perfectapp.data.entities.TransactionType
import com.perfectapp.domain.calendar.CalendarOccurrences
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal val widgetBackground = ColorProvider(com.perfectapp.R.color.widgetbackground)
internal val widgetText = ColorProvider(com.perfectapp.R.color.widgettext)
internal val widgetMuted = ColorProvider(com.perfectapp.R.color.widgetmuted)
internal val widgetAccent = ColorProvider(com.perfectapp.R.color.widgetaccent)
internal val widgetTrack = ColorProvider(com.perfectapp.R.color.widgettrack)
internal fun openWidgetRoute(context: Context, route: String) = actionStartActivity(
    Intent(context, MainActivity::class.java).putExtra("widget_route", route)
        .setAction("com.perfectapp.widget.$route").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))

internal data class TodayItem(val title: String, val time: LocalDateTime, val route: String, val allDay: Boolean = false)
internal data class TodayData(val items: List<TodayItem> = emptyList(), val water: Int = 0, val waterGoal: Int = 2500,
    val calories: Int = 0, val calorieGoal: Int = 2000, val spending: String = "No spending today", val worth: String = "", val payment: String = "No upcoming payments")

internal fun todayData(c: AppContainer): Flow<TodayData> {
    // A new collection on each widget refresh picks up the current local date.
    val today = LocalDate.now()
    val agenda = combine(c.calendarRepository.allEvents, c.reminderRepository.upcoming, c.wealthRepository.activeSubscriptions) { events, reminders, subscriptions ->
        val now = LocalDateTime.now()
        val calendar = CalendarOccurrences.upcoming(events, today.atStartOfDay(), 30)
            .filter { it.event.isAllDay || !it.occurrenceDateTime.isBefore(now) }
            .map { TodayItem(it.event.title, it.occurrenceDateTime, "calendar/edit/${it.event.id}", it.event.isAllDay) }
        val renewals = reminders.filter { !it.isCompleted }.map { TodayItem(it.title, it.dueDate.atStartOfDay(), "renewals", true) }
        val payments = subscriptions.map { TodayItem(it.name, it.nextChargeDate.atStartOfDay(), "wealth", true) }
        (calendar + renewals + payments).sortedBy { it.time }
    }
    val progress = combine(c.waterRepository.totalForDate(today), c.dietRepository.mealsForDate(today), c.dietRepository.goal) { water, meals, goal ->
        TodayData(water = water, waterGoal = goal?.waterGoalMl ?: 2500, calories = meals.sumOf { it.calories }, calorieGoal = goal?.calorieGoal ?: 2000)
    }
    val money = combine(c.wealthRepository.transactions, c.wealthRepository.assets, c.wealthRepository.exchangeRates, c.wealthRepository.goldSettings) { transactions, assets, rates, gold ->
        val spending = transactions.filter { it.date == today && it.type == TransactionType.EXPENSE }.groupBy { it.currencyCode }
            .entries.joinToString(" · ") { (currency, rows) -> "$currency %,.2f".format(rows.sumOf { it.amount }) }
        val exchange = rates.associate { it.currencyCode to it.rateToBase }
        val worth = c.wealthRepository.calculateNetWorth(assets, exchange, "USD") + (gold?.let { c.wealthRepository.goldValueUsd(it, exchange) } ?: 0.0)
        spending.ifEmpty { "No spending today" } to "USD %,.2f".format(worth)
    }
    return combine(agenda, progress, money, c.wealthRepository.activeSubscriptions) { items, progressData, moneyData, subscriptions ->
        progressData.copy(items = items, spending = moneyData.first, worth = moneyData.second,
            payment = subscriptions.minByOrNull { it.nextChargeDate }?.let { "${it.name} · ${it.nextChargeDate.format(DateTimeFormatter.ofPattern("MMM d"))}" } ?: "No upcoming payments")
    }
}

class HomeDashboardWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = todayData((context.applicationContext as PerfectApp).container)
        val initial = source.first()
        provideContent {
            val data by source.collectAsState(initial)
            TodayContent(context, data, WidgetOptions.read(context))
        }
    }
}

@Composable
private fun TodayContent(context: Context, data: TodayData, options: WidgetOptions) {
    val height = LocalSize.current.height
    val expanded = height >= 340.dp
    val roomy = height >= 460.dp
    Column(GlanceModifier.fillMaxSize().background(widgetBackground).cornerRadius(24.dp).padding(16.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Today", GlanceModifier.defaultWeight().clickable(openWidgetRoute(context, "home")), style = TextStyle(color = widgetText, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")), style = TextStyle(color = widgetMuted, fontSize = 12.sp))
            Text("  ⚙", GlanceModifier.width(40.dp).height(40.dp).clickable(openWidgetRoute(context, "widgets")), style = TextStyle(color = widgetAccent, fontSize = 20.sp))
        }
        val first = data.items.firstOrNull()
        Label(if (first != null && first.time.toLocalDate().isBefore(LocalDate.now())) "NEEDS ATTENTION" else "UP NEXT")
        Text(first?.title ?: "Your day is clear", GlanceModifier.fillMaxWidth().clickable(openWidgetRoute(context, first?.route ?: "calendar")),
            style = TextStyle(color = widgetText, fontSize = 19.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Text(first?.let { timeLabel(it) } ?: "Nothing else scheduled today", style = TextStyle(color = widgetMuted, fontSize = 12.sp), maxLines = 1)
        if (expanded && options.timeline) {
            Spacer(GlanceModifier.height(12.dp))
            data.items.drop(1).take(if (roomy) 3 else 2).forEach { item ->
                Text("${timeLabel(item)}  ·  ${item.title}", GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(openWidgetRoute(context, item.route)), style = TextStyle(color = widgetText, fontSize = 12.sp), maxLines = 1)
            }
            Text("View calendar →", GlanceModifier.padding(vertical = 4.dp).clickable(openWidgetRoute(context, "calendar")), style = TextStyle(color = widgetAccent, fontSize = 12.sp))
        }
        if (expanded && options.progress) {
            Label("DAILY PROGRESS")
            Row(GlanceModifier.fillMaxWidth()) {
                Column(GlanceModifier.defaultWeight().padding(end = 10.dp).clickable(openWidgetRoute(context, "diet"))) {
                    Text("Water  ${data.water}/${data.waterGoal} ml", style = TextStyle(color = widgetMuted, fontSize = 11.sp), maxLines = 1)
                    LinearProgressIndicator((data.water.toFloat() / data.waterGoal.coerceAtLeast(1)).coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().padding(top = 5.dp), color = widgetAccent, backgroundColor = widgetTrack)
                }
                Column(GlanceModifier.defaultWeight().clickable(openWidgetRoute(context, "diet"))) {
                    Text("Calories  ${data.calories}/${data.calorieGoal}", style = TextStyle(color = widgetMuted, fontSize = 11.sp), maxLines = 1)
                    LinearProgressIndicator((data.calories.toFloat() / data.calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().padding(top = 5.dp), color = widgetAccent, backgroundColor = widgetTrack)
                }
            }
        }
        if (roomy && options.money) {
            Label("MONEY")
            Text(if (options.privacy) "Spending hidden" else "Today · ${data.spending}", GlanceModifier.clickable(openWidgetRoute(context, "wealth")), style = TextStyle(color = widgetText, fontSize = 12.sp), maxLines = 1)
            Text(data.payment, GlanceModifier.clickable(openWidgetRoute(context, "wealth")), style = TextStyle(color = widgetMuted, fontSize = 12.sp), maxLines = 1)
            if (options.netWorth) Text(if (options.privacy) "Net worth hidden" else "Net worth · ${data.worth}", style = TextStyle(color = widgetMuted, fontSize = 12.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (context.getSharedPreferences("today_widget", Context.MODE_PRIVATE).getLong("undo_water", 0) != 0L) {
            Text("Water added ? Undo", GlanceModifier.height(32.dp).clickable(actionRunCallback<WidgetUndoWaterAction>()), style = TextStyle(color = widgetAccent, fontSize = 12.sp))
        }
        Row(GlanceModifier.fillMaxWidth().padding(top = 10.dp)) {
            options.shortcuts.forEach { shortcut ->
                Text("+ $shortcut", GlanceModifier.defaultWeight().height(40.dp).background(widgetTrack).cornerRadius(12.dp).padding(10.dp)
                    .clickable(if (shortcut == "Water") actionRunCallback<WidgetWaterAction>() else openWidgetRoute(context, shortcutRoute(shortcut))),
                    style = TextStyle(color = widgetAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                Spacer(GlanceModifier.width(4.dp))
            }
        }
    }
}
@Composable private fun Label(text: String) {
    Text(text, GlanceModifier.padding(top = 10.dp, bottom = 4.dp), style = TextStyle(color = widgetAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold))
}
internal fun timeLabel(item: TodayItem, now: LocalDateTime = LocalDateTime.now()): String {
    val date = item.time.toLocalDate()
    if (date.isBefore(now.toLocalDate())) return "Overdue · ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
    if (date == now.toLocalDate()) {
        if (item.allDay) return "Today · All day"
        val minutes = java.time.Duration.between(now, item.time).toMinutes().coerceAtLeast(0)
        return item.time.format(DateTimeFormatter.ofPattern("HH:mm")) + if (minutes < 60) " · In $minutes min" else " · Today"
    }
    return item.time.format(DateTimeFormatter.ofPattern(if (item.allDay) "EEE, MMM d" else "EEE, MMM d · HH:mm"))
}
internal fun shortcutRoute(name: String) = when (name) { "Expense" -> "wealth/add-transaction"; "Meal" -> "diet/add"; else -> "calendar/add" }
class WidgetWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = (context.applicationContext as PerfectApp).container
        val prefs = context.getSharedPreferences("today_widget", Context.MODE_PRIVATE)
        val added = c.waterRepository.addWater(LocalDate.now(), c.settingsRepository.settings.first().glassMl)
        prefs.edit().putLong("undo_water", added).commit()
        WidgetRefresh.request(context)
    }
}
class WidgetUndoWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = (context.applicationContext as PerfectApp).container
        val prefs = context.getSharedPreferences("today_widget", Context.MODE_PRIVATE)
        val id = prefs.getLong("undo_water", 0)
        c.waterRepository.allEntries.first().firstOrNull { it.id == id }?.let { c.waterRepository.removeEntry(it) }
        prefs.edit().remove("undo_water").commit()
        WidgetRefresh.request(context)
    }
}
class HomeDashboardWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = HomeDashboardWidget() }
