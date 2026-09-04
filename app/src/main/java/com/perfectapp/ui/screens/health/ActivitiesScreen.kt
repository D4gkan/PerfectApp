package com.perfectapp.ui.screens.health

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.ActivityEntity
import com.perfectapp.data.repository.HealthRepository
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.DatePickerField
import com.perfectapp.ui.components.TimePickerField
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ActivitiesScreen(repository: HealthRepository) {
    val activities by repository.activities.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Daily activities") }) }, floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add activity") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (activities.isEmpty()) item { Text("Log gym sessions, walks, swimming, or any other activity.") }
            items(activities) { activity ->
                PremiumCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(activity.name); Text("${activity.date}${activity.time?.let { " · $it" } ?: ""}${if (activity.isCompleted) " · completed" else ""}"); activity.notes?.let { Text(it) } }
                    IconButton(onClick = { scope.launch { repository.updateActivity(activity.copy(isCompleted = !activity.isCompleted)) } }) { Icon(Icons.Filled.Check, "Toggle completion") }
                } }
            }
        }
    }
    if (showAdd) AddActivityDialog(onDismiss = { showAdd = false }) { activity -> scope.launch { repository.saveActivity(activity); showAdd = false } }
}

@Composable private fun AddActivityDialog(onDismiss: () -> Unit, onSave: (ActivityEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var notes by remember { mutableStateOf("") }
    var calendarId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add activity") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            DatePickerField("Date", date, { date = it })
            TimePickerField("Time", time, { time = it })
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
            OutlinedTextField(calendarId, { calendarId = it }, label = { Text("Calendar event ID (optional)") })
        } },
        confirmButton = { androidx.compose.material3.Button(onClick = {
            if (name.isNotBlank()) onSave(ActivityEntity(name = name.trim(), date = date, time = time, notes = notes.ifBlank { null }, calendarEventId = calendarId.toLongOrNull()))
        }) { Text("Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
