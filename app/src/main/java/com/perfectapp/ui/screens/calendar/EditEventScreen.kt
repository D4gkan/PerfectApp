package com.perfectapp.ui.screens.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.perfectapp.data.repository.CalendarRepository

@Composable fun EditEventScreen(eventId: Long, repository: CalendarRepository, onSaved: () -> Unit) {
    val events by repository.allEvents.collectAsState(initial = emptyList())
    val event = events.firstOrNull { it.id == eventId }
    if (event != null) AddEventScreen(repository, onSaved, event)
}
