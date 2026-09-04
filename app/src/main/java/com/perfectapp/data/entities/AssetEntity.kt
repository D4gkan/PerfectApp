package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class AssetType { CASH, BANK, GOLD, STOCK, CRYPTO, LIABILITY, OTHER }

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AssetType,
    val currencyCode: String,
    val quantity: Double,
    val valuePerUnit: Double,
    val isLiability: Boolean = false,
    /** Default assets can remain in history without being offered for new entries. */
    val isActive: Boolean = true,
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /** Cash-style assets use quantity as their native-currency balance. */
    val balance: Double get() = quantity
}
