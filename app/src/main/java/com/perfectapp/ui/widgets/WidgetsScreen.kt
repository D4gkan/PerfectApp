package com.perfectapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.perfectapp.ui.components.PremiumCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

internal data class WidgetOptions(val timeline: Boolean = true, val progress: Boolean = true, val money: Boolean = true,
    val netWorth: Boolean = false, val privacy: Boolean = true, val shortcuts: List<String> = listOf("Water", "Expense", "Event")) {
    fun save(context: Context) {
        context.getSharedPreferences("today_widget", Context.MODE_PRIVATE).edit()
            .putBoolean("timeline", timeline).putBoolean("progress", progress).putBoolean("money", money)
            .putBoolean("netWorth", netWorth).putBoolean("privacy", privacy).putString("shortcuts", shortcuts.joinToString(",")).apply()
    }
    companion object {
        fun observe(context: Context) = callbackFlow {
            val prefs = context.getSharedPreferences("today_widget", Context.MODE_PRIVATE)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read(context)) }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(read(context))
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        fun read(context: Context): WidgetOptions {
            val p = context.getSharedPreferences("today_widget", Context.MODE_PRIVATE)
            return WidgetOptions(p.getBoolean("timeline", true), p.getBoolean("progress", true), p.getBoolean("money", true),
                p.getBoolean("netWorth", false), p.getBoolean("privacy", true), p.getString("shortcuts", "Water,Expense,Event")!!.split(","))
        }
    }
}

@Composable
fun WidgetsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var options by remember { mutableStateOf(WidgetOptions.read(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    fun update(next: WidgetOptions) {
        options = next
        next.save(context)
        scope.launch { WidgetRefresh.request(context) }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Your day, together", style = MaterialTheme.typography.headlineMedium)
            Text("One Today widget for your plans, progress and payments.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Today", style = MaterialTheme.typography.headlineSmall)
                Text("Your next priority, at a glance", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("Tasks on the left, birthdays on the right. Enabled summaries appear at every size; scroll inside the widget for more.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(ComponentName(context, HomeDashboardWidgetReceiver::class.java), null, null)
                        message = "Confirm Today in your launcher's prompt."
                    } else message = "Long-press your home screen, choose Widgets, then Perfect App → Today."
                }, modifier = Modifier.padding(top = 12.dp)) { Text("Add Today widget") }
                message?.let { Text(it) }
            }
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Make it yours", style = MaterialTheme.typography.titleLarge)
                Text("Applies to all Today widgets. Financial amounts stay masked while privacy is enabled.", style = MaterialTheme.typography.bodySmall)
                Option("Timeline", options.timeline) { update(options.copy(timeline = it)) }
                Option("Water and calories", options.progress) { update(options.copy(progress = it)) }
                Option("Money summary", options.money) { update(options.copy(money = it)) }
                Option("Show net worth", options.netWorth) { update(options.copy(netWorth = it)) }
                Option("Show financial amounts", !options.privacy) { update(options.copy(privacy = !it)) }
            }
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Quick actions", style = MaterialTheme.typography.titleLarge)
                Text("Tap a slot to change it. Water logs your configured glass size; tap Undo water to remove that entry.", style = MaterialTheme.typography.bodySmall)
                options.shortcuts.forEachIndexed { index, name ->
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text("${index + 1}. $name") }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            listOf("Water", "Expense", "Event", "Meal").filter { it == name || it !in options.shortcuts }.forEach { choice ->
                                DropdownMenuItem(text = { Text(choice) }, onClick = {
                                    update(options.copy(shortcuts = options.shortcuts.toMutableList().also { it[index] = choice }))
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun Option(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = onChange)
    }
}
