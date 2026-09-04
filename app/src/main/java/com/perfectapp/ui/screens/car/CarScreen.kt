package com.perfectapp.ui.screens.car

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
    onAddMaintenance: (carId: Long) -> Unit,
    onAddCarRenewal: (carId: Long) -> Unit
) {
    val viewModel: CarViewModel = viewModel(factory = viewModelFactory { CarViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val carRenewals by reminderRepository.all.collectAsState(initial = emptyList())
    val assets by wealthRepository.assets.collectAsState(initial = emptyList())
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
    if (showFuelDialog) FuelDialog(car.id, car.currentOdometerKm, assets, onDismiss = { showFuelDialog = false }) { entry, assetId -> viewModel.addFuel(entry, assetId); showFuelDialog = false }
    maintenanceToDelete?.let { item ->
        AlertDialog(onDismissRequest = { maintenanceToDelete = null }, title = { Text("Delete maintenance item?") }, text = { Text("${item.title} will be removed from this vehicle's schedule.") }, confirmButton = { Button(onClick = { viewModel.deleteMaintenance(item); maintenanceToDelete = null }) { Text("Delete") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { maintenanceToDelete = null }) { Text("Cancel") } })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(title = car.name, actionLabel = "Edit", onActionClick = onAddOrEditCar)
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
private fun FuelDialog(carId: Long, odometer: Long, assets: List<AssetEntity>, onDismiss: () -> Unit, onSave: (com.perfectapp.data.entities.FuelEntryEntity, Long?) -> Unit) {
    var liters by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("") }; var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }; var km by remember { mutableStateOf(odometer.toString()) }; var currency by remember { mutableStateOf("USD") }; var fundingAssetId by remember { mutableStateOf<Long?>(null) }
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss, title = { Text("Log fuel") }, text = { Column {
        androidx.compose.material3.OutlinedTextField(liters, { liters = it }, label = { Text("Liters") })
        androidx.compose.material3.OutlinedTextField(cost, { cost = it }, label = { Text("Total cost") })
        androidx.compose.material3.OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text("Currency") })
        if (assets.filter { it.currencyCode.equals(currency, true) }.isNotEmpty()) {
            Text("Paid from (optional)", style = MaterialTheme.typography.labelMedium)
            assets.filter { it.currencyCode.equals(currency, true) }.forEach { asset ->
                androidx.compose.material3.FilterChip(selected = fundingAssetId == asset.id, onClick = { fundingAssetId = if (fundingAssetId == asset.id) null else asset.id }, label = { Text(asset.name) })
            }
        }
        androidx.compose.material3.OutlinedTextField(date, { date = it }, label = { Text("Date (yyyy-MM-dd)") })
        androidx.compose.material3.OutlinedTextField(km, { km = it }, label = { Text("Odometer km") })
    } }, confirmButton = { androidx.compose.material3.Button(onClick = {
        val parsedDate = runCatching { java.time.LocalDate.parse(date) }.getOrNull(); val parsedLiters = liters.toDoubleOrNull(); val parsedCost = cost.toDoubleOrNull()
        if (parsedDate != null && parsedLiters != null && parsedLiters > 0 && parsedCost != null && parsedCost >= 0) onSave(com.perfectapp.data.entities.FuelEntryEntity(carId = carId, date = parsedDate, odometerKm = km.toLongOrNull(), liters = parsedLiters, totalCost = parsedCost, currencyCode = currency.ifBlank { "USD" }), fundingAssetId)
    }) { Text("Save") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } })
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
