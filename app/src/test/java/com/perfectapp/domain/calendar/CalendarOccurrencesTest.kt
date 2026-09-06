package com.perfectapp.domain.calendar

import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.RecurrenceUnit
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class CalendarOccurrencesTest {
    private val now = LocalDateTime.of(2026, 9, 6, 10, 0)

    @Test fun oldDailySeriesStillAppearsAfterFiveHundredDays() {
        val event = CalendarEventEntity(title = "Daily", dateTime = now.minusYears(3), recurrenceUnit = RecurrenceUnit.DAILY)
        val occurrences = CalendarOccurrences.expand(listOf(event), now, now.plusDays(1))
        assertEquals(listOf(now, now.plusDays(1)), occurrences.map { it.occurrenceDateTime })
    }

    @Test fun upcomingSortsRepeatsAndOneOffEventsAndOmitsCompletedItems() {
        val events = listOf(
            CalendarEventEntity(title = "Past", dateTime = now.minusDays(1)),
            CalendarEventEntity(title = "Done", dateTime = now, isCompleted = true),
            CalendarEventEntity(title = "Weekly", dateTime = now.minusWeeks(60), recurrenceUnit = RecurrenceUnit.WEEKLY),
            CalendarEventEntity(title = "Tomorrow", dateTime = now.plusDays(1))
        )
        val result = CalendarOccurrences.upcoming(events, now)
        assertEquals(listOf("Weekly", "Tomorrow", "Weekly"), result.map { it.event.title })
        assertEquals(now.plusWeeks(1), result.last().occurrenceDateTime)
    }

    @Test fun distantOneOffEventIsNotHiddenByAnArbitraryWindow() {
        val event = CalendarEventEntity(title = "Future", dateTime = now.plusYears(5))
        assertEquals(event, CalendarOccurrences.upcoming(listOf(event), now).single().event)
    }

    @Test fun emptyCalendarHasNoUpcomingOccurrence() {
        assertTrue(CalendarOccurrences.upcoming(emptyList(), now).isEmpty())
    }
}
