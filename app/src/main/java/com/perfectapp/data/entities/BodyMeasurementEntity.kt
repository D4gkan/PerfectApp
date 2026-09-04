package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measurementDate: LocalDate,
    val measurementTime: LocalTime,
    val bodyType: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double,
    val bmi: Double? = null,
    val bodyFatPercent: Double? = null,
    val fatMassKg: Double? = null,
    val ffmKg: Double? = null,
    val visceralFatRating: Double? = null,
    val wholeBodyImpedance: Double? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
