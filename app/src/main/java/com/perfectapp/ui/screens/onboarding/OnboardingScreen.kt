package com.perfectapp.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.AppContainer
import com.perfectapp.data.entities.DietGoalEntity
import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.repository.AppSettings
import kotlinx.coroutines.launch

@Composable fun OnboardingScreen(container: AppContainer) {
    val currency = remember { mutableStateOf("USD") }; val calories = remember { mutableStateOf("2000") }; val protein = remember { mutableStateOf("150") }; val carbs = remember { mutableStateOf("200") }; val fat = remember { mutableStateOf("65") }; val water = remember { mutableStateOf("2500") }
    val carName = remember { mutableStateOf("") }; val carOdometer = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Welcome to Perfect App")
        Text("Set a few local preferences to personalise your dashboard. You can change all of these later.")
        OutlinedTextField(currency.value, { currency.value = it.uppercase().take(3) }, label = { Text("Display currency") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(calories.value, { calories.value = it }, label = { Text("Daily calories") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(protein.value, { protein.value = it }, label = { Text("Daily protein (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(carbs.value, { carbs.value = it }, label = { Text("Daily carbs (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(fat.value, { fat.value = it }, label = { Text("Daily fat (g)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(water.value, { water.value = it }, label = { Text("Daily water (ml)") }, modifier = Modifier.fillMaxWidth())
        Text("Vehicle (optional)")
        Text("Add it now, or set it up later from the Car tab.")
        OutlinedTextField(carName.value, { carName.value = it }, label = { Text("Vehicle name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(carOdometer.value, { carOdometer.value = it }, label = { Text("Current odometer (km)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { scope.launch {
            val cur = currency.value.takeIf { it in setOf("USD", "TRY", "EUR", "GBP") } ?: "USD"
            container.settingsRepository.save(AppSettings(displayCurrency = cur, onboardingComplete = true))
            container.dietRepository.setGoal(DietGoalEntity(calorieGoal = calories.value.toIntOrNull() ?: 2000, proteinGoalG = protein.value.toIntOrNull() ?: 150, carbsGoalG = carbs.value.toIntOrNull() ?: 200, fatGoalG = fat.value.toIntOrNull() ?: 65, waterGoalMl = water.value.toIntOrNull() ?: 2500))
            if (carName.value.isNotBlank()) container.carRepository.upsertCar(CarEntity(name = carName.value.trim(), currentOdometerKm = carOdometer.value.toLongOrNull()?.coerceAtLeast(0) ?: 0))
        } }, modifier = Modifier.fillMaxWidth()) { Text("Start using Perfect App") }
    }
}
