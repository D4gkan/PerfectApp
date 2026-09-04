package com.perfectapp.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.BodyMeasurementEntity
import com.perfectapp.data.repository.HealthRepository
import com.perfectapp.domain.health.HealthCalculations
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.EmptyState
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import com.perfectapp.ui.components.StatCard
import com.perfectapp.ui.components.TrendLineChart
import com.perfectapp.ui.theme.NegativeRed
import com.perfectapp.ui.theme.PositiveGreen
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Composable
fun HealthScreen(
    repository: HealthRepository,
    onAddMeasurement: () -> Unit,
    onOpenMeasurement: (Long) -> Unit,
    onOpenActivities: () -> Unit
) {
    val viewModel: HealthViewModel = viewModel(factory = viewModelFactory { HealthViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    var targetWeightText by remember { mutableStateOf("") }
    var targetDateText by remember { mutableStateOf("") }
    var trendMetric by remember { mutableStateOf("Weight") }
    var trendDays by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeasurement) {
                Icon(Icons.Filled.Add, contentDescription = "Add measurement")
            }
        }
    ) { innerPadding ->
        if (state.measurements.isEmpty()) {
            EmptyState(
                title = "No measurements yet",
                message = "Log your first weigh-in to start tracking trends.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                actionLabel = "Add Measurement",
                onActionClick = onAddMeasurement
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Weight",
                        value = "%.1f kg".format(state.latest?.weightKg ?: 0.0),
                        subValue = state.weightChangeKg?.let { "%+.1f kg total".format(it) },
                        subValueColor = state.weightChangeKg?.let { if (it <= 0) PositiveGreen else NegativeRed },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "BMI",
                        value = state.latest?.bmi?.let { "%.1f".format(it) } ?: "--",
                        subValue = HealthCalculations.bmiCategory(state.latest?.bmi),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader(title = "Recomposition") }
            item {
                val score = HealthCalculations.recompositionScore(state.fatMassChangeKg, state.muscleGainedKg)
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Perfect App Recomposition Score", style = MaterialTheme.typography.titleMedium)
                        Text(score?.let { "$it / 100 · ${HealthCalculations.recompositionLabel(it)}" } ?: "Not enough composition data yet")
                        Text("A transparent progress indicator, not medical advice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Your earliest saved measurement is the permanent baseline used for total-change comparisons. Add later measurements to track progress over time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { SectionHeader(title = "Weight projection") }
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mathematical estimate only — not medical advice.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = targetWeightText, onValueChange = { targetWeightText = it },
                            label = { Text("Target weight (kg)") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = targetDateText, onValueChange = { targetDateText = it },
                            label = { Text("Target date (yyyy-MM-dd, optional)") }, modifier = Modifier.fillMaxWidth()
                        )
                        val target = targetWeightText.toDoubleOrNull()
                        if (target != null) {
                            val current = state.latest?.weightKg ?: 0.0
                            val difference = target - current
                            MetricRow(label = if (difference < 0) "Weight remaining" else "Difference", value = "%.1f kg".format(kotlin.math.abs(difference)))
                            Text(if (difference < 0) "Lose weight mode" else "Maintain / gain mode", style = MaterialTheme.typography.bodySmall)
                            val targetDate = runCatching { LocalDate.parse(targetDateText) }.getOrNull()
                            if (targetDate != null && targetDate.isAfter(LocalDate.now()) && difference != 0.0) {
                                val weeks = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate) / 7.0
                                MetricRow("Required average", "%.3f kg/week".format(kotlin.math.abs(difference) / weeks))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Fat Mass Change",
                        value = state.fatMassChangeKg?.let { "%+.1f kg".format(it) } ?: "--",
                        subValueColor = state.fatMassChangeKg?.let { if (it <= 0) PositiveGreen else NegativeRed },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Muscle Gained",
                        value = state.muscleGainedKg?.let { "%+.1f kg".format(it) } ?: "--",
                        subValueColor = state.muscleGainedKg?.let { if (it >= 0) PositiveGreen else NegativeRed },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader(title = "Monthly progress") }
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MetricRow("Weight this month", state.monthlyWeightChangeKg?.let { "%+.1f kg".format(it) } ?: "Not enough data")
                        MetricRow("Fat mass this month", state.monthlyFatChangeKg?.let { "%+.1f kg".format(it) } ?: "Not enough data")
                        MetricRow("Muscle this month", state.monthlyMuscleChangeKg?.let { "%+.1f kg".format(it) } ?: "Not enough data")
                        MetricRow("Average monthly weight change", state.averageMonthlyWeightChangeKg?.let { "%+.1f kg/month".format(it) } ?: "Not enough data yet")
                    }
                }
            }

            item { SectionHeader(title = "$trendMetric trend") }
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Weight", "BMI", "Body Fat", "Fat Mass", "Muscle", "Visceral").forEach { metric ->
                                FilterChip(selected = trendMetric == metric, onClick = { trendMetric = metric }, label = { Text(metric) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30d", 90 to "3m", 180 to "6m", 365 to "1y", null to "All").forEach { (days, label) ->
                                FilterChip(selected = trendDays == days, onClick = { trendDays = days }, label = { Text(label) })
                            }
                        }
                        val cutoff = trendDays?.let { LocalDate.now().minusDays(it.toLong()) }
                        val source = state.measurements.reversed().filter { cutoff == null || !it.measurementDate.isBefore(cutoff) }
                        val values = source.mapNotNull { measurement -> when (trendMetric) {
                            "Weight" -> measurement.weightKg
                            "BMI" -> measurement.bmi
                            "Body Fat" -> measurement.bodyFatPercent
                            "Fat Mass" -> measurement.fatMassKg
                            "Muscle" -> measurement.ffmKg
                            else -> measurement.visceralFatRating
                        }?.toFloat() }
                        if (values.size >= 2) TrendLineChart(values = values) else Text("Not enough data in this range", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { SectionHeader(title = "Daily activities", actionLabel = "Open", onActionClick = onOpenActivities) }
            item { SectionHeader(title = "History") }
            items(state.measurements) { measurement ->
                MeasurementRow(measurement) { onOpenMeasurement(measurement.id) }
            }
        }
    }
}

@Composable
private fun MeasurementRow(measurement: BodyMeasurementEntity, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Text(
                text = measurement.measurementDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                style = MaterialTheme.typography.titleMedium
            )
            MetricRow(label = "Weight", value = "%.1f kg".format(measurement.weightKg))
            measurement.bodyFatPercent?.let {
                MetricRow(label = "Body Fat", value = "%.1f%%".format(it))
            }
            measurement.bmi?.let {
                MetricRow(label = "BMI", value = "%.1f".format(it))
            }
            Text("Tap for complete composition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
