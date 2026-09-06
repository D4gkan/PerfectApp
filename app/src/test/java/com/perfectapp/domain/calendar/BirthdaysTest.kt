package com.perfectapp.domain.calendar

import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.CalendarItemType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BirthdaysTest {
    @Test fun countdownUsesCalendarDays() {
        val today = LocalDate.of(2026, 9, 6)
        assertEquals("Birthday in 64 days", Birthdays.countdown(today.plusDays(64), today))
        assertEquals("Birthday today", Birthdays.countdown(today, today))
        assertEquals("Birthday tomorrow", Birthdays.countdown(today.plusDays(1), today))
    }
    @Test fun pastBirthdayRollsIntoNextYear() {
        assertEquals(LocalDate.of(2027, 1, 3), Birthdays.nextDate(LocalDate.of(1999, 1, 3), LocalDate.of(2026, 9, 6)))
    }
    @Test fun leapBirthdayRetainsOriginalMonthDay() {
        val birth = LocalDate.of(2000, 2, 29)
        assertEquals(LocalDate.of(2027, 2, 28), Birthdays.nextDate(birth, LocalDate.of(2027, 1, 1)))
        assertEquals(LocalDate.of(2028, 2, 29), Birthdays.nextDate(birth, LocalDate.of(2027, 3, 1)))
    }
    @Test fun birthdaysAreSeparateFromTasksAndNotLimitedByDailyRepeats() {
        val today = LocalDate.of(2026, 9, 6)
        val birthday = CalendarEventEntity(id = 1, title = "Birthday", dateTime = today.plusDays(64).atStartOfDay(), itemType = CalendarItemType.BIRTHDAY)
        val task = birthday.copy(id = 2, itemType = CalendarItemType.TASK)
        val completed = birthday.copy(id = 3, isCompleted = true)
        assertEquals(listOf(1L), Birthdays.upcoming(listOf(task, completed, birthday), today).map { it.event.id })
    }
}
