package com.perfectapp.ui.screens.wealth

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
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.AssetType
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.domain.wealth.BASE_CURRENCY
import kotlinx.coroutines.launch

@Composable
fun AddAssetScreen(
    repository: WealthRepository,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AssetType.CASH) }
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyCode by remember { mutableStateOf(BASE_CURRENCY) }
    var quantity by remember { mutableStateOf("1") }
    var valuePerUnit by remember { mutableStateOf("") }
    var isLiability by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Asset") }) }
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
                    label = { Text("Asset name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AssetType.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    type = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = currencyCode, onValueChange = { currencyCode = it.uppercase().take(6) },
                    label = { Text("Currency code") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = valuePerUnit, onValueChange = { valuePerUnit = it },
                    label = { Text("Value per unit ($currencyCode)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("This is a liability (debt)")
                    Switch(checked = isLiability, onCheckedChange = { isLiability = it })
                }
            }
            item {
                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull()
                        val value = valuePerUnit.toDoubleOrNull()
                        when {
                            name.isBlank() -> { errorMessage = "Asset name is required."; return@Button }
                            currencyCode.isBlank() -> { errorMessage = "Currency code is required."; return@Button }
                            qty == null || !qty.isFinite() -> { errorMessage = "Enter a finite quantity."; return@Button }
                            value == null || !value.isFinite() -> { errorMessage = "Enter a finite value per unit."; return@Button }
                        }
                        errorMessage = null
                        val asset = AssetEntity(
                            name = name,
                            type = type,
                            currencyCode = currencyCode,
                            quantity = qty,
                            valuePerUnit = value,
                            isLiability = isLiability
                        )
                        scope.launch {
                            repository.upsertAsset(asset)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Asset")
                }
            }
            errorMessage?.let { message -> item { Text(message, color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
        }
    }
}
