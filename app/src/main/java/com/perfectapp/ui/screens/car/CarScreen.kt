package com.perfectapp.ui.screens.car

import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.MaintenanceEntity
import com.perfectapp.data.repository.CarRepository
import com.perfectapp.data.repository.ReminderRepository
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.EmptyState
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import java.time.format.DateTimeFormatter

@Composable
fun CarScreen(
    repository: CarRepository,
    wealthRepository: WealthRepository,
    reminderRepository: ReminderRepository,
    onAddOrEditCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    onAddMaintenance: (carId: Long) -> Unit,
    onAddCarRenewal: (carId: Long) -> Unit
) {
    val viewModel: CarViewModel = viewModel(factory = viewModelFactory { CarViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val carRenewals by reminderRepository.all.collectAsState(initial = emptyList())
    val cars by repository.cars.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var carMenu by remember { mutableStateOf(false) }
    var removeCar by remember { mutableStateOf(false) }
    var showOdometerDialog by remember { mutableStateOf(false) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var maintenanceToDelete by remember { mutableStateOf<MaintenanceEntity?>(null) }

    val car = state.car
    if (car == null) {
        EmptyState(
            title = "No vehicle yet",
            message = "Add your car to start tracking mileage and maintenance.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "Add Vehicle",
            onActionClick = onAddOrEditCar
        )
        return
    }

    if (showOdometerDialog) {
        LogOdometerDialog(
            currentOdometerKm = car.currentOdometerKm,
            onDismiss = { showOdometerDialog = false },
            onSave = {
                viewModel.addOdometerReading(it)
                showOdometerDialog = false
            }
        )
    }
    if (showFuelDialog) FuelDialog(car.id, car.currentOdometerKm, onDismiss = { showFuelDialog = false }) { entry -> viewModel.addFuel(entry); showFuelDialog = false }
    if (removeCar) AlertDialog(onDismissRequest = { removeCar = false }, title = { Text("Remove ${car.name}?") },
        text = { Text("Removes this vehicle, fuel log, mileage and maintenance. Wealth transactions and renewal reminders are kept.") },
        confirmButton = { Button(onClick = { scope.launch { repository.deleteCar(car); removeCar = false } }) { Text("Remove") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { removeCar = false }) { Text("Cancel") } })
    maintenanceToDelete?.let { item ->
        AlertDialog(onDismissRequest = { maintenanceToDelete = null }, title = { Text("Delete maintenance item?") }, text = { Text("${item.title} will be removed from this vehicle's schedule.") }, confirmButton = { Button(onClick = { viewModel.deleteMaintenance(item); maintenanceToDelete = null }) { Text("Delete") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { maintenanceToDelete = null }) { Text("Cancel") } })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box {
                androidx.compose.material3.OutlinedButton(onClick = { carMenu = true }) { Text("${car.name}  -  Vehicles") }
                androidx.compose.material3.DropdownMenu(carMenu, onDismissRequest = { carMenu = false }) {
                    cars.forEach { vehicle -> androidx.compose.material3.DropdownMenuItem(text = { Text(vehicle.name) }, onClick = { repository.selectCar(vehicle.id); carMenu = false }) }
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Add vehicle") }, onClick = { carMenu = false; onAddOrEditCar() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Edit current vehicle") }, onClick = { carMenu = false; onEditCar(car.id) })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Remove current vehicle") }, onClick = { carMenu = false; removeCar = true })
                }
            }
        }
        item { SectionHeader(title = "Fuel spending", actionLabel = "Add fuel", onActionClick = { showFuelDialog = true }) }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) { Column {
                MetricRow("Total logged fuel cost", "%.2f".format(state.totalFuelCost))
                state.averageFuelCost?.let { MetricRow("Average fill-up", "%.2f".format(it)) }
                MetricRow("Fuel entries", state.fuelEntries.size.toString())
                MetricRow("Fuel efficiency", state.fuelEfficiencyLPer100Km?.let { "%.1f L/100 km".format(it) } ?: "Add two odometer-tagged fill-ups")
                MetricRow("Fuel cost per km", state.fuelCostPerKm?.let { "%.3f per km".format(it) } ?: "Add two odometer-tagged fill-ups")
                Text("Efficiency assumes each logged fill-up is a full tank.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        items(state.fuelEntries.take(5)) { fuel ->
            PremiumCard(Modifier.fillMaxWidth()) { Column {
                Text("${fuel.liters} L · ${fuel.totalCost} ${fuel.currencyCode}", style = MaterialTheme.typography.titleMedium)
                Text("${fuel.date}${fuel.odometerKm?.let { " · $it km" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        item { SectionHeader(title = "Car renewals", actionLabel = "Add", onActionClick = { onAddCarRenewal(car.id) }) }
        val linkedRenewals = carRenewals.filter { it.carId == car.id && !it.isCompleted }
        if (linkedRenewals.isEmpty()) item { Text("No car-specific renewals yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(linkedRenewals) { renewal -> PremiumCard(Modifier.fillMaxWidth()) { Column { Text(renewal.title, style = MaterialTheme.typography.titleMedium); Text("Due ${renewal.dueDate}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    MetricRow(label = "Current Odometer", value = "${car.currentOdometerKm} km")
                    car.nextMaintenanceKm?.let {
                        MetricRow(label = "Next Service At", value = "$it km")
                        val remaining = repository.kmRemaining(car)
                        remaining?.let { r -> MetricRow(label = "Remaining", value = "$r km") }
                    }
                }
            }
        }
        item { SectionHeader(title = "Driving statistics") }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    MetricRow(label = "Total logged distance", value = state.totalDrivenKm?.let { "$it km" } ?: "Add two readings to calculate")
                    state.drivenThisMonthKm?.let { MetricRow(label = "This month", value = "$it km") }
                    state.averageDailyKm?.let { MetricRow(label = "Average daily", value = "%.1f km".format(it)) }
                }
            }
        }
        item {
            androidx.compose.material3.OutlinedButton(
                onClick = { showOdometerDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Odometer Reading")
            }
        }

        item {
            SectionHeader(title = "Maintenance Schedule", actionLabel = "Add", onActionClick = { onAddMaintenance(car.id) })
        }
        if (state.maintenanceItems.isEmpty()) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No maintenance items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(state.maintenanceItems) { item ->
            MaintenanceRow(
                item = item,
                currentOdometerKm = car.currentOdometerKm,
                onComplete = { viewModel.completeMaintenance(item) },
                onDelete = { maintenanceToDelete = item }
            )
        }
    }
}

@Composable
private fun FuelDialog(carId: Long, odometer: Long, onDismiss: () -> Unit, onSave: suspend (com.perfectapp.data.entities.FuelEntryEntity) -> Unit) {
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var km by remember { mutableStateOf(odometer.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Log fuel") }, text = { Column {
        Text("Paid from: TRY CASH", color = MaterialTheme.colorScheme.primary)
        Text("Saving also records an expense in Wealth.", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.OutlinedTextField(liters, { liters = it }, label = { Text("Liters") })
        androidx.compose.material3.OutlinedTextField(cost, { cost = it }, label = { Text("Total paid (TRY)") })
        com.perfectapp.ui.components.DatePickerField("Date", date, { date = it })
        androidx.compose.material3.OutlinedTextField(km, { km = it }, label = { Text("Odometer km") })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(enabled = !saving, onClick = {
        val parsedLiters = liters.replace(',', '.').toDoubleOrNull()
        val parsedCost = cost.replace(',', '.').toDoubleOrNull()
        val parsedKm = km.toLongOrNull()
        if (parsedLiters == null || !parsedLiters.isFinite() || parsedLiters <= 0 || parsedCost == null || !parsedCost.isFinite() || parsedCost <= 0 || parsedKm == null || parsedKm < 0) {
            error = "Enter positive liters and cost, and a valid odometer."
        } else {
            saving = true
            scope.launch {
                try { onSave(com.perfectapp.data.entities.FuelEntryEntity(carId = carId, date = date, odometerKm = parsedKm, liters = parsedLiters, totalCost = parsedCost, currencyCode = "TRY")) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; error = e.message ?: "Could not save fuel" }
                finally { saving = false }
            }
        }
    }) { Text(if (saving) "Saving..." else "Save") } }, dismissButton = { androidx.compose.material3.TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun MaintenanceRow(
    item: MaintenanceEntity,
    currentOdometerKm: Long,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                item.dueAtOdometerKm?.let {
                    val remaining = (it - currentOdometerKm).coerceAtLeast(0)
                    Text(
                        text = if (item.isCompleted) "Completed" else "$remaining km remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.dueDate?.let {
                    Text(
                        text = "Due " + it.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                if (!item.isCompleted) {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark complete")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete")
                }
            }
        }
    }
}
