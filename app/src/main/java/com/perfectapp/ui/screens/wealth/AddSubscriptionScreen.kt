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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.BillingCycle
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.domain.wealth.BASE_CURRENCY
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.perfectapp.ui.components.DatePickerField

@Composable
fun AddSubscriptionScreen(
    repository: WealthRepository,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(BASE_CURRENCY) }
    var billingCycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var nextChargeDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fundingAssetId by remember { mutableStateOf<Long?>(null) }
    var assetExpanded by remember { mutableStateOf(false) }
    val assets by repository.assets.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Subscription") }) }
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
                    label = { Text("Name (e.g. Netflix, Spotify)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = assets.firstOrNull { it.id == fundingAssetId }?.name ?: "Select funding asset",
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Charge from") }, modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { assetExpanded = true })
                    DropdownMenu(expanded = assetExpanded, onDismissRequest = { assetExpanded = false }) {
                        assets.filter { it.currencyCode.equals(currencyCode, ignoreCase = true) && !it.isLiability }
                            .forEach { asset ->
                                DropdownMenuItem(text = { Text("${asset.name} (${asset.currencyCode})") }, onClick = {
                                    fundingAssetId = asset.id
                                    assetExpanded = false
                                })
                            }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currencyCode, onValueChange = { currencyCode = it.uppercase().take(6) },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (billingCycle == BillingCycle.MONTHLY) "Monthly" else "Yearly",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Billing cycle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { cycleExpanded = true }
                    )
                    DropdownMenu(
                        expanded = cycleExpanded,
                        onDismissRequest = { cycleExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Monthly") },
                            onClick = { billingCycle = BillingCycle.MONTHLY; cycleExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Yearly") },
                            onClick = { billingCycle = BillingCycle.YEARLY; cycleExpanded = false }
                        )
                    }
                }
            }
            item {
                DatePickerField("Next charge date", nextChargeDate, { nextChargeDate = it }, Modifier.fillMaxWidth())
            }
            item {
                Text(
                    "This subscription will auto-charge on its billing cycle and log an expense " +
                        "transaction automatically. You'll get a daily reminder for 3 days before it renews."
                )
            }
            errorMessage?.let { message ->
                item { Text(text = message, color = androidx.compose.ui.graphics.Color.Red) }
            }
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Name is required"
                            return@Button
                        }
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0.0) {
                            errorMessage = "Enter a finite amount greater than zero"
                            return@Button
                        }
                        if (fundingAssetId == null) {
                            errorMessage = "Select the asset to charge"
                            return@Button
                        }
                        val selectedAsset = assets.firstOrNull { it.id == fundingAssetId }
                        if (selectedAsset == null || !selectedAsset.isActive || !selectedAsset.currencyCode.equals(currencyCode, ignoreCase = true)) {
                            errorMessage = "Select an active asset that uses $currencyCode."
                            return@Button
                        }
                        errorMessage = null
                        val subscription = SubscriptionEntity(
                            name = name,
                            amount = amountValue,
                            currencyCode = currencyCode,
                            billingCycle = billingCycle,
                            nextChargeDate = nextChargeDate
                            ,fundingAssetId = fundingAssetId
                        )
                        scope.launch {
                            repository.addSubscription(subscription)
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Subscription")
                }
            }
        }
    }
}
