package com.perfectapp.ui.screens.renewals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.*
import com.perfectapp.data.repository.ReminderRepository
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.domain.wealth.RenewalCosts
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.formatCurrency
import kotlinx.coroutines.launch

@Composable
fun RenewalsScreen(repository: ReminderRepository, wealthRepository: WealthRepository, onAddRenewal: () -> Unit, onAddAutomatic: () -> Unit) {
    val reminders by repository.all.collectAsState(initial = emptyList())
    val subscriptions by wealthRepository.activeSubscriptions.collectAsState(initial = emptyList())
    val ratesList by wealthRepository.exchangeRates.collectAsState(initial = emptyList())
    val rates = ratesList.associate { it.currencyCode to it.rateToBase }
    val monthly = RenewalCosts.monthly(reminders, subscriptions, rates)
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ReminderEntity?>(null) }
    var editingSubscription by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var deleting by remember { mutableStateOf<ReminderEntity?>(null) }
    var deletingSubscription by remember { mutableStateOf<SubscriptionEntity?>(null) }
    editing?.let { item ->
        androidx.activity.compose.BackHandler { editing = null }
        AddRenewalScreen(repository, onSaved = { editing = null }, existing = item)
        return
    }
    editingSubscription?.let { item ->
        androidx.activity.compose.BackHandler { editingSubscription = null }
        com.perfectapp.ui.screens.wealth.AddSubscriptionScreen(wealthRepository, onSaved = { editingSubscription = null }, existing = item)
        return
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete renewal?") }, text = { Text(item.title) },
            confirmButton = { TextButton(onClick = { scope.launch { repository.delete(item); deleting = null } }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
    deletingSubscription?.let { item ->
        AlertDialog(onDismissRequest = { deletingSubscription = null }, title = { Text("Delete automatic renewal?") }, text = { Text("Stops future charges for ${item.name}. Existing transactions are kept.") },
            confirmButton = { TextButton(onClick = { scope.launch { wealthRepository.deleteSubscription(item); deletingSubscription = null } }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deletingSubscription = null }) { Text("Cancel") } })
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Renewals", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddRenewal) { Text("Add renewal") }
                OutlinedButton(onClick = onAddAutomatic) { Text("Automatic") }
            }
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Estimated monthly cost", style = MaterialTheme.typography.labelMedium)
                if (monthly.missingCurrencies.isEmpty()) Text(formatCurrency(monthly.usd, "USD"), style = MaterialTheme.typography.headlineSmall)
                else {
                    Text("USD total unavailable", style = MaterialTheme.typography.titleMedium)
                    Text("Set ${monthly.missingCurrencies.joinToString()} exchange rates in Wealth to convert all renewals.", style = MaterialTheme.typography.bodySmall)
                }
                Text("Active recurring renewals and automatic subscriptions", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (reminders.isEmpty() && subscriptions.isEmpty()) item { Text("Add subscriptions, insurance or memberships here.") }
        items(reminders, key = { "renewal-${it.id}" }) { item ->
            PremiumCard(Modifier.fillMaxWidth()) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                item.amount?.let { RenewalAmount(it, item.currencyCode ?: "USD", rates) }
                Text(if (item.isCompleted) "Completed" else "Due ${item.dueDate}" + if (item.isRecurring) " - every ${item.recurrenceMonths} month(s)" else "", style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { editing = item }) { Text("Edit") }
                    if (!item.isCompleted) TextButton(onClick = { scope.launch { repository.markPaid(item) } }) { Text("Mark paid") }
                    TextButton(onClick = { deleting = item }) { Text("Delete") }
                }
            }
        }
        items(subscriptions, key = { "subscription-${it.id}" }) { item ->
            PremiumCard(Modifier.fillMaxWidth()) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                RenewalAmount(item.amount, item.currencyCode, rates)
                Text(if (item.autoRenew) "Automatic - due ${item.nextChargeDate}" else "Automatic - paused", style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { editingSubscription = item }) { Text("Edit") }
                    TextButton(onClick = { scope.launch { wealthRepository.updateSubscription(item.copy(autoRenew = !item.autoRenew)) } }) { Text(if (item.autoRenew) "Pause" else "Resume") }
                    TextButton(onClick = { deletingSubscription = item }) { Text("Delete") }
                }
            }
        }
    }
}
@Composable private fun RenewalAmount(amount: Double, currency: String, rates: Map<String, Double>) {
    Text(formatCurrency(amount, currency), style = MaterialTheme.typography.titleSmall)
    if (currency != "USD") Text(RenewalCosts.usd(amount, currency, rates)?.let { "Equivalent: ${formatCurrency(it, "USD")}" } ?: "USD conversion needs a $currency rate in Wealth", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
