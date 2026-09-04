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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.repository.CarRepository
import kotlinx.coroutines.launch

@Composable
fun AddOrEditCarScreen(
    repository: CarRepository,
    onSaved: () -> Unit
) {
    val existingCar by repository.primaryCar.collectAsState(initial = null)

    var name by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("0") }
    var nextService by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    // Pre-fill the form once the existing car (if any) has loaded, without clobbering
    // whatever the user has already typed on subsequent recompositions.
    LaunchedEffect(existingCar) {
        if (!initialized && existingCar != null) {
            name = existingCar!!.name
            odometer = existingCar!!.currentOdometerKm.toString()
            nextService = existingCar!!.nextMaintenanceKm?.toString() ?: ""
            initialized = true
        }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (existingCar == null) "Add Vehicle" else "Edit Vehicle") }) }
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
                    value = name, onValueChange = { name = it },
                    label = { Text("Vehicle name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = odometer, onValueChange = { odometer = it },
                    label = { Text("Current odometer (km)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = nextService, onValueChange = { nextService = it },
                    label = { Text("Next service due at (km) — optional") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val car = CarEntity(
                            id = existingCar?.id ?: 0,
                            name = name,
                            currentOdometerKm = odometer.toLongOrNull() ?: 0,
                            nextMaintenanceKm = nextService.toLongOrNull()
                        )
                        scope.launch {
                            repository.upsertCar(car)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Vehicle")
                }
            }
        }
    }
}
