package com.perfectapp.ui.screens.health

import com.perfectapp.data.entities.BodyMeasurementEntity

data class HealthUiState(
    val isLoading: Boolean = true,
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val latest: BodyMeasurementEntity? = null,
    val first: BodyMeasurementEntity? = null,
    val weightChangeKg: Double? = null,
    val fatMassChangeKg: Double? = null,
    val muscleGainedKg: Double? = null,
    val monthlyWeightChangeKg: Double? = null,
    val monthlyFatChangeKg: Double? = null,
    val monthlyMuscleChangeKg: Double? = null,
    val averageMonthlyWeightChangeKg: Double? = null
)
