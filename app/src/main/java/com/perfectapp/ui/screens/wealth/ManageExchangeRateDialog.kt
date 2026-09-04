package com.perfectapp.ui.screens.wealth

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
import com.perfectapp.domain.wealth.BASE_CURRENCY

/** Lets the user set/update the conversion rate for one currency into the base currency,
 *  e.g. currencyCode="EUR", rateToBase=1.08 means 1 EUR = 1.08 <BASE_CURRENCY>. */
@Composable
fun ManageExchangeRateDialog(
    onDismiss: () -> Unit,
    onSave: (currencyCode: String, rateToBase: Double) -> Unit
) {
    var currencyCode by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Exchange Rate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How much is 1 unit of this currency worth in $BASE_CURRENCY?")
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase().take(6) },
                    label = { Text("Currency code (e.g. EUR)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate to $BASE_CURRENCY") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rateValue = rate.toDoubleOrNull()
                if (currencyCode.isNotBlank() && rateValue != null) {
                    onSave(currencyCode, rateValue)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
