package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/** A personal activity; calendarEventId is optional so this stays independent of Calendar. */
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val calendarEventId: Long? = null
)
