package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_goals")
data class DietGoalEntity(
    @PrimaryKey val id: Int = 1,
    val calorieGoal: Int,
    val proteinGoalG: Int,
    val carbsGoalG: Int,
    val fatGoalG: Int,
    val waterGoalMl: Int = 2500
)
