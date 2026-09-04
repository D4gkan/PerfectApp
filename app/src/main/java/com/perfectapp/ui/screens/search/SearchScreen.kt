package com.perfectapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.AppContainer
import com.perfectapp.ui.components.PremiumCard
import kotlinx.coroutines.flow.flowOf

/** Offline, in-memory search across the records people most often need to find. */
@Composable
fun SearchScreen(container: AppContainer) {
    var query by remember { mutableStateOf("") }
    val events by container.calendarRepository.allEvents.collectAsState(initial = emptyList())
    val reminders by container.reminderRepository.all.collectAsState(initial = emptyList())
    val transactions by container.wealthRepository.transactions.collectAsState(initial = emptyList())
    val measurements by container.healthRepository.allMeasurements.collectAsState(initial = emptyList())
    val meals by container.dietRepository.allMeals.collectAsState(initial = emptyList())
    val waterEntries by container.waterRepository.allEntries.collectAsState(initial = emptyList())
    val assets by container.wealthRepository.assets.collectAsState(initial = emptyList())
    val subscriptions by container.wealthRepository.activeSubscriptions.collectAsState(initial = emptyList())
    val car by container.carRepository.primaryCar.collectAsState(initial = null)
    val maintenance by (car?.let { container.carRepository.maintenanceForCar(it.id) } ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val fuel by (car?.let { container.carRepository.fuelForCar(it.id) } ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val key = query.trim().lowercase()
    val results = buildList {
        if (key.isNotEmpty()) {
            events.filter { it.title.lowercase().contains(key) || it.notes.orEmpty().lowercase().contains(key) }.forEach { add("Calendar · ${it.dateTime.toLocalDate()}" to it.title) }
            reminders.filter { it.title.lowercase().contains(key) }.forEach { add("Reminder · ${it.dueDate}" to it.title) }
            transactions.filter { it.category.lowercase().contains(key) || it.note.orEmpty().lowercase().contains(key) }.forEach { add("${it.type.name.lowercase()} · ${it.date}" to "${it.category} · ${it.amount} ${it.currencyCode}") }
            measurements.filter { it.measurementDate.toString().contains(key) || "weight".contains(key) }.forEach { add("Health · ${it.measurementDate}" to "Weight ${it.weightKg} kg") }
            meals.filter { it.name.lowercase().contains(key) || it.description.orEmpty().lowercase().contains(key) }.forEach { add("Meal · ${it.date}" to "${it.name} · ${it.calories} kcal") }
            waterEntries.filter { it.date.toString().contains(key) || "water hydration".contains(key) }.forEach { add("Water · ${it.date}" to "${it.amountMl} ml") }
            assets.filter { it.name.lowercase().contains(key) || it.type.name.lowercase().contains(key) }.forEach { add("Asset · ${it.type.name.lowercase()}" to "${it.name} · ${it.quantity} ${it.currencyCode}") }
            subscriptions.filter { it.name.lowercase().contains(key) }.forEach { add("Subscription · ${it.nextChargeDate}" to "${it.name} · ${it.amount} ${it.currencyCode}") }
            maintenance.filter { it.title.lowercase().contains(key) || it.notes.orEmpty().lowercase().contains(key) }.forEach { add("Car maintenance" to it.title) }
            fuel.filter { it.notes.orEmpty().lowercase().contains(key) || "fuel petrol gas".contains(key) }.forEach { add("Fuel · ${it.date}" to "${it.totalCost} ${it.currencyCode} · ${it.liters} L") }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { OutlinedTextField(query, { query = it }, label = { Text("Search all records") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            if (key.isNotEmpty() && results.isEmpty()) item { Text("No matching records") }
            results.forEach { (meta, title) -> item { PremiumCard(Modifier.fillMaxWidth()) { Column { Text(title); Text(meta) } } } }
        }
    }
}
