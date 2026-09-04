package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentOdometerKm: Long,
    val nextMaintenanceKm: Long? = null
)
