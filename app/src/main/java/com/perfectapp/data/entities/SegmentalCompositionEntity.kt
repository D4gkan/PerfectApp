package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BodySegment { RIGHT_ARM, LEFT_ARM, TRUNK, RIGHT_LEG, LEFT_LEG }

@Entity(
    tableName = "segmental_composition",
    foreignKeys = [ForeignKey(
        entity = BodyMeasurementEntity::class,
        parentColumns = ["id"],
        childColumns = ["measurementId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("measurementId")]
)
data class SegmentalCompositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measurementId: Long,
    val segment: BodySegment,
    val fatPercent: Double? = null,
    val fatMassKg: Double? = null,
    val ffmKg: Double? = null,
    val predictedMuscleMassKg: Double? = null,
    val impedance: Double? = null
)
