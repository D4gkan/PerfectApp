package com.perfectapp.ui.screens.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.DietGoalEntity
import com.perfectapp.data.entities.MealEntryEntity
import com.perfectapp.data.entities.WaterEntryEntity
import com.perfectapp.data.repository.DietRepository
import com.perfectapp.data.repository.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DietViewModel(
    private val dietRepository: DietRepository,
    private val waterRepository: WaterRepository
) : ViewModel() {

    val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<DietUiState> = combine(
        dietRepository.goal,
        selectedDate.flatMapLatest { dietRepository.mealsForDate(it) },
        selectedDate.flatMapLatest { waterRepository.entriesForDate(it) },
        dietRepository.allMeals,
        waterRepository.allEntries
    ) { goal, meals, waterEntries, allMeals, allWater ->
        DietUiState(
            isLoading = false,
            goal = goal,
            meals = meals,
            totals = dietRepository.dailyTotals(meals),
            waterEntries = waterEntries,
            waterTotalMl = waterEntries.sumOf { it.amountMl },
            allMeals = allMeals,
            allWaterEntries = allWater
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DietUiState()
    )

    fun addWater(amountMl: Int) {
        viewModelScope.launch { waterRepository.addWater(selectedDate.value, amountMl) }
    }

    fun removeWaterEntry(entry: WaterEntryEntity) {
        viewModelScope.launch { waterRepository.removeEntry(entry) }
    }

    fun deleteMeal(meal: MealEntryEntity) {
        viewModelScope.launch { dietRepository.deleteMeal(meal) }
    }

    fun saveGoal(goal: DietGoalEntity) {
        viewModelScope.launch { dietRepository.setGoal(goal) }
    }

    fun selectDate(date: LocalDate) { selectedDate.value = date }
}
