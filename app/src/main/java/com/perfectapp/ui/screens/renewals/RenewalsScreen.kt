package com.perfectapp.ui.screens.renewals

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.repository.ReminderRepository
import com.perfectapp.ui.common.viewModelFactory
import com.perfectapp.ui.components.CurrencyAmount
import com.perfectapp.ui.components.EmptyState
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.formatCurrency
import com.perfectapp.ui.theme.NegativeRed
import com.perfectapp.ui.theme.PositiveGreen
import java.time.format.DateTimeFormatter

@Composable
fun RenewalsScreen(
    repository: ReminderRepository,
    onAddRenewal: () -> Unit
) {
    val viewModel: RenewalsViewModel = viewModel(factory = viewModelFactory { RenewalsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    var itemToDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    itemToDelete?.let { item ->
        AlertDialog(onDismissRequest = { itemToDelete = null }, title = { Text("Delete renewal?") }, text = { Text("${item.title} will no longer appear in your renewal schedule.") }, confirmButton = { Button(onClick = { viewModel.delete(item); itemToDelete = null }) { Text("Delete") } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRenewal) {
                Icon(Icons.Filled.Add, contentDescription = "Add renewal")
            }
        }
    ) { innerPadding ->
        if (state.items.isEmpty()) {
            EmptyState(
                title = "No renewals tracked yet",
                message = "Add subscriptions, insurance, or memberships to keep due dates and costs in one place.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                actionLabel = "Add Renewal",
                onActionClick = onAddRenewal
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Estimated Monthly Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CurrencyAmount(amount = state.estimatedMonthlyCost)
                        Text(
                            text = "Across all active recurring renewals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.items) { item ->
                RenewalRow(
                    item = item,
                    daysLeft = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), item.dueDate),
                    onMarkPaid = { viewModel.markPaid(item) },
                    onDelete = { itemToDelete = item }
                )
            }
        }
    }
}

@Composable
private fun RenewalRow(
    item: ReminderEntity,
    daysLeft: Long,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append(item.category?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Reminder")
                        item.amount?.let { append(" · " + formatCurrency(it, item.currencyCode ?: "USD")) }
                        if (item.isRecurring) append(" · recurring")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Due " + item.dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                        if (daysLeft in 0..7) "  (${daysLeft}d)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (daysLeft < 0) NegativeRed
                        else if (daysLeft <= 7) PositiveGreen
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                if (!item.isCompleted) {
                    IconButton(onClick = onMarkPaid) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark paid")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = "Delete")
                }
            }
        }
    }
}
