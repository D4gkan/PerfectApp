package com.perfectapp.ui.screens.renewals

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.ReminderCategory
import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.repository.ReminderRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.perfectapp.ui.components.DatePickerField

private val RECURRENCE_PRESETS = listOf(
    "Monthly" to 1, "Quarterly" to 3, "Yearly" to 12
)

@Composable
fun AddRenewalScreen(
    repository: ReminderRepository,
    onSaved: () -> Unit,
    linkedCarId: Long? = null,
    existing: ReminderEntity? = null
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: if (linkedCarId == null) ReminderCategory.SUBSCRIPTION else ReminderCategory.CAR) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(existing?.amount?.toString().orEmpty()) }
    var currencyCode by remember { mutableStateOf(existing?.currencyCode ?: "USD") }
    var dueDate by remember { mutableStateOf(existing?.dueDate ?: LocalDate.now().plusMonths(1)) }
    var isRecurring by remember { mutableStateOf(existing?.isRecurring ?: true) }
    var recurrenceMonths by remember { mutableStateOf(existing?.recurrenceMonths ?: 1) }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existing == null) "Add Renewal" else "Edit Renewal") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (linkedCarId != null) item { Text("This renewal is linked to your vehicle.") }
            item {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Name (e.g. Netflix, Car Insurance)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ReminderCategory.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { category = option; categoryExpanded = false }
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currencyCode, onValueChange = { currencyCode = it.trim().uppercase().take(3) },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                DatePickerField("Next due date", dueDate, { dueDate = it }, Modifier.fillMaxWidth())
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recurring")
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
            }
            if (isRecurring) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val label = RECURRENCE_PRESETS.firstOrNull { it.second == recurrenceMonths }?.first
                            ?: "Every $recurrenceMonths months"
                        OutlinedTextField(
                            value = label,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Repeats") },
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
                            RECURRENCE_PRESETS.forEach { (label, months) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { recurrenceMonths = months; recurrenceExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
            errorMessage?.let { message ->
                item { Text(text = message, color = androidx.compose.ui.graphics.Color.Red) }
            }
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Name is required"
                            return@Button
                        }
                        val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
                        if (amount.isNotBlank() && (parsedAmount == null || !parsedAmount.isFinite() || parsedAmount < 0)) { errorMessage = "Enter a valid non-negative amount"; return@Button }
                        if (currencyCode !in setOf("USD", "TRY", "EUR", "GBP")) { errorMessage = "Choose USD, TRY, EUR or GBP"; return@Button }
                        errorMessage = null
                        val reminder = ReminderEntity(
                            id = existing?.id ?: 0,
                            isCompleted = existing?.isCompleted ?: false,
                            title = title,
                            dueDate = dueDate,
                            isRecurring = isRecurring,
                            recurrenceMonths = if (isRecurring) recurrenceMonths else null,
                            amount = parsedAmount,
                            currencyCode = currencyCode,
                            category = category,
                            carId = existing?.carId ?: linkedCarId
                        )
                        scope.launch {
                            if (existing == null) repository.add(reminder) else repository.update(reminder)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Renewal")
                }
            }
        }
    }
}
