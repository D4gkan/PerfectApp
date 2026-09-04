package com.perfectapp.ui.screens.car

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.MaintenanceEntity
import com.perfectapp.data.repository.CarRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun AddMaintenanceScreen(
    repository: CarRepository,
    carId: Long,
    onSaved: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueKm by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Maintenance Item") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title (e.g. Oil Change)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = dueKm, onValueChange = { dueKm = it },
                    label = { Text("Due at odometer (km) — optional") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = dueDateText, onValueChange = { dueDateText = it },
                    label = { Text("Due date (yyyy-MM-dd) — optional") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            errorMessage?.let { message ->
                item { Text(text = message, color = androidx.compose.ui.graphics.Color.Red) }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Title is required"
                            return@Button
                        }
                        val dueDate = if (dueDateText.isBlank()) null else try {
                            LocalDate.parse(dueDateText, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: DateTimeParseException) {
                            errorMessage = "Date must be in yyyy-MM-dd format"
                            return@Button
                        }
                        errorMessage = null
                        val item = MaintenanceEntity(
                            carId = carId,
                            title = title,
                            dueAtOdometerKm = dueKm.toLongOrNull(),
                            dueDate = dueDate,
                            notes = notes.ifBlank { null }
                        )
                        scope.launch {
                            repository.addMaintenance(item)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Item")
                }
            }
        }
    }
}
