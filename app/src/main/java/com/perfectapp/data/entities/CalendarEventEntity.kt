package com.perfectapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class RecurrenceUnit { DAILY, WEEKLY, MONTHLY, YEARLY }
enum class CalendarItemType { EVENT, TASK, UNIVERSITY_LESSON, BIRTHDAY;
    val displayName: String get() = when (this) {
        UNIVERSITY_LESSON -> "University"
        else -> name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val notes: String? = null,
    val recurrenceUnit: RecurrenceUnit? = null
    ,val itemType: CalendarItemType = CalendarItemType.EVENT
    ,val isCompleted: Boolean = false
    /** Null means no event-specific alert; otherwise alert this many minutes before it starts. */
    ,val reminderMinutesBefore: Int? = null
) {
    val isRepeating: Boolean get() = recurrenceUnit != null
}
