package com.perfectapp.domain.calendar

import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.RecurrenceUnit
import java.time.LocalDateTime

/** A single point-in-time occurrence of an event — either the event's own dateTime,
 *  or one generated instance of a repeating event. */
data class EventOccurrence(
    val event: CalendarEventEntity,
    val occurrenceDateTime: LocalDateTime
)

object CalendarOccurrences {

    /**
     * Expands a list of events (some possibly repeating) into concrete occurrences
     * that fall within [rangeStart, rangeEnd]. Non-repeating events contribute at most
     * one occurrence; repeating events contribute one per recurrence step in range.
     * A safety cap prevents runaway loops for very wide ranges.
     */
    fun expand(
        events: List<CalendarEventEntity>,
        rangeStart: LocalDateTime,
        rangeEnd: LocalDateTime,
        maxOccurrencesPerEvent: Int = 500
    ): List<EventOccurrence> {
        val result = mutableListOf<EventOccurrence>()

        for (event in events) {
            val unit = event.recurrenceUnit
            if (unit == null) {
                if (!event.dateTime.isBefore(rangeStart) && !event.dateTime.isAfter(rangeEnd)) {
                    result.add(EventOccurrence(event, event.dateTime))
                }
                continue
            }

            var current = event.dateTime
            var count = 0
            while (current.isBefore(rangeEnd.plusSeconds(1)) && count < maxOccurrencesPerEvent) {
                if (!current.isBefore(rangeStart)) {
                    result.add(EventOccurrence(event, current))
                }
                current = step(current, unit)
                count++
            }
        }

        return result.sortedBy { it.occurrenceDateTime }
    }

    private fun step(dateTime: LocalDateTime, unit: RecurrenceUnit): LocalDateTime = when (unit) {
        RecurrenceUnit.DAILY -> dateTime.plusDays(1)
        RecurrenceUnit.WEEKLY -> dateTime.plusWeeks(1)
        RecurrenceUnit.MONTHLY -> dateTime.plusMonths(1)
        RecurrenceUnit.YEARLY -> dateTime.plusYears(1)
    }
}
