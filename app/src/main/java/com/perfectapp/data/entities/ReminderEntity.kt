package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ReminderCategory { SUBSCRIPTION, INSURANCE, UTILITY, MEMBERSHIP, CAR, OTHER }

/**
 * Doubles as both a plain reminder and a recurring expense/renewal (subscription, insurance,
 * membership) when [amount] is set — the same entity backs both the Home dashboard's simple
 * "Reminders" list and the dedicated Expenses & Renewals screen.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: LocalDate,
    val isRecurring: Boolean = false,
    val recurrenceMonths: Int? = null,
    val isCompleted: Boolean = false,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val category: ReminderCategory? = null,
    /** Optional vehicle relationship; null keeps ordinary renewals independent. */
    val carId: Long? = null
)
