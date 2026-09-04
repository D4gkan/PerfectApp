package com.perfectapp.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.repository.CalendarRepository
import com.perfectapp.domain.calendar.CalendarOccurrences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.allEvents,
        visibleMonth,
        selectedDate
    ) { events, month, selected ->
        val monthStart = month.atDay(1).atStartOfDay()
        val monthEnd = month.atEndOfMonth().atTime(23, 59, 59)
        val occurrencesInMonth = CalendarOccurrences.expand(events, monthStart, monthEnd)

        val now = LocalDateTime.now()
        val agendaEnd = now.plusYears(2)
        val upcomingAgenda = CalendarOccurrences.expand(events, now, agendaEnd).take(50)

        CalendarUiState(
            isLoading = false,
            visibleMonth = month,
            occurrencesInMonth = occurrencesInMonth,
            upcomingAgenda = upcomingAgenda,
            selectedDate = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun goToNextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    fun goToPreviousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    fun selectDate(date: LocalDate?) {
        selectedDate.value = date
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun toggleTask(event: CalendarEventEntity) {
        viewModelScope.launch { repository.updateEvent(event.copy(isCompleted = !event.isCompleted)) }
    }
}
