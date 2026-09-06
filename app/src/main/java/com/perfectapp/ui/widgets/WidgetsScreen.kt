package com.perfectapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.perfectapp.ui.components.PremiumCard

@Composable
fun WidgetsScreen() {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    var message by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Your day, at a glance", style = MaterialTheme.typography.headlineMedium)
            Text("Add a widget to your phone's home screen. Resize it to see more.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { message?.let { Text(it, color = MaterialTheme.colorScheme.primary) } }
        listOf(
            Triple("Home dashboard", "Upcoming events, total net worth and recent transactions", HomeDashboardWidgetReceiver::class.java),
            Triple("Next event", "Your closest upcoming calendar event", NextEventWidgetReceiver::class.java),
            Triple("Today's schedule", "Today's events, including repeats", TodayScheduleWidgetReceiver::class.java),
            Triple("Reminders", "Renewals and subscriptions in due-date order", UpcomingRemindersWidgetReceiver::class.java)
        ).forEach { (title, description, receiver) ->
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
                            message = "Confirm the widget in your launcher's prompt."
                        } else message = "Long-press your home screen, choose Widgets, then Perfect App."
                    }, modifier = Modifier.padding(top = 12.dp)) { Text("Add widget") }
                }
            }
        }
    }
}
