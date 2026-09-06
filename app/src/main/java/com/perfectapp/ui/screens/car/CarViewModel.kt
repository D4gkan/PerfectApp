package com.perfectapp.ui.screens.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.entities.MaintenanceEntity
import com.perfectapp.data.repository.CarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class CarViewModel(private val repository: CarRepository) : ViewModel() {

    val uiState: StateFlow<CarUiState> = repository.primaryCar
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(CarUiState(isLoading = false, car = null))
            } else {
                combine(
                    repository.odometerHistory(car.id),
                    repository.maintenanceForCar(car.id),
                    repository.fuelForCar(car.id)
                ) { history, maintenance, fuel ->
                    val ordered = history.sortedBy { it.date }
                    val total = ordered.lastOrNull()?.odometerKm?.minus(ordered.firstOrNull()?.odometerKm ?: 0L)
                    val monthStart = LocalDate.now().withDayOfMonth(1)
                    val beforeMonth = ordered.filter { !it.date.isAfter(monthStart) }.maxByOrNull { it.date }
                    val latest = ordered.lastOrNull()
                    val thisMonth = if (beforeMonth != null && latest != null) latest.odometerKm - beforeMonth.odometerKm else null
                    val days = if (ordered.size >= 2) ChronoUnit.DAYS.between(ordered.first().date, ordered.last().date).coerceAtLeast(1) else 0
                    val fuelByOdometer = fuel.filter { it.odometerKm != null }.sortedBy { it.odometerKm }
                    // Full-tank assumption: each fill after the first represents fuel used since its predecessor.
                    val fuelDistance = if (fuelByOdometer.size >= 2) fuelByOdometer.last().odometerKm!! - fuelByOdometer.first().odometerKm!! else null
                    val fuelUsed = if (fuelByOdometer.size >= 2) fuelByOdometer.drop(1).sumOf { it.liters } else 0.0
                    val fuelCostUsed = if (fuelByOdometer.size >= 2) fuelByOdometer.drop(1).sumOf { it.totalCost } else 0.0
                    CarUiState(
                        isLoading = false,
                        car = car,
                        odometerHistory = history,
                        maintenanceItems = maintenance,
                        totalDrivenKm = total,
                        drivenThisMonthKm = thisMonth,
                        averageDailyKm = if (total != null && days > 0) total.toDouble() / days else null
                        ,fuelEntries = fuel
                        ,totalFuelCost = fuel.sumOf { it.totalCost }
                        ,averageFuelCost = fuel.takeIf { it.isNotEmpty() }?.let { it.sumOf { item -> item.totalCost } / it.size }
                        ,fuelEfficiencyLPer100Km = fuelDistance?.takeIf { it > 0 }?.let { fuelUsed / it * 100.0 }
                        ,fuelCostPerKm = fuelDistance?.takeIf { it > 0 }?.let { fuelCostUsed / it }
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarUiState()
        )

    fun saveCar(car: CarEntity) {
        viewModelScope.launch { repository.upsertCar(car) }
    }

    fun addOdometerReading(odometerKm: Long) {
        val car = uiState.value.car ?: return
        viewModelScope.launch { repository.addOdometerEntry(car, odometerKm) }
    }

    fun completeMaintenance(item: MaintenanceEntity) {
        viewModelScope.launch { repository.markMaintenanceComplete(item) }
    }

    fun deleteMaintenance(item: MaintenanceEntity) {
        viewModelScope.launch { repository.deleteMaintenance(item) }
    }
    suspend fun addFuel(entry: com.perfectapp.data.entities.FuelEntryEntity) {
        val car = uiState.value.car ?: error("Select a vehicle")
        repository.addFuel(entry, car.name)
    }
}
