package com.perfectapp.data

import androidx.room.TypeConverter
import com.perfectapp.data.entities.AssetType
import com.perfectapp.data.entities.BillingCycle
import com.perfectapp.data.entities.BodySegment
import com.perfectapp.data.entities.RecurrenceUnit
import com.perfectapp.data.entities.ReminderCategory
import com.perfectapp.data.entities.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalDate(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun toLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun fromLocalTime(v: LocalTime?): String? = v?.toString()
    @TypeConverter fun toLocalTime(v: String?): LocalTime? = v?.let(LocalTime::parse)

    @TypeConverter fun fromLocalDateTime(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun toLocalDateTime(v: String?): LocalDateTime? = v?.let(LocalDateTime::parse)

    @TypeConverter fun fromBodySegment(v: BodySegment?): String? = v?.name
    @TypeConverter fun toBodySegment(v: String?): BodySegment? = v?.let { BodySegment.valueOf(it) }

    @TypeConverter fun fromAssetType(v: AssetType?): String? = v?.name
    @TypeConverter fun toAssetType(v: String?): AssetType? = v?.let { AssetType.valueOf(it) }

    @TypeConverter fun fromTransactionType(v: TransactionType?): String? = v?.name
    @TypeConverter fun toTransactionType(v: String?): TransactionType? = v?.let { TransactionType.valueOf(it) }

    @TypeConverter fun fromRecurrenceUnit(v: RecurrenceUnit?): String? = v?.name
    @TypeConverter fun toRecurrenceUnit(v: String?): RecurrenceUnit? = v?.let { RecurrenceUnit.valueOf(it) }

    @TypeConverter fun fromReminderCategory(v: ReminderCategory?): String? = v?.name
    @TypeConverter fun toReminderCategory(v: String?): ReminderCategory? = v?.let { ReminderCategory.valueOf(it) }

    @TypeConverter fun fromBillingCycle(v: BillingCycle?): String? = v?.name
    @TypeConverter fun toBillingCycle(v: String?): BillingCycle? = v?.let { BillingCycle.valueOf(it) }
}
