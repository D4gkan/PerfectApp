package com.perfectapp.domain.calendar

import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.CalendarItemType
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

object Birthdays {
    // MonthDay retains February 29 across non-leap years (observed February 28).
    fun nextDate(date: LocalDate, today: LocalDate): LocalDate {
        val day = MonthDay.from(date)
        val thisYear = day.atYear(today.year)
        return if (thisYear.isBefore(today)) day.atYear(today.year + 1) else thisYear
    }
    fun upcoming(events: List<CalendarEventEntity>, today: LocalDate): List<EventOccurrence> =
        events.filter { it.itemType == CalendarItemType.BIRTHDAY && !it.isCompleted }
            .map { EventOccurrence(it, nextDate(it.dateTime.toLocalDate(), today).atStartOfDay()) }
            .sortedBy { it.occurrenceDateTime }

    fun countdown(date: LocalDate, today: LocalDate): String = when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> "Birthday today"
        1L -> "Birthday tomorrow"
        else -> "Birthday in $days days"
    }
}
