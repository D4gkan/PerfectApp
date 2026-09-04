package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class BillingCycle { MONTHLY, YEARLY }

/**
 * A recurring, auto-charging subscription (streaming, software, memberships that bill
 * automatically). Distinct from [ReminderEntity]-based renewals, which are manual/no
 * auto-charge items — this one drives real transactions when it renews.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val currencyCode: String,
    val billingCycle: BillingCycle,
    val nextChargeDate: LocalDate,
    /** The local asset used when the simulated charge is recorded. */
    val fundingAssetId: Long? = null,
    val autoRenew: Boolean = true,
    val isActive: Boolean = true
)
