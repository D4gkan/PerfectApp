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
import androidx.glance.appwidget.lazy.LazyColumn
import com.perfectapp.data.entities.CalendarItemType
import com.perfectapp.domain.calendar.Birthdays
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
internal data class TodayData(val university: List<TodayItem> = emptyList(), val recent: List<com.perfectapp.data.entities.TransactionEntity> = emptyList(), val protein: Double = 0.0, val carbs: Double = 0.0, val proteinGoal: Int = 0, val carbsGoal: Int = 0, val items: List<TodayItem> = emptyList(), val birthdays: List<TodayItem> = emptyList(), val water: Int = 0, val waterGoal: Int = 2500,
    val calories: Int = 0, val calorieGoal: Int = 2000, val spending: String = "No spending today", val worth: String = "", val payment: String = "No upcoming payments")

internal fun todayData(c: AppContainer): Flow<TodayData> {
    // A new collection on each widget refresh picks up the current local date.
    val today = LocalDate.now()
    val agenda = combine(c.calendarRepository.allEvents, c.reminderRepository.upcoming, c.wealthRepository.activeSubscriptions) { events, reminders, subscriptions ->
        val now = LocalDateTime.now()
        val calendar = CalendarOccurrences.upcoming(events.filter { it.itemType != CalendarItemType.BIRTHDAY && it.itemType != CalendarItemType.UNIVERSITY_LESSON }, today.atStartOfDay(), 30)
            .filter { it.event.isAllDay || !it.occurrenceDateTime.isBefore(now) }
            .map { TodayItem(it.event.title, it.occurrenceDateTime, "calendar/edit/${it.event.id}", it.event.isAllDay) }
        val renewals = reminders.filter { !it.isCompleted }.map { TodayItem(it.title, it.dueDate.atStartOfDay(), "renewals", true) }
        val payments = subscriptions.map { TodayItem(it.name, it.nextChargeDate.atStartOfDay(), "renewals", true) }
        Triple((calendar + renewals + payments).sortedBy { it.time }, Birthdays.upcoming(events, today).map {
            TodayItem(it.event.title, it.occurrenceDateTime, "calendar/edit/${it.event.id}", true)
        }, CalendarOccurrences.upcoming(events.filter { it.itemType == CalendarItemType.UNIVERSITY_LESSON }, today.atStartOfDay(), 30).filter { it.event.isAllDay || !it.occurrenceDateTime.isBefore(now) }.take(10).map {
            TodayItem(it.event.title, it.occurrenceDateTime, "calendar/edit/${it.event.id}", it.event.isAllDay)
        })
    }
    val progress = combine(c.waterRepository.totalForDate(today), c.dietRepository.mealsForDate(today), c.dietRepository.goal) { water, meals, goal ->
        TodayData(protein = meals.sumOf { it.proteinG }, carbs = meals.sumOf { it.carbsG }, proteinGoal = goal?.proteinGoalG ?: 0, carbsGoal = goal?.carbsGoalG ?: 0, water = water, waterGoal = goal?.waterGoalMl ?: 2500, calories = meals.sumOf { it.calories }, calorieGoal = goal?.calorieGoal ?: 2000)
    }
    val money = combine(c.wealthRepository.transactions, c.wealthRepository.assets, c.wealthRepository.exchangeRates, c.wealthRepository.goldSettings) { transactions, assets, rates, gold ->
        val spending = transactions.filter { it.date == today && it.type == TransactionType.EXPENSE }.groupBy { it.currencyCode }
            .entries.joinToString(" · ") { (currency, rows) -> "$currency %,.2f".format(rows.sumOf { it.amount }) }
        val exchange = rates.associate { it.currencyCode to it.rateToBase }
        val worth = c.wealthRepository.calculateNetWorth(assets, exchange, "USD") + (gold?.let { c.wealthRepository.goldValueUsd(it, exchange) } ?: 0.0)
        Triple(spending.ifEmpty { "No spending today" }, "USD %,.2f".format(worth), transactions.sortedWith(compareByDescending<com.perfectapp.data.entities.TransactionEntity> { it.date }.thenByDescending { it.time }).take(3))
    }
    return combine(agenda, progress, money, c.wealthRepository.activeSubscriptions, c.reminderRepository.all) { items, progressData, moneyData, subscriptions, reminders ->
        progressData.copy(items = items.first, birthdays = items.second, university = items.third, recent = moneyData.third, spending = moneyData.first, worth = moneyData.second,
            payment = (subscriptions.filter { it.autoRenew }.map { it.nextChargeDate to it.name } + reminders.filter { !it.isCompleted && it.amount != null }.map { it.dueDate to it.title })
                .minByOrNull { it.first }?.let { "${it.second} - ${it.first.format(DateTimeFormatter.ofPattern("MMM d"))}" } ?: "No upcoming payments")
    }
}

class HomeDashboardWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = todayData((context.applicationContext as PerfectApp).container)
        val initial = source.first()
        provideContent {
            val data by source.collectAsState(initial)
            val options by WidgetOptions.observe(context).collectAsState(WidgetOptions.read(context))
            TodayContent(context, data, options)
        }
    }
}

