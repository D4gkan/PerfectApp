package com.perfectapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.AppContainer
import com.perfectapp.ui.components.CountdownCard
import com.perfectapp.ui.components.CurrencyAmount
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.ProgressCard
import com.perfectapp.ui.components.SectionHeader
import com.perfectapp.ui.components.StatCard
import com.perfectapp.ui.theme.NegativeRed
import com.perfectapp.ui.theme.PositiveGreen
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(container: AppContainer) {
    val viewModel: HomeViewModel = viewModel(factory = homeViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Good day", style = MaterialTheme.typography.headlineLarge)
        }

        item {
            SectionHeader(title = "Health")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Weight",
                    value = state.latestWeightKg?.let { "%.1f kg".format(it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Body Fat",
                    value = state.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "BMI",
                    value = state.bmi?.let { "%.1f".format(it) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    state.fatMassKg?.let { MetricRow("Fat Mass", "%.1f kg".format(it)) }
                    state.weightChangeKg?.let { MetricRow("Total Weight Change", "%+.1f kg".format(it)) }
                    state.fatMassChangeKg?.let { MetricRow("Total Fat Change", "%+.1f kg".format(it)) }
                    state.muscleGainedKg?.let { MetricRow("Total Muscle Change", "%+.1f kg".format(it)) }
                }
            }
        }

        item {
            SectionHeader(title = "Water")
        }
        item {
            ProgressCard(
                title = "Today's Intake",
                currentValue = "${state.waterTodayMl} ml",
                goalValue = "${state.waterGoalMl} ml",
                progress = if (state.waterGoalMl > 0) state.waterTodayMl / state.waterGoalMl.toFloat() else 0f
            )
        }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    MetricRow("Protein", "%.0f / %d g".format(state.dietTotals.proteinG, state.proteinGoal ?: 0))
                    MetricRow("Carbohydrates", "%.0f / %d g".format(state.dietTotals.carbsG, state.carbsGoal ?: 0))
                    MetricRow("Fat", "%.0f / %d g".format(state.dietTotals.fatG, state.fatGoal ?: 0))
                }
            }
        }

        item {
            SectionHeader(title = "Diet")
        }
        item {
            ProgressCard(
                title = "Calories",
                currentValue = "${state.dietTotals.calories} kcal",
                goalValue = "${state.calorieGoal ?: 0} kcal",
                progress = if ((state.calorieGoal ?: 0) > 0)
                    state.dietTotals.calories / (state.calorieGoal ?: 1).toFloat()
                else 0f
            )
        }

        item {
            SectionHeader(title = "Wealth")
        }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmount(amount = state.netWorth, currencyCode = state.baseCurrency)
                    state.netWorthChangeThisMonth?.let { change ->
                        Text(
                            text = (if (change >= 0) "+" else "") + "%.2f this month".format(change),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (change >= 0) PositiveGreen else NegativeRed
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Calendar")
        }
        item {
            val event = state.nextEvent
            if (event != null) {
                val daysLeft = ChronoUnit.DAYS.between(java.time.LocalDate.now(), event.dateTime.toLocalDate())
                CountdownCard(
                    title = event.title,
                    subtitle = event.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d '-' h:mm a")),
                    daysLabel = if (daysLeft == 0L) "Today" else "$daysLeft days"
                )
            } else {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No upcoming events", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SectionHeader(title = "Car")
        }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = state.carName ?: "No vehicle added", style = MaterialTheme.typography.titleMedium)
                    state.carName?.let { MetricRow(label = "Current odometer", value = "${state.carKmRemaining?.let { "${it} km to next service" } ?: "Service schedule not set"}") }
                    state.carKmRemaining?.let {
                        MetricRow(label = "Until next service", value = "$it km")
                    }
                }
            }
        }

        if (state.upcomingReminders.isNotEmpty()) {
            item {
                SectionHeader(title = "Reminders")
            }
            items(state.upcomingReminders) { reminder ->
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    MetricRow(
                        label = reminder.title,
                        value = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), reminder.dueDate).let { if (it == 0L) "Due today" else "$it days left" }
                    )
                }
            }
        }
    }
}
