package com.perfectapp.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.BodySegment
import com.perfectapp.data.repository.HealthRepository
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import java.time.LocalDate
import java.time.LocalTime
import com.perfectapp.ui.components.DatePickerField
import com.perfectapp.ui.components.TimePickerField

@Composable
fun AddMeasurementScreen(
    repository: HealthRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddMeasurementViewModel =
        viewModel(factory = viewModelFactory { AddMeasurementViewModel(repository) })

    var weight by remember { mutableStateOf("") }
    var measurementDate by remember { mutableStateOf(LocalDate.now()) }
    var measurementTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var bodyType by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var fatMass by remember { mutableStateOf("") }
    var ffm by remember { mutableStateOf("") }
    var visceralFat by remember { mutableStateOf("") }
    var impedance by remember { mutableStateOf("") }

    val segmentInputs = remember {
        BodySegment.values().associateWith { mutableStateOf(SegmentDraft()) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Measurement") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(title = "General") }
            item { DatePickerField("Measurement date", measurementDate, { measurementDate = it }, Modifier.fillMaxWidth()) }
            item { TimePickerField("Measurement time", measurementTime, { measurementTime = it }, Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = bodyType, onValueChange = { bodyType = it }, label = { Text("Body type (optional)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender (optional)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age (optional)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }

            item { SectionHeader(title = "Main Body Composition") }

            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { OutlinedTextField(value = fatMass, onValueChange = { fatMass = it }, label = { Text("Fat Mass (kg) — optional override") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = ffm, onValueChange = { ffm = it }, label = { Text("Fat-Free Mass (kg) — optional override") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
            item {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm) — optional, needed for BMI") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it },
                    label = { Text("Body Fat % — optional") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = visceralFat,
                    onValueChange = { visceralFat = it },
                    label = { Text("Visceral Fat Rating — optional") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = impedance,
                    onValueChange = { impedance = it },
                    label = { Text("Whole Body Impedance (Ω) — optional") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionHeader(title = "Segmental Composition (optional)") }
            item {
                BodyCompositionMap(modifier = Modifier.fillMaxWidth())
            }
            items(BodySegment.values().toList()) { segment ->
                val state = segmentInputs.getValue(segment)
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = segment.name.replace("_", " "), style = MaterialTheme.typography.titleMedium)
                        SegmentFields(state)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val weightValue = weight.toDoubleOrNull()
                        if (weightValue == null) return@Button
                        val segmentEntries = segmentInputs.mapValues { (_, state) -> state.value.toInput() }
                        viewModel.saveMeasurement(
                            weightKg = weightValue,
                            measurementDate = measurementDate,
                            measurementTime = measurementTime,
                            bodyType = bodyType.ifBlank { null },
                            gender = gender.ifBlank { null },
                            age = age.toIntOrNull(),
                            heightCm = height.toDoubleOrNull(),
                            bodyFatPercent = bodyFat.toDoubleOrNull(),
                            manualFatMassKg = fatMass.toDoubleOrNull(),
                            manualFfmKg = ffm.toDoubleOrNull(),
                            visceralFatRating = visceralFat.toDoubleOrNull(),
                            wholeBodyImpedance = impedance.toDoubleOrNull(),
                            segmentEntries = segmentEntries,
                            onSaved = onSaved
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Measurement")
                }
            }
        }
    }
}

/** A neutral, non-anatomical body map that makes the five segment readings easy to locate. */
@Composable
private fun BodyCompositionMap(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outline
    PremiumCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Body map", style = MaterialTheme.typography.titleMedium)
            Text("Enter each reading in the matching section below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                val cx = size.width / 2f
                val unit = size.height / 26f
                // Head, trunk, arms, and legs form a simple, inclusive body silhouette.
                drawCircle(muted, radius = unit * 2f, center = androidx.compose.ui.geometry.Offset(cx, unit * 3f))
                drawRoundRect(accent.copy(alpha = .82f), androidx.compose.ui.geometry.Offset(cx - unit * 3f, unit * 5.5f), androidx.compose.ui.geometry.Size(unit * 6f, unit * 7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit))
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(cx - unit * 7f, unit * 6.5f), androidx.compose.ui.geometry.Size(unit * 3f, unit * 7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit))
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(cx + unit * 4f, unit * 6.5f), androidx.compose.ui.geometry.Size(unit * 3f, unit * 7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit))
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(cx - unit * 3f, unit * 13f), androidx.compose.ui.geometry.Size(unit * 2.5f, unit * 10f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit))
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(cx + unit * .5f, unit * 13f), androidx.compose.ui.geometry.Size(unit * 2.5f, unit * 10f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit))
                drawCircle(Color.Transparent, radius = unit * 2f, center = androidx.compose.ui.geometry.Offset(cx, unit * 3f), style = Stroke(width = 1.5f))
                drawRoundRect(Color.Transparent, androidx.compose.ui.geometry.Offset(cx - unit * 3f, unit * 5.5f), androidx.compose.ui.geometry.Size(unit * 6f, unit * 7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit), style = Stroke(width = 1.5f))
            }
            Text("Head • Trunk • Left arm • Right arm • Left leg • Right leg", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class SegmentDraft(val fatPercent: String = "", val fatMass: String = "", val ffm: String = "", val muscle: String = "", val impedance: String = "") {
    fun toInput() = SegmentInput(fatPercent.toDoubleOrNull(), fatMass.toDoubleOrNull(), ffm.toDoubleOrNull(), muscle.toDoubleOrNull(), impedance.toDoubleOrNull())
}

@Composable
private fun SegmentFields(state: androidx.compose.runtime.MutableState<SegmentDraft>) {
    val options = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.value.fatPercent, { state.value = state.value.copy(fatPercent = it) }, label = { Text("Fat %") }, keyboardOptions = options, modifier = Modifier.weight(1f))
            OutlinedTextField(state.value.fatMass, { state.value = state.value.copy(fatMass = it) }, label = { Text("Fat mass kg") }, keyboardOptions = options, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.value.ffm, { state.value = state.value.copy(ffm = it) }, label = { Text("FFM kg") }, keyboardOptions = options, modifier = Modifier.weight(1f))
            OutlinedTextField(state.value.muscle, { state.value = state.value.copy(muscle = it) }, label = { Text("Muscle kg") }, keyboardOptions = options, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(state.value.impedance, { state.value = state.value.copy(impedance = it) }, label = { Text("Impedance Ω") }, keyboardOptions = options, modifier = Modifier.fillMaxWidth())
    }
}
