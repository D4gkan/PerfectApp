package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "fuel_entries", foreignKeys = [ForeignKey(entity = CarEntity::class, parentColumns = ["id"], childColumns = ["carId"], onDelete = ForeignKey.CASCADE)], indices = [Index("carId")])
data class FuelEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: LocalDate,
    val odometerKm: Long? = null,
    val liters: Double,
    val totalCost: Double,
    val currencyCode: String = "USD",
    val notes: String? = null,
    /** The expense created in Wealth for this fill-up, when a funding asset was selected. */
    val transactionId: Long? = null
)
