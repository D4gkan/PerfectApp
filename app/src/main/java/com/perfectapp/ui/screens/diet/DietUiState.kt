package com.perfectapp.ui.screens.diet

import com.perfectapp.data.entities.DietGoalEntity
import com.perfectapp.data.entities.MealEntryEntity
import com.perfectapp.data.entities.WaterEntryEntity
import com.perfectapp.data.repository.DailyDietTotals

data class DietUiState(
    val isLoading: Boolean = true,
    val goal: DietGoalEntity? = null,
    val meals: List<MealEntryEntity> = emptyList(),
    val totals: DailyDietTotals = DailyDietTotals(),
    val waterEntries: List<WaterEntryEntity> = emptyList(),
    val waterTotalMl: Int = 0
    ,val allMeals: List<MealEntryEntity> = emptyList()
    ,val allWaterEntries: List<WaterEntryEntity> = emptyList()
) {
    val waterGoalMl: Int get() = goal?.waterGoalMl ?: 2500
}
