package com.perfectapp.ui.screens.home

import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.repository.DailyDietTotals

data class HomeUiState(
    val isLoading: Boolean = true,
    // Health
    val latestWeightKg: Double? = null,
    val weightChangeKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val fatMassKg: Double? = null,
    val fatMassChangeKg: Double? = null,
    val muscleGainedKg: Double? = null,
    val bmi: Double? = null,
    // Water
    val waterTodayMl: Int = 0,
    val waterGoalMl: Int = 2500,
    // Diet
    val dietTotals: DailyDietTotals = DailyDietTotals(),
    val calorieGoal: Int? = null,
    val proteinGoal: Int? = null,
    val carbsGoal: Int? = null,
    val fatGoal: Int? = null,
    // Wealth
    val netWorth: Double = 0.0,
    val netWorthChangeThisMonth: Double? = null,
    val baseCurrency: String = "USD",
    // Calendar
    val nextEvent: CalendarEventEntity? = null,
    // Car
    val carName: String? = null,
    val carKmRemaining: Long? = null,
    // Reminders
    val upcomingReminders: List<ReminderEntity> = emptyList()
)
