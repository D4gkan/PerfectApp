package com.perfectapp.ui.screens.wealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.entities.BillingCycle
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.data.entities.TransactionType
import com.perfectapp.data.entities.AssetType
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.data.repository.SettingsRepository
import com.perfectapp.data.repository.AppSettings
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.CurrencyAmount
import com.perfectapp.ui.components.MetricRow
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.SectionHeader
import com.perfectapp.ui.components.formatCurrency
import com.perfectapp.ui.theme.NegativeRed
import com.perfectapp.ui.theme.PositiveGreen
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Composable
fun WealthScreen(
    repository: WealthRepository,
    settingsRepository: SettingsRepository,
    onAddAsset: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val viewModel: WealthViewModel = viewModel(factory = viewModelFactory { WealthViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var transactionQuery by remember { mutableStateOf("") }
    var transactionTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    var transactionToReverse by remember { mutableStateOf<TransactionEntity?>(null) }
    val displayRate = if (settings.displayCurrency == "USD") 1.0 else state.exchangeRates.firstOrNull { it.currencyCode == settings.displayCurrency }?.rateToBase
    val displayedNetWorth = displayRate?.let { state.netWorth / it } ?: state.netWorth

    assetToDelete?.let { asset ->
        AlertDialog(onDismissRequest = { assetToDelete = null }, title = { Text("Delete asset?") },
            text = { Text("This removes ${asset.name}. Transactions are kept for history, but future reversals cannot restore this asset.") },
            confirmButton = { Button(onClick = { viewModel.deleteAsset(asset); assetToDelete = null }) { Text("Delete") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { assetToDelete = null }) { Text("Cancel") } })
    }
    transactionToReverse?.let { transaction ->
        AlertDialog(onDismissRequest = { transactionToReverse = null }, title = { Text("Reverse transaction?") },
            text = { Text("This removes the transaction and restores its affected asset balance.") },
            confirmButton = { Button(onClick = { viewModel.reverseTransaction(transaction); transactionToReverse = null }) { Text("Reverse") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { transactionToReverse = null }) { Text("Cancel") } })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyAmount(amount = displayedNetWorth, currencyCode = settings.displayCurrency)
                    state.netWorthChangeThisMonth?.let { change ->
                        Text(
                            text = (if (change >= 0) "+" else "") + formatCurrency(change / (displayRate ?: 1.0), settings.displayCurrency) + " this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (change >= 0) PositiveGreen else NegativeRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.recordSnapshot() }) {
                        Text("Record Snapshot")
                    }
                }
            }
        }

        item { SectionHeader(title = "Exchange Rates (vs USD)") }
        items(listOf("USD", "EUR", "GBP", "TRY")) { code ->
            val rate = if (code == "USD") 1.0 else state.exchangeRates.firstOrNull { it.currencyCode == code }?.rateToBase ?: 0.0
            ExchangeRateRow(code, rate, viewModel::setExchangeRate)
        }

        item {
            SectionHeader(title = "Cash & Assets", actionLabel = "Add", onActionClick = onAddAsset)
        }
        val cashAssets = state.assets.filter { it.type != AssetType.GOLD }
        if (cashAssets.isEmpty()) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No assets added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(cashAssets) { asset ->
            AssetRow(asset, viewModel::setAssetBalance, onDelete = { assetToDelete = asset })
        }

        item { SectionHeader(title = "Gold Calculator") }
        item { GoldCalculator(state.goldSettings, state.exchangeRates.firstOrNull { it.currencyCode == "TRY" }?.rateToBase ?: 0.0, viewModel::updateGoldSettings) }

        item {
            SectionHeader(title = "Transactions", actionLabel = "Add", onActionClick = onAddTransaction)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(transactionQuery, { transactionQuery = it }, label = { Text("Search category or note") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = transactionTypeFilter == null, onClick = { transactionTypeFilter = null }, label = { Text("All") })
                    FilterChip(selected = transactionTypeFilter == TransactionType.EXPENSE, onClick = { transactionTypeFilter = TransactionType.EXPENSE }, label = { Text("Expenses") })
                    FilterChip(selected = transactionTypeFilter == TransactionType.INCOME, onClick = { transactionTypeFilter = TransactionType.INCOME }, label = { Text("Income") })
                }
            }
        }
        val visibleTransactions = state.transactions.filter { transaction ->
            (transactionTypeFilter == null || transaction.type == transactionTypeFilter) &&
                (transactionQuery.isBlank() || transaction.category.contains(transactionQuery, true) || transaction.note.orEmpty().contains(transactionQuery, true))
        }
        if (visibleTransactions.isEmpty()) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.transactions.isEmpty()) "No transactions logged yet" else "No matching transactions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(visibleTransactions) { transaction ->
            TransactionRow(transaction, state.assets.firstOrNull { it.id == transaction.assetId }?.name, onReverse = { transactionToReverse = transaction })
        }

        item { SectionHeader(title = "Spending statistics") }
        val monthStart = LocalDate.now().withDayOfMonth(1)
        val monthlyExpenses = state.transactions.filter { it.type == TransactionType.EXPENSE && !it.date.isBefore(monthStart) }
        if (monthlyExpenses.isEmpty()) {
            item { PremiumCard(modifier = Modifier.fillMaxWidth()) { Text("No expenses recorded this month", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        val rates = state.exchangeRates.associate { it.currencyCode to it.rateToBase }
                        val converted = monthlyExpenses.map { com.perfectapp.domain.wealth.RenewalCosts.usd(it.amount, it.currencyCode, rates) }
                        if (converted.all { it != null }) MetricRow("This month's spending", formatCurrency(converted.filterNotNull().sum(), "USD"))
                        else Text("Set missing exchange rates to see a USD total.")
                        monthlyExpenses.groupBy { it.category }.toList().sortedByDescending { (_, items) -> items.sumOf { it.amount } }.take(5).forEach { (category, items) ->
                            items.groupBy { it.currencyCode }.forEach { (currency, entries) -> MetricRow(category, formatCurrency(entries.sumOf { it.amount }, currency)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeRateRow(code: String, rate: Double, onRateChanged: (String, Double) -> Unit) {
    var text by remember(code, rate) { mutableStateOf(if (code == "USD") "1" else if (rate == 0.0) "" else rate.toString()) }
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1 $code =", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = text, onValueChange = { entered ->
                text = entered
                entered.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }?.let { onRateChanged(code, it) }
            }, enabled = code != "USD", label = { Text("USD rate") }, suffix = { Text("USD") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
        }
    }
}

@Composable
private fun AssetRow(asset: AssetEntity, onBalanceSaved: (AssetEntity, Double) -> Unit, onDelete: () -> Unit) {
    var balanceText by remember(asset.id, asset.quantity) { mutableStateOf(if (asset.quantity == 0.0) "" else asset.quantity.toString()) }
    var balanceError by remember(asset.id) { mutableStateOf<String?>(null) }
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(asset.name, style = MaterialTheme.typography.titleMedium)
                    Text("${asset.type.name} · ${asset.currencyCode}" + if (asset.isLiability) " (liability)" else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Delete asset") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(balanceText, { balanceText = it; balanceError = null }, label = { Text("Balance (${asset.currencyCode})") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                androidx.compose.material3.TextButton(onClick = {
                    val balance = balanceText.toDoubleOrNull()
                    if (balance == null || !balance.isFinite()) balanceError = "Enter a valid finite balance." else onBalanceSaved(asset, balance)
                }) { Text("Save") }
            }
            balanceError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun AssetRow(asset: AssetEntity, onDelete: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = asset.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${asset.type.name} · ${asset.quantity} @ ${formatCurrency(asset.valuePerUnit, asset.currencyCode)}" +
                        if (asset.isLiability) " (liability)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete asset")
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, assetName: String?, onReverse: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = transaction.category, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = transaction.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Asset: ${assetName ?: "Unavailable asset"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.isAutomatic) Text("Automatic subscription charge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                val isIncome = transaction.type == TransactionType.INCOME
                Text(text = (if (isIncome) "+" else "-") + formatCurrency(transaction.amount, transaction.currencyCode), style = MaterialTheme.typography.titleMedium, color = if (isIncome) PositiveGreen else NegativeRed)
                androidx.compose.material3.TextButton(onClick = onReverse) { Text("Reverse") }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: SubscriptionEntity,
    daysUntilCharge: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = subscription.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatCurrency(subscription.amount, subscription.currencyCode) +
                        " / " + (if (subscription.billingCycle == BillingCycle.MONTHLY) "month" else "year"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (!subscription.autoRenew) "Paused"
                        else "Renews " + subscription.nextChargeDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !subscription.autoRenew -> MaterialTheme.colorScheme.onSurfaceVariant
                        daysUntilCharge in 0..3 -> PositiveGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Row {
                androidx.compose.material3.TextButton(onClick = if (subscription.autoRenew) onPause else onResume) {
                    Text(if (subscription.autoRenew) "Pause" else "Resume")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete subscription")
                }
            }
        }
    }
}
