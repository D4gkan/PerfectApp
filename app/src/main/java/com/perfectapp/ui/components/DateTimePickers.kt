package com.perfectapp.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Native pickers keep dates valid and respect the device's familiar controls. */
@Composable
fun DatePickerField(label: String, value: LocalDate, onValueChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        OutlinedTextField(value = value.format(DateTimeFormatter.ofPattern("MMM d, yyyy")), onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.matchParentSize().clickable {
            DatePickerDialog(context, { _, year, month, day -> onValueChange(LocalDate.of(year, month + 1, day)) }, value.year, value.monthValue - 1, value.dayOfMonth).show()
        })
    }
}

@Composable
fun TimePickerField(label: String, value: LocalTime, onValueChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        OutlinedTextField(value = value.format(DateTimeFormatter.ofPattern("HH:mm")), onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.matchParentSize().clickable {
            TimePickerDialog(context, { _, hour, minute -> onValueChange(LocalTime.of(hour, minute)) }, value.hour, value.minute, true).show()
        })
    }
}
