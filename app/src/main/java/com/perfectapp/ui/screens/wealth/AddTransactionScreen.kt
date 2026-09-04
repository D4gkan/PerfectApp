package com.perfectapp.ui.screens.wealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.data.entities.TransactionType
import com.perfectapp.data.repository.WealthRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import com.perfectapp.ui.components.DatePickerField
import com.perfectapp.ui.components.TimePickerField

@Composable
fun AddTransactionScreen(
    repository: WealthRepository,
    onSaved: () -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var selectedAssetId by remember { mutableStateOf<Long?>(null) }
    var assetExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val assets by repository.assets.collectAsState(initial = emptyList())
    val selectedAsset = assets.firstOrNull { it.id == selectedAssetId }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Transaction") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                }
            }
            item {
                DatePickerField("Date", date, { date = it }, Modifier.fillMaxWidth())
            }
            item {
                TimePickerField("Time", time, { time = it }, Modifier.fillMaxWidth())
            }
            item {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                        androidx.compose.material3.TextButton(onClick = { categoryExpanded = true }, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 6.dp)) { Text("Browse") }
                        val categories = if (type == TransactionType.EXPENSE) listOf("Food & dining", "Transport", "Housing", "Bills", "Health", "Shopping", "Entertainment", "Education", "Other") else listOf("Salary", "Freelance", "Investment", "Refund", "Gift", "Other")
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { category = choice; categoryExpanded = false }) }
                        }
                    }
                    Text("Choose a standard category or enter a custom one.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAsset?.name ?: "Select asset",
                        onValueChange = {}, readOnly = true, enabled = false,
                        label = { Text("Asset") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { assetExpanded = true })
                    DropdownMenu(expanded = assetExpanded, onDismissRequest = { assetExpanded = false }) {
                        assets.filter { it.isActive && !it.isLiability }
                            .forEach { asset ->
                                DropdownMenuItem(text = { Text(asset.name) }, onClick = {
                                    selectedAssetId = asset.id
                                    assetExpanded = false
                                })
                            }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (${selectedAsset?.currencyCode?.let(::currencySymbol) ?: "select asset first"})") },
                    prefix = { selectedAsset?.currencyCode?.let { Text(currencySymbol(it)) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                errorMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        if (amountValue == null || !amountValue.isFinite() || amountValue <= 0) { errorMessage = "Enter a finite amount greater than zero."; return@Button }
                        if (category.isBlank()) { errorMessage = "Select or enter a category."; return@Button }
                        if (selectedAsset == null) { errorMessage = "Select a valid asset affected by this transaction."; return@Button }
                        errorMessage = null
                        val transaction = TransactionEntity(
                            date = date,
                            time = time,
                            type = type,
                            category = category,
                            amount = amountValue,
                            currencyCode = selectedAsset.currencyCode,
                            assetId = selectedAsset.id,
                            note = note.ifBlank { null }
                        )
                        scope.launch {
                            // The DAO repeats these checks in its database transaction so
                            // an asset deleted between selection and save cannot corrupt data.
                            try {
                                repository.addTransaction(transaction)
                                onSaved()
                            } catch (error: IllegalArgumentException) {
                                errorMessage = error.message ?: "This transaction is no longer valid."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }
}

private fun currencySymbol(currencyCode: String): String = try {
    java.util.Currency.getInstance(currencyCode).symbol
} catch (_: IllegalArgumentException) {
    "$currencyCode "
}
