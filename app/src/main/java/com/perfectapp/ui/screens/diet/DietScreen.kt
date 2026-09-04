package com.perfectapp.ui.screens.diet

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.MealEntryEntity
import com.perfectapp.data.repository.DietRepository
import com.perfectapp.data.repository.WaterRepository
import com.perfectapp.data.repository.SettingsRepository
import com.perfectapp.data.repository.AppSettings
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.ProgressCard
import com.perfectapp.ui.components.SectionHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DietScreen(
    dietRepository: DietRepository,
    waterRepository: WaterRepository,
    settingsRepository: SettingsRepository,
    onAddMeal: () -> Unit
) {
    val viewModel: DietViewModel = viewModel(
        factory = viewModelFactory { DietViewModel(dietRepository, waterRepository) }
    )
    val state by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val clipboard = LocalClipboardManager.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<MealEntryEntity?>(null) }
    var waterToDelete by remember { mutableStateOf<com.perfectapp.data.entities.WaterEntryEntity?>(null) }

    if (showGoalDialog) {
        EditGoalDialog(
            currentGoal = state.goal,
            onDismiss = { showGoalDialog = false },
            onSave = {
                viewModel.saveGoal(it)
                showGoalDialog = false
            }
        )
    }
    mealToDelete?.let { meal ->
        AlertDialog(onDismissRequest = { mealToDelete = null }, title = { Text("Delete meal?") }, text = { Text("${meal.name} will be removed from this day's totals.") }, confirmButton = { Button(onClick = { viewModel.deleteMeal(meal); mealToDelete = null }) { Text("Delete") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { mealToDelete = null }) { Text("Cancel") } })
    }
    waterToDelete?.let { entry ->
        AlertDialog(onDismissRequest = { waterToDelete = null }, title = { Text("Remove water entry?") }, text = { Text("${entry.amountMl} ml will be removed from this day's intake.") }, confirmButton = { Button(onClick = { viewModel.removeWaterEntry(entry); waterToDelete = null }) { Text("Remove") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { waterToDelete = null }) { Text("Cancel") } })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMeal) {
                Icon(Icons.Filled.Add, contentDescription = "Add meal")
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { viewModel.selectDate(selectedDate.minusDays(1)) }) { Icon(Icons.Filled.ChevronLeft, "Previous day") }
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (selectedDate.isBefore(LocalDate.now())) viewModel.selectDate(selectedDate.plusDays(1)) }) { Icon(Icons.Filled.ChevronRight, "Next day") }
                }
            }
            item {
                SectionHeader(title = "Water", actionLabel = "Goals", onActionClick = { showGoalDialog = true })
            }
            item {
                ProgressCard(
                    title = "Today's Intake",
                    currentValue = "${state.waterTotalMl} ml",
                    goalValue = "${state.waterGoalMl} ml",
                    progress = if (state.waterGoalMl > 0) state.waterTotalMl / state.waterGoalMl.toFloat() else 0f
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(settings.glassMl, settings.smallBottleMl, settings.bigBottleMl).forEach { amount ->
                        OutlinedButton(
                            onClick = { viewModel.addWater(amount) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+${amount}ml")
                        }
                    }
                }
            }

            item { SectionHeader(title = "Calories") }
            item {
                ProgressCard(
                    title = "Consumed",
                    currentValue = "${state.totals.calories} kcal",
                    goalValue = "${state.goal?.calorieGoal ?: 0} kcal",
                    progress = if ((state.goal?.calorieGoal ?: 0) > 0)
                        state.totals.calories / (state.goal?.calorieGoal ?: 1).toFloat()
                    else 0f
                )
            }

            item { SectionHeader(title = "Macros") }
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MetricRow(label = "Protein", value = "%.0f / %d g".format(state.totals.proteinG, state.goal?.proteinGoalG ?: 0))
                        MetricRow(label = "Carbs", value = "%.0f / %d g".format(state.totals.carbsG, state.goal?.carbsGoalG ?: 0))
                        MetricRow(label = "Fat", value = "%.0f / %d g".format(state.totals.fatG, state.goal?.fatGoalG ?: 0))
                    }
                }
            }

            item { SectionHeader(title = "Statistics") }
            item {
                val today = LocalDate.now()
                val weekStart = today.minusDays(6)
                val monthStart = today.withDayOfMonth(1)
                fun averageFor(start: LocalDate): String {
                    val meals = state.allMeals.filter { !it.date.isBefore(start) }
                    val dates = meals.map { it.date }.distinct().size
                    if (dates == 0) return "No data yet"
                    val water = state.allWaterEntries.filter { !it.date.isBefore(start) }.sumOf { it.amountMl }
                    return "${meals.sumOf { it.calories } / dates} kcal · P%.0fg · C%.0fg · F%.0fg · %d ml water".format(meals.sumOf { it.proteinG } / dates, meals.sumOf { it.carbsG } / dates, meals.sumOf { it.fatG } / dates, water / dates)
                }
                val monthlyMeals = state.allMeals.filter { !it.date.isBefore(monthStart) }
                val proteinDays = monthlyMeals.groupBy { it.date }.count { (_, values) -> values.sumOf { it.proteinG } >= (state.goal?.proteinGoalG ?: Int.MAX_VALUE) }
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        MetricRow("Last 7 days", averageFor(weekStart))
                        MetricRow("This month", averageFor(monthStart))
                        MetricRow("Protein goal achieved", "$proteinDays / ${monthlyMeals.map { it.date }.distinct().size} days")
                    }
                }
            }

            item { SectionHeader(title = "Today's Meals") }
            item {
                OutlinedButton(onClick = {
                    val goal = state.goal
                    val report = buildString {
                        appendLine("DATE: ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}")
                        appendLine("\nDAILY GOALS")
                        appendLine("Calories: ${goal?.calorieGoal ?: 0} kcal")
                        appendLine("Protein: ${goal?.proteinGoalG ?: 0} g")
                        appendLine("Carbohydrates: ${goal?.carbsGoalG ?: 0} g")
                        appendLine("Fat: ${goal?.fatGoalG ?: 0} g")
                        appendLine("\nMEALS")
                        state.meals.reversed().forEach { meal ->
                            appendLine("\n${meal.name.uppercase()} — ${meal.loggedAt.toLocalTime()}")
                            meal.description?.let { appendLine(it) }
                            appendLine("Calories: ${meal.calories} kcal | Protein: ${meal.proteinG}g | Carbs: ${meal.carbsG}g | Fat: ${meal.fatG}g")
                        }
                        appendLine("\nTOTAL\nCalories: ${state.totals.calories} / ${goal?.calorieGoal ?: 0}")
                        appendLine("Protein: ${state.totals.proteinG} / ${goal?.proteinGoalG ?: 0}g")
                        appendLine("Carbs: ${state.totals.carbsG} / ${goal?.carbsGoalG ?: 0}g")
                        appendLine("Fat: ${state.totals.fatG} / ${goal?.fatGoalG ?: 0}g")
                        append("\nWATER\n${state.waterTotalMl} / ${state.waterGoalMl} ml")
                    }
                    clipboard.setText(AnnotatedString(report))
                }, modifier = Modifier.fillMaxWidth()) { Text("Copy day's diet") }
            }
            if (state.meals.isEmpty()) {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No meals logged yet today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.meals) { meal ->
                MealRow(meal = meal, onDelete = { mealToDelete = meal })
            }
            if (state.waterEntries.isNotEmpty()) {
                item { SectionHeader(title = "Water history") }
                items(state.waterEntries) { entry ->
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${entry.amountMl} ml · ${entry.loggedAt.toLocalTime()}")
                            IconButton(onClick = { waterToDelete = entry }) { Icon(Icons.Filled.Close, "Remove water entry") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: MealEntryEntity, onDelete: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${meal.calories} kcal · P${meal.proteinG.toInt()} C${meal.carbsG.toInt()} F${meal.fatG.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete meal")
            }
        }
    }
}
