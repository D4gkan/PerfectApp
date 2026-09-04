package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.perfectapp.data.entities.DietGoalEntity
import com.perfectapp.data.entities.MealEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DietDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGoal(goal: DietGoalEntity)

    @Query("SELECT * FROM diet_goals WHERE id = 1")
    fun observeGoal(): Flow<DietGoalEntity?>

    @Insert
    suspend fun insertMeal(meal: MealEntryEntity): Long

    @Delete
    suspend fun deleteMeal(meal: MealEntryEntity)

    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY loggedAt DESC")
    fun observeMealsForDate(date: LocalDate): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries ORDER BY date DESC, loggedAt DESC")
    fun observeAllMeals(): Flow<List<MealEntryEntity>>
}
