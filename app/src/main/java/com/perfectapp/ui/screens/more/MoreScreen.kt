package com.perfectapp.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfectapp.ui.components.PremiumCard

private data class MoreItem(val title: String, val subtitle: String, val enabled: Boolean)

private val moreItems = listOf(
    MoreItem("Car & Maintenance", "Vehicle profile, odometer, service schedule", enabled = true),
    MoreItem("Expenses & Renewals", "Subscriptions, insurance, membership tracking", enabled = true),
    MoreItem("Notifications", "Daily summary of what needs your attention", enabled = true),
    MoreItem("Home Screen Widgets", "Dashboard, next event, today's schedule, and reminders", enabled = true),
    MoreItem("Search", "Find events, reminders, expenses, and measurements", enabled = true),
    MoreItem("Backup & Restore", "Export or restore all of your local data", enabled = true),
    MoreItem("Settings", "Currency and water quick-add preferences", enabled = true)
)

@Composable
fun MoreScreen(onCarClick: () -> Unit, onRenewalsClick: () -> Unit, onNotificationsClick: () -> Unit, onSearchClick: () -> Unit, onBackupClick: () -> Unit, onSettingsClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(moreItems.size) { index ->
            val item = moreItems[index]
            val onClick: () -> Unit = when (index) {
                0 -> onCarClick
                1 -> onRenewalsClick
                2 -> onNotificationsClick
                4 -> onSearchClick
                5 -> onBackupClick
                6 -> onSettingsClick
                else -> ({})
            }
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (item.enabled) it.clickable(onClick = onClick) else it }
            ) {
                Column {
                    Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