@Composable
private fun TodayContent(context: Context, data: TodayData, options: WidgetOptions) {
    val tall = LocalSize.current.height >= 340.dp
    val shown = if (tall && options.timeline) 3 else 1
    Column(GlanceModifier.fillMaxSize().background(widgetBackground).cornerRadius(22.dp).padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Today", GlanceModifier.defaultWeight().clickable(openWidgetRoute(context, "home")), style = TextStyle(color = widgetText, fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")), style = TextStyle(color = widgetMuted, fontSize = 11.sp))
            Text("Settings", GlanceModifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp).clickable(openWidgetRoute(context, "widgets")), style = TextStyle(color = widgetAccent, fontSize = 10.sp))
        }
        LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (options.money || options.netWorth) item {
                Row(GlanceModifier.fillMaxWidth().background(widgetTrack).cornerRadius(10.dp).padding(9.dp)) {
                    Column(GlanceModifier.defaultWeight().padding(end = 8.dp).clickable(openWidgetRoute(context, "wealth"))) {
                        if (options.netWorth) {
                            Label("NET WORTH")
                            Text(if (options.privacy) "Amount hidden" else data.worth, style = TextStyle(color = widgetText, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                        }
                        if (options.money) {
                            SmallText(if (options.privacy) "Spending hidden" else "Today: ${data.spending}", widgetText)
                            SmallText(data.payment, widgetMuted)
                        }
                    }
                    Column(GlanceModifier.defaultWeight().padding(start = 8.dp).clickable(openWidgetRoute(context, "wealth"))) {
                        Label("RECENT TRANSACTIONS")
                        if (data.recent.isEmpty()) SmallText("No transactions yet", widgetMuted)
                        data.recent.take(if (tall) 3 else 2).forEach { tx ->
                            val income = tx.type == TransactionType.INCOME
                            SmallText(tx.category, widgetMuted)
                            SmallText(if (options.privacy) "Amount hidden" else (if (income) "+" else "-") + "${tx.currencyCode} %,.2f".format(tx.amount),
                                ColorProvider(if (income) com.perfectapp.R.color.widgetincome else com.perfectapp.R.color.widgetexpense), bold = true)
                        }
                    }
                }
            }
            item {
                Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                    Column(GlanceModifier.defaultWeight().padding(end = 6.dp)) {
                        Label("TASKS & REMINDERS")
                        AgendaItems(context, data.items.take(shown), "Nothing scheduled")
                    }
                    Column(GlanceModifier.defaultWeight().padding(horizontal = 6.dp)) {
                        Label("UNIVERSITY")
                        AgendaItems(context, data.university.take(shown), "No lessons or exams")
                    }
                    Column(GlanceModifier.defaultWeight().padding(start = 6.dp)) {
                        Label("BIRTHDAYS")
                        if (data.birthdays.isEmpty()) Text("Add birthday", GlanceModifier.clickable(openWidgetRoute(context, "calendar/birthday")), style = TextStyle(color = widgetMuted, fontSize = 11.sp))
                        data.birthdays.take(shown).forEach { item ->
                            Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).clickable(openWidgetRoute(context, item.route))) {
                                SmallText(item.title, widgetText, bold = true)
                                SmallText(Birthdays.countdown(item.time.toLocalDate(), LocalDate.now()), widgetAccent)
                            }
                        }
                    }
                }
            }
            if (options.progress) item {
                Column(GlanceModifier.padding(top = 4.dp, bottom = 8.dp).clickable(openWidgetRoute(context, "diet"))) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        Column(GlanceModifier.defaultWeight().padding(end = 10.dp)) {
                            SmallText("Water ${data.water}/${data.waterGoal} ml", widgetMuted)
                            LinearProgressIndicator((data.water.toFloat() / data.waterGoal.coerceAtLeast(1)).coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().padding(top = 4.dp), color = widgetAccent, backgroundColor = widgetTrack)
                        }
                        Column(GlanceModifier.defaultWeight()) {
                            SmallText("Calories ${data.calories}/${data.calorieGoal}", widgetMuted)
                            LinearProgressIndicator((data.calories.toFloat() / data.calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().padding(top = 4.dp), color = widgetAccent, backgroundColor = widgetTrack)
                        }
                    }
                    if (tall) Row(GlanceModifier.fillMaxWidth().padding(top = 10.dp)) {
                        Column(GlanceModifier.defaultWeight()) {
                            Label("PROTEIN")
                            SmallText("%.0f / %d g".format(data.protein, data.proteinGoal), widgetText)
                        }
                        Column(GlanceModifier.defaultWeight()) {
                            Label("CARBS")
                            SmallText("%.0f / %d g".format(data.carbs, data.carbsGoal), widgetText)
                        }
                    }
                }
            }
        }
        if (context.getSharedPreferences("today_widget", Context.MODE_PRIVATE).getLong("undo_water", 0) != 0L) {
            Text("Water added - Undo", GlanceModifier.height(28.dp).clickable(actionRunCallback<WidgetUndoWaterAction>()), style = TextStyle(color = widgetAccent, fontSize = 11.sp))
        }
        Row(GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
            options.shortcuts.forEach { shortcut ->
                Box(GlanceModifier.defaultWeight().height(36.dp).background(widgetTrack).cornerRadius(10.dp)
                    .clickable(if (shortcut == "Water") actionRunCallback<WidgetWaterAction>() else openWidgetRoute(context, shortcutRoute(shortcut))), contentAlignment = Alignment.Center) {
                    Text("+ $shortcut", style = TextStyle(color = widgetAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
                Spacer(GlanceModifier.width(4.dp))
            }
        }
    }
}
@Composable private fun AgendaItems(context: Context, items: List<TodayItem>, empty: String) {
    if (items.isEmpty()) SmallText(empty, widgetMuted)
    items.forEach { item ->
        Column(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp).clickable(openWidgetRoute(context, item.route))) {
            SmallText(item.title, widgetText, bold = true)
            SmallText(timeLabel(item), widgetMuted)
        }
    }
}
@Composable private fun SmallText(text: String, color: ColorProvider, bold: Boolean = false) {
    Text(text, style = TextStyle(color = color, fontSize = 11.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal), maxLines = 2)
}
@Composable private fun Label(text: String) {
    Text(text, GlanceModifier.padding(top = 4.dp, bottom = 5.dp), style = TextStyle(color = widgetAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold), maxLines = 2)
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
