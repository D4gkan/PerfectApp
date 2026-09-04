package com.perfectapp.ui.screens.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.DietGoalEntity

@Composable
fun EditGoalDialog(
    currentGoal: DietGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (DietGoalEntity) -> Unit
) {
    var calories by remember { mutableStateOf((currentGoal?.calorieGoal ?: 2000).toString()) }
    var protein by remember { mutableStateOf((currentGoal?.proteinGoalG ?: 150).toString()) }
    var carbs by remember { mutableStateOf((currentGoal?.carbsGoalG ?: 200).toString()) }
    var fat by remember { mutableStateOf((currentGoal?.fatGoalG ?: 65).toString()) }
    var water by remember { mutableStateOf((currentGoal?.waterGoalMl ?: 2500).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calories, onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein, onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbs, onValueChange = { carbs = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fat, onValueChange = { fat = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = water, onValueChange = { water = it },
                    label = { Text("Water (ml)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    DietGoalEntity(
                        calorieGoal = calories.toIntOrNull() ?: 2000,
                        proteinGoalG = protein.toIntOrNull() ?: 150,
                        carbsGoalG = carbs.toIntOrNull() ?: 200,
                        fatGoalG = fat.toIntOrNull() ?: 65,
                        waterGoalMl = water.toIntOrNull() ?: 2500
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
