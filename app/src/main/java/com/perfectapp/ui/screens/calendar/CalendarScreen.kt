package com.perfectapp.ui.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.repository.CalendarRepository
import com.perfectapp.domain.calendar.EventOccurrence
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.EmptyState
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import java.time.format.DateTimeFormatter

private enum class CalendarViewMode { AGENDA, MONTH }

@Composable
fun CalendarScreen(
    repository: CalendarRepository,
    onAddEvent: () -> Unit,
    onEditEvent: (Long) -> Unit
) {
    val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory { CalendarViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(CalendarViewMode.AGENDA) }
    var pendingDelete by remember { mutableStateOf<com.perfectapp.data.entities.CalendarEventEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEvent) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == CalendarViewMode.AGENDA,
                        onClick = { mode = CalendarViewMode.AGENDA },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Agenda") }
                    SegmentedButton(
                        selected = mode == CalendarViewMode.MONTH,
                        onClick = { mode = CalendarViewMode.MONTH },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Month") }
                }
            }

            if (mode == CalendarViewMode.AGENDA) {
                if (state.upcomingAgenda.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No upcoming events",
                            message = "Add an event to start filling your calendar.",
                            actionLabel = "Add Event",
                            onActionClick = onAddEvent
                        )
                    }
                } else {
                    items(state.upcomingAgenda) { occurrence ->
                        EventRow(
                            occurrence = occurrence,
                            onDelete = { pendingDelete = occurrence.event },
                            onToggleTask = { viewModel.toggleTask(occurrence.event) },
                            onEdit = { onEditEvent(occurrence.event.id) }
                        )
                    }
                }
            } else {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        MonthGridView(
                            yearMonth = state.visibleMonth,
                            eventDates = state.occurrencesInMonth.map { it.occurrenceDateTime.toLocalDate() }.toSet(),
                            selectedDate = state.selectedDate,
                            onDateClick = { viewModel.selectDate(if (it == state.selectedDate) null else it) },
                            onPrevMonth = { viewModel.goToPreviousMonth() },
                            onNextMonth = { viewModel.goToNextMonth() }
                        )
                    }
                }

                val selected = state.selectedDate
                if (selected != null) {
                    val dayEvents = state.occurrencesInMonth.filter { it.occurrenceDateTime.toLocalDate() == selected }
                    item {
                        SectionHeader(title = selected.format(DateTimeFormatter.ofPattern("EEEE, MMM d")))
                    }
                    if (dayEvents.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No events this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(dayEvents) { occurrence ->
                            EventRow(occurrence = occurrence, onDelete = { pendingDelete = occurrence.event }, onToggleTask = { viewModel.toggleTask(occurrence.event) }, onEdit = { onEditEvent(occurrence.event.id) })
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { event ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null }, title = { Text("Delete event?") },
            text = { Text("This permanently removes ${event.title}.") },
            confirmButton = { androidx.compose.material3.Button(onClick = { viewModel.deleteEvent(event); pendingDelete = null }) { Text("Delete") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EventRow(occurrence: EventOccurrence, onDelete: () -> Unit, onToggleTask: () -> Unit, onEdit: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = occurrence.event.title, style = MaterialTheme.typography.titleMedium)
                Text(occurrence.event.itemType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } + if (occurrence.event.isCompleted) " · completed" else "", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = occurrence.occurrenceDateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")) +
                        if (occurrence.event.isRepeating) " · repeats" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                occurrence.event.notes?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit event") }
                if (occurrence.event.itemType == com.perfectapp.data.entities.CalendarItemType.TASK) IconButton(onClick = onToggleTask) { Icon(Icons.Filled.Check, contentDescription = "Toggle task completion") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete event")
                }
            }
        }
    }
}
