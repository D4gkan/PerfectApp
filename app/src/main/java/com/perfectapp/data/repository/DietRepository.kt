package com.perfectapp.data.repository

import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.DietGoalEntity
import com.perfectapp.data.entities.MealEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class DailyDietTotals(
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0
)

class DietRepository(db: PerfectDatabase) {
    private val dao = db.dietDao()
    val goal: Flow<DietGoalEntity?> = dao.observeGoal()
    fun mealsForDate(date: LocalDate): Flow<List<MealEntryEntity>> = dao.observeMealsForDate(date)
    val allMeals: Flow<List<MealEntryEntity>> = dao.observeAllMeals()
    suspend fun setGoal(goal: DietGoalEntity) = dao.setGoal(goal)
    suspend fun addMeal(meal: MealEntryEntity) = dao.insertMeal(meal)
    suspend fun deleteMeal(meal: MealEntryEntity) = dao.deleteMeal(meal)

    fun dailyTotals(meals: List<MealEntryEntity>): DailyDietTotals = DailyDietTotals(
        calories = meals.sumOf { it.calories },
        proteinG = meals.sumOf { it.proteinG },
        carbsG = meals.sumOf { it.carbsG },
        fatG = meals.sumOf { it.fatG }
    )
}
