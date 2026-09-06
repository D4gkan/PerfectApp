package com.perfectapp.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.CalendarEventEntity
import com.perfectapp.data.entities.RecurrenceUnit
import com.perfectapp.data.entities.CalendarItemType
import com.perfectapp.data.repository.CalendarRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.perfectapp.ui.components.DatePickerField
import com.perfectapp.ui.components.TimePickerField

@Composable
fun AddEventScreen(
    repository: CalendarRepository,
    onSaved: () -> Unit,
    existing: CalendarEventEntity? = null,
    initialType: CalendarItemType = CalendarItemType.EVENT
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.dateTime?.toLocalDate() ?: LocalDate.now()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.dateTime?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.now().withSecond(0).withNano(0)) }
    var isAllDay by remember(existing?.id) { mutableStateOf(existing?.isAllDay ?: false) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var recurrence by remember(existing?.id) { mutableStateOf(existing?.recurrenceUnit) }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var itemType by remember(existing?.id) { mutableStateOf(existing?.itemType ?: initialType) }
    var typeExpanded by remember { mutableStateOf(false) }
    var reminderMinutes by remember(existing?.id) { mutableStateOf(existing?.reminderMinutesBefore) }
    var reminderExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (itemType == CalendarItemType.BIRTHDAY) "Birthday" else if (existing == null) "New Event" else "Edit Event") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = itemType.displayName, onValueChange = {}, readOnly = true, enabled = false, label = { Text("Item type") }, modifier = Modifier.fillMaxWidth())
                    Box(modifier = Modifier.matchParentSize().clickable { typeExpanded = true })
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        CalendarItemType.values().forEach { option -> DropdownMenuItem(text = { Text(option.displayName) }, onClick = { itemType = option; typeExpanded = false }) }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val reminderLabel = when (reminderMinutes) {
                        null -> "No reminder"
                        0 -> "At event time"
                        10 -> "10 minutes before"
                        30 -> "30 minutes before"
                        60 -> "1 hour before"
                        1440 -> "1 day before"
                        else -> "$reminderMinutes minutes before"
                    }
                    OutlinedTextField(value = reminderLabel, onValueChange = {}, readOnly = true, enabled = false, label = { Text("Reminder") }, modifier = Modifier.fillMaxWidth())
                    Box(modifier = Modifier.matchParentSize().clickable { reminderExpanded = true })
                    DropdownMenu(expanded = reminderExpanded, onDismissRequest = { reminderExpanded = false }) {
                        listOf(null to "No reminder", 0 to "At event time", 10 to "10 minutes before", 30 to "30 minutes before", 60 to "1 hour before", 1440 to "1 day before").forEach { (minutes, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { reminderMinutes = minutes; reminderExpanded = false })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(if (itemType == CalendarItemType.BIRTHDAY) "Person's name" else "Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                DatePickerField("Date", date, { date = it }, Modifier.fillMaxWidth())
            }
            if (!isAllDay && itemType != CalendarItemType.BIRTHDAY) {
                item {
                    TimePickerField("Time", time, { time = it }, Modifier.fillMaxWidth())
                }
            }
            if (itemType != CalendarItemType.BIRTHDAY) item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("All-day event")
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }
            }
            if (itemType == CalendarItemType.BIRTHDAY) item { Text("All day. Repeats every year. Shown separately in your widget.") }
            else item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = recurrence?.name ?: "Does not repeat",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Repeat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { recurrenceExpanded = true }
                    )
                    DropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Does not repeat") },
                            onClick = { recurrence = null; recurrenceExpanded = false }
                        )
                        RecurrenceUnit.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { recurrence = option; recurrenceExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            errorMessage?.let { message ->
                item {
                    Text(text = message, color = androidx.compose.ui.graphics.Color.Red)
                }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Title is required"
                            return@Button
                        }
                        val eventTime = if (isAllDay || itemType == CalendarItemType.BIRTHDAY) LocalTime.MIDNIGHT else time
                        errorMessage = null
                        val event = CalendarEventEntity(
                            id = existing?.id ?: 0,
                            title = title,
                            dateTime = LocalDateTime.of(date, eventTime),
                            isAllDay = isAllDay || itemType == CalendarItemType.BIRTHDAY,
                            notes = notes.ifBlank { null },
                            recurrenceUnit = if (itemType == CalendarItemType.BIRTHDAY) RecurrenceUnit.YEARLY else recurrence
                            ,itemType = itemType,
                            isCompleted = existing?.isCompleted ?: false,
                            reminderMinutesBefore = reminderMinutes
                        )
                        scope.launch {
                            if (existing == null) repository.addEvent(event) else repository.updateEvent(event)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (existing == null) "Save Event" else "Save Changes")
                }
            }
        }
    }
}
