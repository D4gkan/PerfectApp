package com.perfectapp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.repository.AppSettings
import com.perfectapp.data.repository.SettingsRepository
import com.perfectapp.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(repository: SettingsRepository, onSaved: () -> Unit) {
    val stored by repository.settings.collectAsState(initial = AppSettings())
    var currency by remember(stored) { mutableStateOf(stored.displayCurrency) }
    var glass by remember(stored) { mutableStateOf(stored.glassMl.toString()) }
    var small by remember(stored) { mutableStateOf(stored.smallBottleMl.toString()) }
    var big by remember(stored) { mutableStateOf(stored.bigBottleMl.toString()) }
    var notificationsEnabled by remember(stored) { mutableStateOf(stored.notificationsEnabled) }
    var calendarNotifications by remember(stored) { mutableStateOf(stored.calendarNotifications) }
    var renewalNotifications by remember(stored) { mutableStateOf(stored.renewalNotifications) }
    var subscriptionNotifications by remember(stored) { mutableStateOf(stored.subscriptionNotifications) }
    var carNotifications by remember(stored) { mutableStateOf(stored.carNotifications) }
    var notificationHour by remember(stored) { mutableStateOf(stored.notificationHour.toString()) }
    var notificationMinute by remember(stored) { mutableStateOf(stored.notificationMinute.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(title = "Display currency") }
            item {
                OutlinedTextField(
                    value = currency, onValueChange = { currency = it.uppercase().take(3) },
                    label = { Text("USD, TRY, EUR, or GBP") }, modifier = Modifier.fillMaxWidth()
                )
            }
            item { SectionHeader(title = "Water quick-add amounts") }
            listOf("Glass (ml)" to glass, "Small bottle (ml)" to small, "Big bottle (ml)" to big)
                .forEachIndexed { index, (label, value) ->
                    item {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { new -> when (index) { 0 -> glass = new; 1 -> small = new; else -> big = new } },
                            label = { Text(label) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            error?.let { message -> item { Text(message) } }
            item { SectionHeader(title = "Notifications") }
            item { androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Daily alerts"); Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it }) } }
            if (notificationsEnabled) {
                item { androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Calendar"); Switch(calendarNotifications, { calendarNotifications = it }) } }
                item { androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Renewals"); Switch(renewalNotifications, { renewalNotifications = it }) } }
                item { androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subscriptions"); Switch(subscriptionNotifications, { subscriptionNotifications = it }) } }
                item { androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Vehicle maintenance"); Switch(carNotifications, { carNotifications = it }) } }
                item { OutlinedTextField(notificationHour, { notificationHour = it }, label = { Text("Daily alert hour (0–23)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(notificationMinute, { notificationMinute = it }, label = { Text("Daily alert minute (0–59)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
            }
            item {
                Button(onClick = {
                    val next = AppSettings(displayCurrency = currency, glassMl = glass.toIntOrNull() ?: 0, smallBottleMl = small.toIntOrNull() ?: 0, bigBottleMl = big.toIntOrNull() ?: 0, onboardingComplete = stored.onboardingComplete, notificationsEnabled = notificationsEnabled, calendarNotifications = calendarNotifications, renewalNotifications = renewalNotifications, subscriptionNotifications = subscriptionNotifications, carNotifications = carNotifications, notificationHour = notificationHour.toIntOrNull() ?: -1, notificationMinute = notificationMinute.toIntOrNull() ?: -1)
                    if (next.displayCurrency !in setOf("USD", "TRY", "EUR", "GBP") || next.glassMl <= 0 || next.smallBottleMl <= 0 || next.bigBottleMl <= 0 || next.notificationHour !in 0..23 || next.notificationMinute !in 0..59) {
                        error = "Enter a supported currency and positive amounts."
                    } else scope.launch { repository.save(next); (context.applicationContext as com.perfectapp.PerfectApp).scheduleDailySummaryWork(next.notificationHour, next.notificationMinute); onSaved() }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
            }
        }
    }
}
