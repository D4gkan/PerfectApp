package com.perfectapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        container.healthRepository.allMeasurements,
        container.waterRepository.totalForDate(today),
        container.dietRepository.mealsForDate(today),
        container.dietRepository.goal,
        container.wealthRepository.assets,
        container.wealthRepository.exchangeRates,
        container.wealthRepository.goldSettings,
        container.wealthRepository.snapshots,
        container.calendarRepository.nextEvent(),
        container.carRepository.primaryCar,
        container.reminderRepository.upcoming
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val measurements = flows[0] as List<com.perfectapp.data.entities.BodyMeasurementEntity>
        val latestMeasurement = measurements.firstOrNull()
        val firstMeasurement = measurements.lastOrNull()
        val waterTotal = flows[1] as Int
        val meals = flows[2] as List<com.perfectapp.data.entities.MealEntryEntity>
        val dietGoal = flows[3] as com.perfectapp.data.entities.DietGoalEntity?
        val assets = flows[4] as List<com.perfectapp.data.entities.AssetEntity>
        val rates = (flows[5] as List<com.perfectapp.data.entities.ExchangeRateEntity>)
            .associate { it.currencyCode to it.rateToBase }
        val gold = flows[6] as com.perfectapp.data.entities.GoldSettingsEntity?
        val snapshots = flows[7] as List<com.perfectapp.data.entities.NetWorthSnapshotEntity>
        val nextEvent = flows[8] as com.perfectapp.data.entities.CalendarEventEntity?
        val car = flows[9] as com.perfectapp.data.entities.CarEntity?
        val reminders = flows[10] as List<com.perfectapp.data.entities.ReminderEntity>

        val netWorth = container.wealthRepository.calculateNetWorth(assets, rates, "USD") +
            (gold?.let { container.wealthRepository.goldValueUsd(it, rates) } ?: 0.0)
        val netWorthChange = container.wealthRepository.netWorthChangeThisMonth(snapshots, netWorth)

        HomeUiState(
            isLoading = false,
            latestWeightKg = latestMeasurement?.weightKg,
            bodyFatPercent = latestMeasurement?.bodyFatPercent,
            fatMassKg = latestMeasurement?.fatMassKg,
            weightChangeKg = container.healthRepository.weightChangeSince(firstMeasurement, latestMeasurement),
            fatMassChangeKg = container.healthRepository.fatMassChange(firstMeasurement, latestMeasurement),
            muscleGainedKg = container.healthRepository.muscleGained(firstMeasurement, latestMeasurement),
            bmi = latestMeasurement?.bmi,
            waterTodayMl = waterTotal,
            waterGoalMl = dietGoal?.waterGoalMl ?: 2500,
            dietTotals = container.dietRepository.dailyTotals(meals),
            calorieGoal = dietGoal?.calorieGoal,
            proteinGoal = dietGoal?.proteinGoalG,
            carbsGoal = dietGoal?.carbsGoalG,
            fatGoal = dietGoal?.fatGoalG,
            netWorth = netWorth,
            netWorthChangeThisMonth = netWorthChange,
            nextEvent = nextEvent,
            carName = car?.name,
            carKmRemaining = container.carRepository.kmRemaining(car),
            upcomingReminders = reminders.take(3)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
