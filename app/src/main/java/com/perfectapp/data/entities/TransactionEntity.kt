package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime = LocalTime.now(),
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val currencyCode: String,
    /** Asset balance affected by this entry. Null is allowed for imported history. */
    val assetId: Long? = null,
    val note: String? = null,
    /** Source subscription for auditable automatic charges; null for manual entries. */
    val subscriptionId: Long? = null,
    val isAutomatic: Boolean = false
)
