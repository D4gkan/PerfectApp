package com.perfectapp.ui.screens.car

import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.entities.MaintenanceEntity
import com.perfectapp.data.entities.OdometerEntryEntity
import com.perfectapp.data.entities.FuelEntryEntity

data class CarUiState(
    val isLoading: Boolean = true,
    val car: CarEntity? = null,
    val odometerHistory: List<OdometerEntryEntity> = emptyList(),
    val maintenanceItems: List<MaintenanceEntity> = emptyList(),
    val totalDrivenKm: Long? = null,
    val drivenThisMonthKm: Long? = null,
    val averageDailyKm: Double? = null
    ,val fuelEntries: List<FuelEntryEntity> = emptyList()
    ,val totalFuelCost: Double = 0.0
    ,val averageFuelCost: Double? = null
    ,val fuelEfficiencyLPer100Km: Double? = null
    ,val fuelCostPerKm: Double? = null
)
