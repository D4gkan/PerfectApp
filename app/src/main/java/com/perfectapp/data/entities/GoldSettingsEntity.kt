package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted calculator inputs; gold is valued in TRY and converted only for display. */
@Entity(tableName = "gold_settings")
data class GoldSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val gramGoldPriceTry: Double = 0.0,
    val quarterQuantity: Double = 0.0,
    val halfQuantity: Double = 0.0,
    val fullQuantity: Double = 0.0,
    val republicQuantity: Double = 0.0
) {
    companion object { const val SINGLETON_ID = 1 }
}
