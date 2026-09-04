package com.perfectapp.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.repository.HealthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class HealthViewModel(private val repository: HealthRepository) : ViewModel() {

    val uiState: StateFlow<HealthUiState> = repository.allMeasurements
        .map { measurements ->
            val latest = measurements.firstOrNull() // observeAll is DESC ordered
            val first = measurements.lastOrNull()
            val latestMonth = latest?.measurementDate?.let(YearMonth::from)
            val previous = latestMonth?.let { month -> measurements.filter { YearMonth.from(it.measurementDate).isBefore(month) }.maxByOrNull { it.measurementDate } }
            val elapsedMonths = if (first != null && latest != null) ChronoUnit.MONTHS.between(YearMonth.from(first.measurementDate), YearMonth.from(latest.measurementDate)) else 0
            HealthUiState(
                isLoading = false,
                measurements = measurements,
                latest = latest,
                first = first,
                weightChangeKg = repository.weightChangeSince(first, latest),
                fatMassChangeKg = repository.fatMassChange(first, latest),
                muscleGainedKg = repository.muscleGained(first, latest),
                monthlyWeightChangeKg = repository.weightChangeSince(previous, latest),
                monthlyFatChangeKg = repository.fatMassChange(previous, latest),
                monthlyMuscleChangeKg = repository.muscleGained(previous, latest),
                averageMonthlyWeightChangeKg = if (elapsedMonths > 0) repository.weightChangeSince(first, latest)?.div(elapsedMonths) else null
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HealthUiState()
        )
}
