package com.perfectapp.ui.screens.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.BodyMeasurementEntity
import com.perfectapp.data.entities.BodySegment
import com.perfectapp.data.entities.SegmentalCompositionEntity
import com.perfectapp.data.repository.HealthRepository
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.perfectapp.domain.health.HealthCalculations

/** Complete read-only view. The first measurement is deliberately never editable. */
@Composable
fun MeasurementDetailScreen(measurementId: Long, repository: HealthRepository) {
    val measurements by repository.allMeasurements.collectAsState(initial = emptyList())
    val measurement = measurements.firstOrNull { it.id == measurementId }
    val segments by repository.segmentsFor(measurementId).collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<BodySegment?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isBaseline = measurement != null && measurements.lastOrNull()?.id == measurement.id
    if (showEdit && measurement != null) EditMeasurementDialog(measurement, onDismiss = { showEdit = false }) { updated ->
        scope.launch { repository.updateMeasurement(updated); showEdit = false }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Measurement details") }) }) { padding ->
        if (measurement == null) return@Scaffold
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Summary(measurement) }
            item {
                if (isBaseline) Text("This is your permanent baseline and is locked to preserve historical comparisons.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else androidx.compose.material3.OutlinedButton(onClick = { showEdit = true }) { Text("Edit measurement") }
            }
            item { SectionHeader("Body composition map") }
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tap a body region to inspect its saved values.", style = MaterialTheme.typography.bodySmall)
                        listOf(BodySegment.RIGHT_ARM, BodySegment.LEFT_ARM, BodySegment.TRUNK, BodySegment.RIGHT_LEG, BodySegment.LEFT_LEG).forEach { segment ->
                            val hasData = segments.any { it.segment == segment }
                            Text(
                                text = segment.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } + if (hasData) "  •" else "  — no data",
                                modifier = Modifier.fillMaxWidth().clickable { selected = segment },
                                color = if (hasData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            selected?.let { segment ->
                item { SegmentDetails(segment, segments.firstOrNull { it.segment == segment }) }
            }
            item { SectionHeader("Impedance analysis") }
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Column {
                        MetricRow("Whole body", measurement.wholeBodyImpedance?.let { "$it Ω" } ?: "Not recorded")
                        segments.filter { it.impedance != null }.forEach {
                            MetricRow(it.segment.name.replace('_', ' '), "${it.impedance} Ω")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMeasurementDialog(measurement: BodyMeasurementEntity, onDismiss: () -> Unit, onSave: (BodyMeasurementEntity) -> Unit) {
    var weight by remember { mutableStateOf(measurement.weightKg.toString()) }
    var bodyFat by remember { mutableStateOf(measurement.bodyFatPercent?.toString().orEmpty()) }
    var height by remember { mutableStateOf(measurement.heightCm?.toString().orEmpty()) }
    var visceral by remember { mutableStateOf(measurement.visceralFatRating?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit measurement") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") })
            OutlinedTextField(bodyFat, { bodyFat = it }, label = { Text("Body fat % (optional)") })
            OutlinedTextField(height, { height = it }, label = { Text("Height cm (optional)") })
            OutlinedTextField(visceral, { visceral = it }, label = { Text("Visceral fat (optional)") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(onClick = {
        val parsedWeight = weight.toDoubleOrNull()
        if (parsedWeight == null || parsedWeight <= 0) { error = "Enter a valid weight."; return@Button }
        val parsedFat = bodyFat.toDoubleOrNull()
        if (bodyFat.isNotBlank() && (parsedFat == null || parsedFat !in 0.0..100.0)) { error = "Body fat must be between 0 and 100."; return@Button }
        val parsedHeight = height.toDoubleOrNull()
        if (height.isNotBlank() && (parsedHeight == null || parsedHeight <= 0)) { error = "Enter a valid height."; return@Button }
        val fatMass = HealthCalculations.fatMassKg(parsedWeight, parsedFat)
        onSave(measurement.copy(weightKg = parsedWeight, bodyFatPercent = parsedFat, heightCm = parsedHeight, bmi = HealthCalculations.bmi(parsedWeight, parsedHeight), fatMassKg = fatMass, ffmKg = HealthCalculations.fatFreeMassKg(parsedWeight, fatMass), visceralFatRating = visceral.toDoubleOrNull()))
    }) { Text("Save") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun Summary(m: BodyMeasurementEntity) = PremiumCard(Modifier.fillMaxWidth()) { Column {
    Text(m.measurementDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), style = MaterialTheme.typography.titleLarge)
    MetricRow("Weight", "%.1f kg".format(m.weightKg)); MetricRow("BMI", m.bmi?.let { "%.1f".format(it) } ?: "Not recorded")
    MetricRow("Body fat", m.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "Not recorded"); MetricRow("Fat mass", m.fatMassKg?.let { "%.1f kg".format(it) } ?: "Not recorded")
    MetricRow("Fat-free mass", m.ffmKg?.let { "%.1f kg".format(it) } ?: "Not recorded"); MetricRow("Visceral fat", m.visceralFatRating?.toString() ?: "Not recorded")
} }

@Composable private fun SegmentDetails(segment: BodySegment, data: SegmentalCompositionEntity?) = PremiumCard(Modifier.fillMaxWidth()) { Column {
    Text(segment.name.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
    if (data == null) Text("No data was entered for this region.") else { MetricRow("Fat", data.fatPercent?.let { "$it%" } ?: "Not recorded"); MetricRow("Fat mass", data.fatMassKg?.let { "$it kg" } ?: "Not recorded"); MetricRow("FFM", data.ffmKg?.let { "$it kg" } ?: "Not recorded"); MetricRow("Predicted muscle", data.predictedMuscleMassKg?.let { "$it kg" } ?: "Not recorded"); MetricRow("Impedance", data.impedance?.let { "$it Ω" } ?: "Not recorded") }
} }
