package com.perfectapp.ui.screens.calendar

import com.perfectapp.domain.calendar.EventOccurrence
import java.time.YearMonth

data class CalendarUiState(
    val isLoading: Boolean = true,
    val visibleMonth: YearMonth = YearMonth.now(),
    val occurrencesInMonth: List<EventOccurrence> = emptyList(),
    val upcomingAgenda: List<EventOccurrence> = emptyList(),
    val selectedDate: java.time.LocalDate? = null
)
